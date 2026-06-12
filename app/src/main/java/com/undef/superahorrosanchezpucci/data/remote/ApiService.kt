package com.undef.superahorrosanchezpucci.data.remote

import com.undef.superahorrosanchezpucci.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<AuthResponse>>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<AuthResponse>>

    @POST("api/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshRequest): Response<ApiResponse<AuthResponse>>

    @GET("api/users/me")
    suspend fun getProfile(): Response<ApiResponse<UserProfileResponse>>

    @PUT("api/users/me")
    suspend fun updateProfile(@Body body: Map<String, String>): Response<ApiResponse<UserProfileResponse>>

    @PUT("api/users/me/password")
    suspend fun changePassword(@Body body: Map<String, String>): Response<ApiResponse<Map<String, String>>>

    @GET("api/stores")
    suspend fun getStores(): Response<ApiResponse<List<StoreResponse>>>

    @GET("api/categories")
    suspend fun getCategories(): Response<ApiResponse<List<CategoryResponse>>>

    @GET("api/products")
    suspend fun getProducts(@Query("categoryId") categoryId: String? = null): Response<ApiResponse<List<ProductResponse>>>

    @GET("api/purchases")
    suspend fun getPurchases(@Query("groupId") groupId: String): Response<ApiResponse<List<PurchaseResponse>>>

    @POST("api/purchases")
    suspend fun createPurchase(@Body request: CreatePurchaseRequest): Response<ApiResponse<PurchaseResponse>>

    @PUT("api/purchases/{id}")
    suspend fun updatePurchase(@Path("id") id: String, @Body request: UpdatePurchaseRequest): Response<ApiResponse<PurchaseResponse>>

    @DELETE("api/purchases/{id}")
    suspend fun deletePurchase(@Path("id") id: String): Response<ApiResponse<String>>

    @GET("api/offers/active")
    suspend fun getActiveOffers(@Query("storeId") storeId: String? = null): Response<ApiResponse<List<OfferResponse>>>

    @GET("api/offers/match")
    suspend fun matchOffers(@Query("productId") productIds: List<String>, @Query("storeId") storeId: String? = null): Response<ApiResponse<List<MatchedOfferResponse>>>

    @POST("api/offers/ai-suggest")
    suspend fun aiSuggestOffers(@Body request: AiOfferSuggestionRequest): Response<ApiResponse<AiOfferSuggestionResponse>>

    @POST("api/tickets/analyze-image")
    suspend fun analyzeTicketImage(@Body request: AnalyzeTicketImageRequest): Response<ApiResponse<AnalyzeTicketImageResponse>>

    @GET("api/statistics/group/{groupId}/spending-by-category")
    suspend fun getSpendingByCategory(@Path("groupId") groupId: String): Response<ApiResponse<List<SpendingByCategory>>>

    @GET("api/statistics/group/{groupId}/spending-by-importance")
    suspend fun getSpendingByImportance(@Path("groupId") groupId: String): Response<ApiResponse<List<SpendingByImportance>>>

    @GET("api/statistics/group/{groupId}/spending-by-store")
    suspend fun getSpendingByStore(@Path("groupId") groupId: String): Response<ApiResponse<List<SpendingByStore>>>

    @GET("api/statistics/group/{groupId}/monthly-summary")
    suspend fun getMonthlySummary(@Path("groupId") groupId: String): Response<ApiResponse<List<MonthlySummary>>>

    @GET("api/statistics/group/{groupId}/most-frequent-store")
    suspend fun getMostFrequentStore(@Path("groupId") groupId: String): Response<ApiResponse<List<StoreFrequency>>>

    @GET("api/statistics/group/{groupId}/most-purchased-products")
    suspend fun getMostPurchasedProducts(@Path("groupId") groupId: String): Response<ApiResponse<List<MostPurchasedProduct>>>

    @GET("api/statistics/group/{groupId}/member-spending")
    suspend fun getMemberSpending(@Path("groupId") groupId: String): Response<ApiResponse<List<MemberSpending>>>

    @GET("api/statistics/group/{groupId}/budget-progress")
    suspend fun getBudgetProgress(@Path("groupId") groupId: String): Response<ApiResponse<List<BudgetProgress>>>

    @GET("api/groups")
    suspend fun getMyGroups(): Response<ApiResponse<List<GroupResponse>>>

    @GET("api/groups/{id}")
    suspend fun getGroupById(@Path("id") id: String): Response<ApiResponse<GroupDetailResponse>>

    @POST("api/groups")
    suspend fun createGroup(@Body request: CreateGroupRequest): Response<ApiResponse<GroupResponse>>

    @GET("api/budgets")
    suspend fun getBudgets(@Query("groupId") groupId: String): Response<ApiResponse<List<BudgetResponse>>>

    @GET("api/budgets/{id}")
    suspend fun getBudgetById(@Path("id") id: String): Response<ApiResponse<BudgetResponse>>

    @POST("api/budgets")
    suspend fun createBudget(@Body request: CreateBudgetRequest): Response<ApiResponse<BudgetResponse>>

    @PUT("api/budgets/{id}")
    suspend fun updateBudget(@Path("id") id: String, @Body request: UpdateBudgetRequest): Response<ApiResponse<BudgetResponse>>

    @PATCH("api/budgets/{id}/activate")
    suspend fun activateBudget(@Path("id") id: String): Response<ApiResponse<BudgetResponse>>

    @GET("api/notifications")
    suspend fun getNotifications(): Response<ApiResponse<List<NotificationResponse>>>

    @GET("api/notifications/unread/count")
    suspend fun getUnreadNotificationsCount(): Response<ApiResponse<UnreadCountResponse>>

    @PUT("api/notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: String): Response<ApiResponse<NotificationResponse>>

    @PUT("api/notifications/read-all")
    suspend fun markAllNotificationsRead(): Response<ApiResponse<String>>

    @DELETE("api/notifications/{id}")
    suspend fun deleteNotification(@Path("id") id: String): Response<ApiResponse<String>>

    // Shopping Lists
    @GET("api/shopping-lists")
    suspend fun getShoppingLists(@Query("groupId") groupId: String): Response<ApiResponse<List<ShoppingListResponse>>>

    @GET("api/shopping-lists/{id}")
    suspend fun getShoppingListById(@Path("id") id: String): Response<ApiResponse<ShoppingListResponse>>

    @POST("api/shopping-lists")
    suspend fun createShoppingList(@Body request: CreateShoppingListRequest): Response<ApiResponse<ShoppingListResponse>>

    @PUT("api/shopping-lists/{id}")
    suspend fun updateShoppingList(@Path("id") id: String, @Body request: UpdateShoppingListRequest): Response<ApiResponse<ShoppingListResponse>>

    @DELETE("api/shopping-lists/{id}")
    suspend fun deleteShoppingList(@Path("id") id: String): Response<ApiResponse<String>>

    // Shopping List Products
    @POST("api/shopping-lists/{id}/products")
    suspend fun addProductToList(@Path("id") listId: String, @Body request: AddProductRequest): Response<ApiResponse<ShoppingListResponse>>

    @PUT("api/shopping-lists/{id}/products/{productId}")
    suspend fun updateProductInList(@Path("id") listId: String, @Path("productId") productId: String, @Body request: UpdateProductRequest): Response<ApiResponse<ShoppingListResponse>>

    @DELETE("api/shopping-lists/{id}/products/{productId}")
    suspend fun deleteProductFromList(@Path("id") listId: String, @Path("productId") productId: String): Response<ApiResponse<ShoppingListResponse>>

    @POST("api/groups/{id}/invitations")
    suspend fun inviteMember(@Path("id") groupId: String, @Body request: InviteRequest): Response<ApiResponse<InvitationResponse>>

    @GET("api/groups/invitations")
    suspend fun getMyInvitations(): Response<ApiResponse<List<InvitationResponse>>>

    @POST("api/groups/invitations/{token}/accept")
    suspend fun acceptInvitation(@Path("token") token: String): Response<ApiResponse<InvitationResponse>>

    @POST("api/groups/invitations/{token}/reject")
    suspend fun rejectInvitation(@Path("token") token: String): Response<ApiResponse<InvitationResponse>>
}
