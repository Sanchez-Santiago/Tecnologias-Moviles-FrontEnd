package com.undef.superahorrosanchezpucci.data.remote.dto

data class ProductResponse(
    val id: String,
    val name: String,
    val price: Double,
    val categoryId: String,
    val categoryName: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val barcode: String? = null,
    val priority: String,
    val active: Boolean
)
