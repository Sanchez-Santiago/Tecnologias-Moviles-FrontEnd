package com.undef.superahorrosanchezpucci.data.remote.dto

data class AnalyzeTicketImageRequest(
    val imageBase64: String,
    val mimeType: String = "image/jpeg"
)

data class AnalyzeTicketImageResponse(
    val storeName: String? = null,
    val purchaseDate: String? = null,
    val total: Double? = null,
    val products: List<TicketProductDetection> = emptyList()
)

data class TicketProductDetection(
    val name: String,
    val quantity: Double? = null,
    val unitPrice: Double? = null,
    val totalPrice: Double? = null
)
