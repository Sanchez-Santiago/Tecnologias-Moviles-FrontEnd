package com.undef.superahorrosanchezpucci.data.remote.dto

data class CreatePurchaseRequest(
    val groupId: String,
    val storeId: String? = null,
    val notes: String? = null,
    val items: List<CreatePurchaseItem>
)

data class CreatePurchaseItem(
    val productId: String,
    val quantity: Int = 1
)

data class PurchaseResponse(
    val id: String,
    val groupId: String,
    val storeId: String? = null,
    val storeName: String? = null,
    val userId: String,
    val userName: String,
    val total: Double,
    val notes: String? = null,
    val items: List<PurchaseProductResponse>,
    val purchaseDate: String,
    val createdAt: String? = null
)

data class PurchaseProductResponse(
    val id: String,
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val subtotal: Double
)

data class UpdatePurchaseRequest(
    val storeId: String? = null,
    val notes: String? = null
)
