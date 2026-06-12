package com.undef.superahorrosanchezpucci.data.repository

import com.undef.superahorrosanchezpucci.data.local.AppDao
import com.undef.superahorrosanchezpucci.data.local.NotificationCacheEntity
import com.undef.superahorrosanchezpucci.data.local.OfferCacheEntity
import com.undef.superahorrosanchezpucci.data.remote.ApiService
import com.undef.superahorrosanchezpucci.data.remote.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationRepository(
    private val apiService: ApiService,
    private val appDao: AppDao
) {
    suspend fun getNotifications(): List<NotificationResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getNotifications()
            if (response.isSuccessful && response.body()?.success == true) {
                val notifications = response.body()?.data ?: emptyList()
                appDao.clearCachedNotifications()
                appDao.insertCachedNotifications(notifications.map { it.toEntity() })
                return@withContext notifications
            }
        } catch (_: Exception) {}
        appDao.getCachedNotifications().map { it.toModel() }
    }

    suspend fun getUnreadCount(): Int = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getUnreadNotificationsCount()
            if (response.isSuccessful) return@withContext response.body()?.data?.count ?: 0
        } catch (_: Exception) {}
        0
    }

    suspend fun markRead(id: String) = withContext(Dispatchers.IO) {
        try { apiService.markNotificationRead(id) } catch (_: Exception) {}
    }

    suspend fun markAllRead() = withContext(Dispatchers.IO) {
        try { apiService.markAllNotificationsRead() } catch (_: Exception) {}
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        try { apiService.deleteNotification(id) } catch (_: Exception) {}
    }

    suspend fun getOffers(): List<OfferResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getActiveOffers()
            if (response.isSuccessful && response.body()?.success == true) {
                val offers = response.body()?.data ?: emptyList()
                appDao.clearCachedOffers()
                appDao.insertCachedOffers(offers.map { it.toEntity() })
                return@withContext offers
            }
        } catch (_: Exception) {}
        appDao.getCachedOffers().map { it.toModel() }
    }

    suspend fun aiSuggestOffers(productNames: List<String>, storeId: String?): Result<List<AiOfferSuggestion>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.aiSuggestOffers(AiOfferSuggestionRequest(productNames, storeId))
            if (response.isSuccessful) Result.success(response.body()?.data?.suggestions ?: emptyList())
            else Result.failure(Exception("Error en sugerencias IA"))
        } catch (e: Exception) { Result.failure(e) }
    }

    // Mappings
    private fun NotificationResponse.toEntity() = NotificationCacheEntity(id, type, title, message, data, read, createdAt)
    private fun NotificationCacheEntity.toModel() = NotificationResponse(id, type, title, message, data, read, createdAt)
    private fun OfferResponse.toEntity() = OfferCacheEntity(id, storeId, storeName, title, description, discountType, discountValue, startDate ?: "", endDate ?: "", imageUrl)
    private fun OfferCacheEntity.toModel() = OfferResponse(id, storeId, storeName, title, description, discountType, discountValue, startDate, endDate, imageUrl)
}
