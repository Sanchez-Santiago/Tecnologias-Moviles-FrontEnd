package com.undef.superahorrosanchezpucci.data.repository

import android.util.Log
import com.undef.superahorrosanchezpucci.data.local.AppDao
import com.undef.superahorrosanchezpucci.data.local.TicketEntity
import com.undef.superahorrosanchezpucci.data.local.TicketProductoEntity
import com.undef.superahorrosanchezpucci.data.model.MetodoPago
import com.undef.superahorrosanchezpucci.data.model.Ticket
import com.undef.superahorrosanchezpucci.data.model.TicketImageAnalysis
import com.undef.superahorrosanchezpucci.data.model.TicketProducto
import com.undef.superahorrosanchezpucci.data.remote.ApiService
import com.undef.superahorrosanchezpucci.data.remote.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class PurchaseRepository(
    private val apiService: ApiService,
    private val appDao: AppDao
) {
    suspend fun getPurchases(groupId: String, budgetId: String): List<Ticket> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getPurchases(groupId)
            if (response.isSuccessful && response.body()?.success == true) {
                val apiPurchases = response.body()?.data ?: emptyList()
                val tickets = apiPurchases.map { it.toModel(budgetId) }
                
                appDao.clearTickets()
                appDao.clearTicketProductos()
                appDao.insertTickets(tickets.map { it.toEntity() })
                tickets.forEach { t -> appDao.insertTicketProductos(t.productos.mapIndexed { i, p -> p.toEntity(t.id, i) }) }
                return@withContext tickets
            }
        } catch (e: Exception) {
            Log.e("PurchaseRepo", "Error fetching purchases", e)
        }
        appDao.getTickets().map { it.toModel(appDao.getTicketProductosByTicketId(it.id).map { p -> p.toModel() }) }
    }

    suspend fun createPurchase(groupId: String, storeId: String?, storeName: String, items: List<CreatePurchaseItem>) = withContext(Dispatchers.IO) {
        try {
            apiService.createPurchase(CreatePurchaseRequest(groupId, storeId, if (storeId == null) storeName else null, items))
        } catch (_: Exception) {}
    }

    suspend fun deletePurchase(id: String) = withContext(Dispatchers.IO) {
        try { apiService.deletePurchase(id) } catch (_: Exception) {}
    }

    suspend fun analyzeTicket(imageBytes: ByteArray, mimeType: String): TicketImageAnalysis = withContext(Dispatchers.IO) {
        try {
            val base64 = android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP)
            val response = apiService.analyzeTicketImage(AnalyzeTicketImageRequest(base64, mimeType))
            if (response.isSuccessful) {
                val data = response.body()?.data
                return@withContext TicketImageAnalysis(
                    data?.storeName, data?.purchaseDate, data?.total?.toInt(),
                    data?.products?.map { TicketProducto(it.name, (it.totalPrice ?: it.unitPrice ?: 0.0).toInt(), (it.quantity ?: 1.0).toInt()) } ?: emptyList()
                )
            }
        } catch (_: Exception) {}
        TicketImageAnalysis(null, null, null, emptyList())
    }

    suspend fun localSaveTickets(tickets: List<Ticket>) = withContext(Dispatchers.IO) {
        appDao.clearTicketProductos()
        appDao.clearTickets()
        appDao.insertTickets(tickets.map { it.toEntity() })
        tickets.forEach { t -> appDao.insertTicketProductos(t.productos.mapIndexed { i, p -> p.toEntity(t.id, i) }) }
    }

    // Mappings
    private fun PurchaseResponse.toModel(budgetId: String) = Ticket(
        id = id, supermercado = storeName ?: notes ?: "Supermercado", direccion = "",
        fechaHora = parseDate(purchaseDate), total = (total.toDoubleOrNull()?.toInt() ?: 0), metodoPago = MetodoPago.EFECTIVO,
        imagenPath = "", presupuestoId = budgetId, productos = items.map { TicketProducto(it.productName, (it.unitPrice.toDoubleOrNull()?.toInt() ?: 0), it.quantity) }
    )

    private fun TicketEntity.toModel(productos: List<TicketProducto>) = Ticket(
        id, supermercado, direccion, fechaHora, total, try { MetodoPago.valueOf(metodoPago) } catch (_: Exception) { MetodoPago.EFECTIVO },
        imagenPath, presupuestoId, productos
    )

    private fun TicketProductoEntity.toModel() = TicketProducto(nombre, precio, cantidad)
    private fun Ticket.toEntity() = TicketEntity(id, supermercado, direccion, fechaHora, total, metodoPago.name, imagenPath, presupuestoId)
    private fun TicketProducto.toEntity(ticketId: String, pos: Int) = TicketProductoEntity(ticketId, pos, nombre, precio, cantidad)

    private fun parseDate(date: String): Long = try {
        LocalDateTime.parse(date, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    } catch (_: Exception) { System.currentTimeMillis() }
}
