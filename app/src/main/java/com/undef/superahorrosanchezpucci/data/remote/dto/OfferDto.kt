package com.undef.superahorrosanchezpucci.data.remote.dto

data class OfferResponse(
    val id: String,
    val storeId: String? = null,
    val storeName: String? = null,
    val title: String,
    val description: String? = null,
    val discountType: String,
    val discountValue: Double,
    val startDate: String? = null,
    val endDate: String? = null,
    val imageUrl: String? = null,
    val termsConditions: String? = null,
    val createdAt: String? = null
)

data class AiOfferSuggestion(
    val productName: String,
    val offerId: String,
    val relevance: String,
    val explanation: String
)

data class AiOfferSuggestionResponse(
    val suggestions: List<AiOfferSuggestion>
)

data class AiOfferSuggestionRequest(
    val productNames: List<String>,
    val storeId: String? = null
)

data class MatchedOfferResponse(
    val productId: String,
    val productName: String,
    val offer: OfferResponse,
    val reason: String
)
