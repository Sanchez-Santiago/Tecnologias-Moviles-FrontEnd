package com.undef.superahorrosanchezpucci.data.remote

import com.undef.superahorrosanchezpucci.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
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

    @GET("api/statistics/group/{groupId}/spending-by-category")
    suspend fun getSpendingByCategory(@Path("groupId") groupId: String): Response<ApiResponse<List<SpendingByCategory>>>

    @GET("api/statistics/group/{groupId}/spending-by-store")
    suspend fun getSpendingByStore(@Path("groupId") groupId: String): Response<ApiResponse<List<SpendingByStore>>>

    @GET("api/statistics/group/{groupId}/monthly-summary")
    suspend fun getMonthlySummary(@Path("groupId") groupId: String): Response<ApiResponse<List<MonthlySummary>>>

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

    @POST("api/groups/{id}/invitations")
    suspend fun inviteMember(@Path("id") groupId: String, @Body request: InviteRequest): Response<ApiResponse<InvitationResponse>>

    @GET("api/groups/invitations")
    suspend fun getMyInvitations(): Response<ApiResponse<List<InvitationResponse>>>

    @POST("api/groups/invitations/{token}/accept")
    suspend fun acceptInvitation(@Path("token") token: String): Response<ApiResponse<InvitationResponse>>

    @POST("api/groups/invitations/{token}/reject")
    suspend fun rejectInvitation(@Path("token") token: String): Response<ApiResponse<InvitationResponse>>
}
