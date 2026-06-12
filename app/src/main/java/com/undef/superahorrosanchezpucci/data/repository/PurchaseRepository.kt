package com.undef.superahorrosanchezpucci.data.repository

import android.util.Log
import com.undef.superahorrosanchezpucci.data.local.*
import com.undef.superahorrosanchezpucci.data.model.Ticket
import com.undef.superahorrosanchezpucci.data.model.TicketImageAnalysis
import com.undef.superahorrosanchezpucci.data.remote.ApiService
import com.undef.superahorrosanchezpucci.data.remote.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PurchaseRepository(
    private val apiService: ApiService,
    private val appDao: AppDao
) {
    /**
     * Obtiene los tickets/compras.
     * Muestra datos de Room al instante y actualiza desde la API.
     */
    suspend fun getPurchases(groupId: String, budgetId: String): List<Ticket> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getPurchases(groupId)
            if (response.isSuccessful && response.body()?.success == true) {
                val apiPurchases = response.body()?.data ?: emptyList()
                
                // Log para verificar parseo
                Log.d("PurchaseRepo", "Fetched ${apiPurchases.size} purchases for group $groupId")
                
                val tickets = apiPurchases.map { it.toModel(groupId, budgetId) }
                
                // Guardar en Room para persistencia permanente
                val ticketEntities = tickets.map { it.toEntity() }
                val productEntities = tickets.flatMap { t -> 
                    t.productos.mapIndexed { i, p -> p.toEntity(t.id, i) } 
                }
                appDao.refreshTickets(groupId, ticketEntities, productEntities)
                
                return@withContext tickets
            }
        } catch (e: Exception) {
            Log.e("PurchaseRepo", "Error fetching purchases from API", e)
        }

        // Fallback a Room filtrando por grupo
        appDao.getTickets()
            .filter { it.groupId == groupId }
            .map { entity -> 
                entity.toModel(appDao.getTicketProductosByTicketId(entity.id).map { it.toModel() }) 
            }
    }

    suspend fun createPurchase(groupId: String, storeId: String?, storeName: String, items: List<CreatePurchaseItem>, budgetId: String): Ticket? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.createPurchase(CreatePurchaseRequest(groupId, storeId, if (storeId == null) storeName else null, items))
            if (response.isSuccessful && response.body()?.success == true) {
                val purchase = response.body()!!.data!!
                val ticket = purchase.toModel(groupId, budgetId)
                
                // Guardar el ticket real en Room
                val entity = ticket.toEntity()
                val products = ticket.productos.mapIndexed { i, p -> p.toEntity(ticket.id, i) }
                appDao.saveFullTicket(entity, products)
                
                return@withContext ticket
            }
        } catch (e: Exception) {
            Log.e("PurchaseRepo", "Error creating purchase", e)
        }
        null
    }

    suspend fun deletePurchase(id: String) = withContext(Dispatchers.IO) {
        try { 
            apiService.deletePurchase(id) 
            appDao.deleteFullTicket(id)
        } catch (_: Exception) {
            appDao.deleteFullTicket(id)
        }
    }

    suspend fun updatePurchase(ticket: Ticket) = withContext(Dispatchers.IO) {
        appDao.saveFullTicket(ticket.toEntity(), ticket.productos.mapIndexed { i, p -> p.toEntity(ticket.id, i) })
        // Opcionalmente sincronizar con la API si no es temporal
    }

    suspend fun analyzeTicket(imageBytes: ByteArray, mimeType: String): TicketImageAnalysis = withContext(Dispatchers.IO) {
        try {
            val base64 = android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP)
            val response = apiService.analyzeTicketImage(AnalyzeTicketImageRequest(base64, mimeType))
            if (response.isSuccessful) {
                val data = response.body()?.data
                return@withContext TicketImageAnalysis(
                    data?.storeName, data?.purchaseDate, data?.total?.toInt(),
                    data?.products?.map { it.toModel() } ?: emptyList()
                )
            }
        } catch (_: Exception) {}
        TicketImageAnalysis(null, null, null, emptyList())
    }

    suspend fun localSaveTickets(groupId: String, tickets: List<Ticket>) = withContext(Dispatchers.IO) {
        val ticketEntities = tickets.map { it.toEntity() }
        val productEntities = tickets.flatMap { t -> 
            t.productos.mapIndexed { i, p -> p.toEntity(t.id, i) }
        }
        appDao.refreshTickets(groupId, ticketEntities, productEntities)
    }

    suspend fun localSaveSingleTicket(ticket: Ticket) = withContext(Dispatchers.IO) {
        val entity = ticket.toEntity()
        val productEntities = ticket.productos.mapIndexed { i, p -> p.toEntity(ticket.id, i) }
        appDao.saveFullTicket(entity, productEntities)
    }
}
