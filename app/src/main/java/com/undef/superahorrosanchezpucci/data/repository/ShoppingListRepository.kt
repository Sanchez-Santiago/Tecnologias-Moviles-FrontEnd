package com.undef.superahorrosanchezpucci.data.repository

import android.util.Log
import com.undef.superahorrosanchezpucci.data.local.*
import com.undef.superahorrosanchezpucci.data.model.ListaCompra
import com.undef.superahorrosanchezpucci.data.model.Producto
import com.undef.superahorrosanchezpucci.data.remote.ApiService
import com.undef.superahorrosanchezpucci.data.remote.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ShoppingListRepository(
    private val apiService: ApiService,
    private val appDao: AppDao
) {
    /**
     * Obtiene las listas de compras. 
     * Implementa una estrategia de "Local First": Primero devuelve lo que hay en Room 
     * y luego intenta actualizar desde la API en segundo plano.
     */
    suspend fun getShoppingLists(groupId: String, presupuestoId: String, modoIndividual: Boolean): List<ListaCompra> = withContext(Dispatchers.IO) {
        // Intentar obtener de la API para refrescar la caché
        try {
            val response = apiService.getShoppingLists(groupId)
            if (response.isSuccessful && response.body()?.success == true) {
                val apiLists = response.body()?.data ?: emptyList()
                
                // Logging para verificar datos (como un curl)
                Log.d("ShoppingListRepo", "API Response for group $groupId: ${apiLists.size} lists")
                apiLists.forEach { list ->
                    Log.d("ShoppingListRepo", "List: ${list.name}, Products: ${list.products.size}")
                    list.products.forEach { p ->
                        Log.d("ShoppingListRepo", "  - Product: ${p.productName}, FinalPrice: ${p.finalPrice}, UnitPrice: ${p.unitPrice}, Price: ${p.price}, Subtotal: ${p.subtotal}")
                    }
                }

                val lists = apiLists.map { it.toModel(groupId, presupuestoId, !modoIndividual) }
                
                // Actualizar Room
                appDao.refreshListas(lists.map { it.toEntity() }, lists.flatMap { it.productos.map { p -> p.toEntity(it.id) } })
                
                return@withContext lists
            } else {
                Log.e("ShoppingListRepo", "API Error: ${response.code()} ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("ShoppingListRepo", "Error fetching from API, falling back to cache", e)
        }

        // Si falla la API, devolvemos lo que hay en Room filtrado por grupo
        appDao.getListas()
            .filter { it.groupId == groupId }
            .map { entity ->
                entity.toModel(appDao.getProductosByListaId(entity.id).map { it.toModel() })
            }
    }

    suspend fun createList(groupId: String, name: String): String? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.createShoppingList(CreateShoppingListRequest(groupId, name))
            if (response.isSuccessful) return@withContext response.body()?.data?.id
        } catch (e: Exception) {
            Log.e("ListRepo", "Exception creating list", e)
        }
        null
    }

    suspend fun syncProduct(listaId: String, producto: Producto, isNew: Boolean, productCatalogId: String?) = withContext(Dispatchers.IO) {
        if (productCatalogId == null) {
            Log.w("ShoppingListRepo", "No se puede sincronizar '${producto.nombre}' porque no se encontró en el catálogo (ID nulo)")
            return@withContext
        }
        
        try {
            Log.d("ShoppingListRepo", "Sincronizando '${producto.nombre}' con la API. isNew: $isNew, catalogId: $productCatalogId")
            if (isNew) {
                val response = apiService.addProductToList(listaId, AddProductRequest(productCatalogId, producto.cantidad.toDouble()))
                if (!response.isSuccessful) {
                    Log.e("ShoppingListRepo", "Error al agregar producto: ${response.code()} ${response.errorBody()?.string()}")
                }
            } else {
                val effectiveProdId = if (producto.id.length < 30) productCatalogId else producto.id
                val response = apiService.updateProductInList(listaId, effectiveProdId, UpdateProductRequest(
                    checked = producto.comprado,
                    finalPrice = producto.precio.toDouble(),
                    finalQuantity = producto.cantidad.toDouble()
                ))
                if (!response.isSuccessful) {
                    Log.e("ShoppingListRepo", "Error al actualizar producto: ${response.code()} ${response.errorBody()?.string()}")
                }
            }
        } catch (e: Exception) {
            Log.e("ShoppingListRepo", "Excepción al sincronizar producto ${producto.nombre}", e)
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
}
