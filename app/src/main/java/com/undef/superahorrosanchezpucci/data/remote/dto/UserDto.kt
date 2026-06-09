package com.undef.superahorrosanchezpucci.data.remote.dto

data class UserProfileResponse(
    val id: String,
    val fullName: String,
    val email: String,
    val phone: String? = null,
    val alternativePhone: String? = null,
    val profilePictureUrl: String? = null,
    val role: String,
    val verified: Boolean,
    val createdAt: String? = null
)
