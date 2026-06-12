package com.undef.superahorrosanchezpucci.data.repository

import com.undef.superahorrosanchezpucci.data.local.AppDao
import com.undef.superahorrosanchezpucci.data.local.CatalogoProductoEntity
import com.undef.superahorrosanchezpucci.data.local.TiendaEntity
import com.undef.superahorrosanchezpucci.data.remote.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProductRepository(
    private val apiService: ApiService,
    private val appDao: AppDao
) {
    suspend fun fetchProducts(): Pair<Map<String, String>, Map<String, String>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getProducts()
            if (response.isSuccessful && response.body()?.success == true) {
                val products = response.body()?.data ?: emptyList()
                appDao.clearCatalogoProductos()
                appDao.insertCatalogoProductos(products.map { p ->
                    CatalogoProductoEntity(p.id, p.name, p.price, p.categoryId, p.categoryName, p.description, p.imageUrl, p.barcode, p.priority, p.active)
                })
                val nameToId = products.associate { it.name.lowercase().trim() to it.id }
                val barcodeToId = products.filter { !it.barcode.isNullOrBlank() }.associate { it.barcode!! to it.id }
                return@withContext nameToId to barcodeToId
            }
        } catch (_: Exception) {}
        val cached = appDao.getCatalogoProductos()
        val nameToId = cached.associate { it.name.lowercase().trim() to it.id }
        val barcodeToId = cached.filter { !it.barcode.isNullOrBlank() }.associate { it.barcode!! to it.id }
        nameToId to barcodeToId
    }

    suspend fun fetchStores(): Map<String, String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getStores()
            if (response.isSuccessful && response.body()?.success == true) {
                val stores = response.body()?.data ?: emptyList()
                appDao.clearTiendas()
                appDao.insertTiendas(stores.map { s ->
                    TiendaEntity(s.id, s.name, s.address, s.phone, s.latitude, s.longitude, s.active)
                })
                return@withContext stores.associate { it.name.lowercase().trim() to it.id }
            }
        } catch (_: Exception) {}
        appDao.getTiendas().associate { it.name.lowercase().trim() to it.id }
    }
}
