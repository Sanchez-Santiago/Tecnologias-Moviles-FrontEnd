package com.undef.superahorrosanchezpucci.data.remote.dto

data class SpendingByCategory(
    val categoryId: String,
    val categoryName: String,
    val total: Double,
    val percentage: Double
)

data class SpendingByStore(
    val storeId: String? = null,
    val storeName: String,
    val total: Double,
    val percentage: Double
)

data class MonthlySummary(
    val year: Int,
    val month: Int,
    val total: Double,
    val purchaseCount: Int
)
