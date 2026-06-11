package com.undef.superahorrosanchezpucci.data.remote.dto

data class NotificationResponse(
    val id: String,
    val type: String,
    val title: String,
    val message: String,
    val data: String? = null,
    val read: Boolean,
    val createdAt: String
)

data class UnreadCountResponse(
    val count: Int
)
