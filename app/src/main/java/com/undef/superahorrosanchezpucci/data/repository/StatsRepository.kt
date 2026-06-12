package com.undef.superahorrosanchezpucci.data.repository

import com.undef.superahorrosanchezpucci.data.remote.ApiService
import com.undef.superahorrosanchezpucci.data.remote.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StatsRepository(private val apiService: ApiService) {
    suspend fun getBudgetProgress(groupId: String) = safeApiCall { apiService.getBudgetProgress(groupId) }
    suspend fun getSpendingByCategory(groupId: String) = safeApiCall { apiService.getSpendingByCategory(groupId) }
    suspend fun getSpendingByStore(groupId: String) = safeApiCall { apiService.getSpendingByStore(groupId) }
    suspend fun getSpendingByImportance(groupId: String) = safeApiCall { apiService.getSpendingByImportance(groupId) }
    suspend fun getMonthlySummary(groupId: String) = safeApiCall { apiService.getMonthlySummary(groupId) }
    suspend fun getMostFrequentStore(groupId: String) = safeApiCall { apiService.getMostFrequentStore(groupId) }
    suspend fun getMostPurchasedProducts(groupId: String) = safeApiCall { apiService.getMostPurchasedProducts(groupId) }
    suspend fun getMemberSpending(groupId: String) = safeApiCall { apiService.getMemberSpending(groupId) }

    private suspend fun <T> safeApiCall(call: suspend () -> retrofit2.Response<ApiResponse<T>>): Result<T> = withContext(Dispatchers.IO) {
        try {
            val response = call()
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error en la API de estadísticas"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
