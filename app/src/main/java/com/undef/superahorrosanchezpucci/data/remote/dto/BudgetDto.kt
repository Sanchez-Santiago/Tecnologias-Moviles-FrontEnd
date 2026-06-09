package com.undef.superahorrosanchezpucci.data.remote.dto

data class BudgetResponse(
    val id: String,
    val groupId: String,
    val name: String,
    val totalAmount: Double,
    val period: String,
    val startDate: String,
    val endDate: String? = null,
    val items: List<BudgetItemResponse>,
    val createdAt: String
)

data class BudgetItemResponse(
    val id: String,
    val categoryId: String,
    val categoryName: String,
    val amount: Double
)

data class CreateBudgetRequest(
    val groupId: String,
    val name: String,
    val totalAmount: Double,
    val period: String,
    val startDate: String,
    val endDate: String? = null,
    val items: List<CreateBudgetItem>
)

data class CreateBudgetItem(
    val categoryId: String,
    val amount: Double
)

data class UpdateBudgetRequest(
    val name: String? = null,
    val totalAmount: Double? = null,
    val period: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val items: List<CreateBudgetItem>? = null
)
