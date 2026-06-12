package com.undef.superahorrosanchezpucci.data.repository

import android.util.Log
import com.undef.superahorrosanchezpucci.data.local.AppDao
import com.undef.superahorrosanchezpucci.data.local.ListaCompraEntity
import com.undef.superahorrosanchezpucci.data.local.ProductoEntity
import com.undef.superahorrosanchezpucci.data.model.Categoria
import com.undef.superahorrosanchezpucci.data.model.ListaCompra
import com.undef.superahorrosanchezpucci.data.model.Producto
import com.undef.superahorrosanchezpucci.data.remote.ApiService
import com.undef.superahorrosanchezpucci.data.remote.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ShoppingListRepository(
    private val apiService: ApiService,
    private val appDao: AppDao
) {
    suspend fun getShoppingLists(groupId: String, presupuestoId: String, modoIndividual: Boolean): List<ListaCompra> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getShoppingLists(groupId)
            if (response.isSuccessful && response.body()?.success == true) {
                val apiLists = response.body()?.data ?: emptyList()
                val lists = apiLists.map { it.toModel(presupuestoId, !modoIndividual) }
                
                appDao.clearListas()
                appDao.insertListas(lists.map { it.toEntity() })
                lists.forEach { list ->
                    appDao.deleteProductosByListaId(list.id)
                    appDao.insertProductos(list.productos.map { it.toEntity(list.id) })
                }
                return@withContext lists
            }
        } catch (e: Exception) {
            Log.e("ListRepo", "Error fetching lists", e)
        }
        appDao.getListas().map { it.toModel(appDao.getProductosByListaId(it.id).map { p -> p.toModel() }) }
    }

    suspend fun createList(groupId: String, name: String): String? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.createShoppingList(CreateShoppingListRequest(groupId, name))
            if (response.isSuccessful) return@withContext response.body()?.data?.id
            else Log.e("ListRepo", "Error creating list: ${response.errorBody()?.string()}")
        } catch (e: Exception) {
            Log.e("ListRepo", "Exception creating list", e)
        }
        null
    }

    suspend fun syncProduct(listaId: String, producto: Producto, isNew: Boolean, productCatalogId: String?) = withContext(Dispatchers.IO) {
        if (productCatalogId == null) {
            Log.w("ListRepo", "Cannot sync product ${producto.nombre}: No catalog ID found")
            return@withContext
        }
        
        try {
                val response = if (isNew) {
                    apiService.addProductToList(listaId, AddProductRequest(productCatalogId, producto.cantidad.toDouble()))
                } else {
                    val effectiveProdId = if (producto.id.length < 30) productCatalogId else producto.id
                    apiService.updateProductInList(listaId, effectiveProdId, UpdateProductRequest(producto.comprado, producto.precioEstimado.toDouble(), producto.cantidad.toDouble()))
                }
                if (!response.isSuccessful) {
                    Log.e("ListRepo", "Error syncing product ${producto.nombre}: ${response.errorBody()?.string()}")
                }
        } catch (e: Exception) {
            Log.e("ListRepo", "Error syncing product ${producto.nombre}", e)
        }
    }

    suspend fun deleteProduct(listaId: String, productoId: String) = withContext(Dispatchers.IO) {
        if (listaId.startsWith("lista-")) return@withContext
        try { apiService.deleteProductFromList(listaId, productoId) } catch (_: Exception) {}
    }

    suspend fun localSaveList(lista: ListaCompra) = withContext(Dispatchers.IO) {
        appDao.insertListas(listOf(lista.toEntity()))
    }

    suspend fun localSaveProducts(listaId: String, productos: List<Producto>) = withContext(Dispatchers.IO) {
        appDao.deleteProductosByListaId(listaId)
        appDao.insertProductos(productos.map { it.toEntity(listaId) })
    }

    // Mappings
    private fun ShoppingListResponse.toModel(presupuestoId: String, esFamiliar: Boolean) = ListaCompra(
        id = id, nombre = name, presupuestoId = presupuestoId, esFamiliar = esFamiliar,
        fechaCreacion = parseDate(createdAt), total = products.sumOf { (it.finalPrice ?: 0.0) * (it.finalQuantity ?: 1.0) }.toInt(),
        productos = products.map { it.toModel() }.toMutableList()
    )

    private fun ShoppingListProductResponse.toModel() = Producto(
        id = id, nombre = productName, codigo = productId, precio = (finalPrice ?: 0.0).toInt(),
        precioEstimado = (finalPrice ?: 0.0).toInt(), cantidad = (finalQuantity ?: 1.0).toInt(),
        comprado = checked, categoria = Categoria.ESENCIAL
    )

    private fun ListaCompraEntity.toModel(productos: List<Producto>) = ListaCompra(
        id, nombre, presupuestoId, esFamiliar, fechaCreacion, hora, supermercado, total, productos.toMutableList()
    )

    private fun ProductoEntity.toModel() = Producto(
        id, codigo, nombre, descripcion, precio, marca, precioEstimado, precioReal, cantidad, comprado,
        try { Categoria.valueOf(categoria) } catch (_: Exception) { Categoria.ESENCIAL }
    )

    private fun ListaCompra.toEntity() = ListaCompraEntity(id, nombre, presupuestoId, esFamiliar, fechaCreacion, hora, supermercado, total)
    private fun Producto.toEntity(listaId: String) = ProductoEntity(id, listaId, codigo, nombre, descripcion, precio, marca, precioEstimado, precioReal, cantidad, comprado, categoria.name)

    private fun parseDate(date: String): Long = try {
        LocalDateTime.parse(date, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    } catch (_: Exception) { System.currentTimeMillis() }
}
