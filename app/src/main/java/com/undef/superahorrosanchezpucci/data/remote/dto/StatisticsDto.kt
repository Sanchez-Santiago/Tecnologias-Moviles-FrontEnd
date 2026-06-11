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

data class SpendingByImportance(
    val importance: String,
    val total: Double,
    val percentage: Double,
    val purchaseCount: Int,
    val itemCount: Int
)

data class StoreFrequency(
    val storeId: String? = null,
    val storeName: String,
    val purchaseCount: Int,
    val totalSpent: Double,
    val percentage: Double
)

data class MostPurchasedProduct(
    val productName: String,
    val count: Int,
    val totalSpent: Double
)

data class BudgetProgress(
    val budgetId: String,
    val budgetName: String,
    val budgetAmount: Double,
    val spent: Double,
    val percentageUsed: Double,
    val period: String
)

data class MemberSpending(
    val userId: String,
    val userName: String,
    val totalSpent: Double,
    val percentage: Double,
    val purchaseCount: Int
)
