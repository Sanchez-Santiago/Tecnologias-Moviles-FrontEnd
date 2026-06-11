package com.undef.superahorrosanchezpucci.data.remote

import com.undef.superahorrosanchezpucci.data.remote.dto.ApiResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URL
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    private val gson = Gson()

    private var apiService: ApiService? = null
    
    // Interceptor para cambiar la URL base dinámicamente
    private val dynamicBaseUrlInterceptor = object : okhttp3.Interceptor {
        override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
            var request = chain.request()
            // Si ya tenemos una URL base determinada en ApiConfig, la usamos
            val baseUrl = ApiConfig.activeBaseUrl 
            if (baseUrl != null) {
                val urlObj = URL(baseUrl)
                val newUrl = request.url.newBuilder()
                    .scheme(urlObj.protocol)
                    .host(urlObj.host)
                    .port(if (urlObj.port != -1) urlObj.port else urlObj.defaultPort)
                    .build()
                request = request.newBuilder().url(newUrl).build()
            }
            return chain.proceed(request)
        }
    }

    fun getApiService(): ApiService {
        if (apiService == null) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(dynamicBaseUrlInterceptor)
                .addInterceptor { chain ->
                    val original = chain.request()
                    val token = AuthSessionStore.accessToken
                    if (token.isNullOrBlank()) {
                        chain.proceed(original)
                    } else {
                        chain.proceed(original.newBuilder()
                            .header("Authorization", "Bearer $token")
                            .build())
                    }
                }
                .addInterceptor(logging)
                .authenticator { _, response ->
                    if (response.request.url.encodedPath.contains("/auth/refresh")) {
                        null
                    } else {
                        val refreshToken = AuthSessionStore.refreshToken ?: return@authenticator null
                        val json = """{"refreshToken":"$refreshToken"}"""
                        val baseUrl = ApiConfig.activeBaseUrl ?: "https://tecnologias-moviles-backend.onrender.com/"
                        val refreshRequest = Request.Builder()
                            .url("${baseUrl}api/auth/refresh")
                            .post(json.toRequestBody(JSON_MEDIA_TYPE))
                            .build()

                        val refreshResponse = OkHttpClient().newCall(refreshRequest).execute()
                        if (!refreshResponse.isSuccessful) null
                        else {
                            val bodyStr = refreshResponse.body?.string() ?: return@authenticator null
                            try {
                                val type = object : TypeToken<ApiResponse<Map<String, Any>>>() {}.type
                                val apiResp: ApiResponse<Map<String, Any>> = gson.fromJson(bodyStr, type)
                                if (apiResp.success != true || apiResp.data == null) null
                                else {
                                    val newAccess = apiResp.data["accessToken"] as? String ?: return@authenticator null
                                    val newRefresh = apiResp.data["refreshToken"] as? String ?: return@authenticator null
                                    AuthSessionStore.save(newAccess, newRefresh)
                                    response.request.newBuilder()
                                        .header("Authorization", "Bearer $newAccess")
                                        .build()
                                }
                            } catch (_: Exception) {
                                null
                            }
                        }
                    }
                }
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://tecnologias-moviles-backend.onrender.com/") // Placeholder
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            apiService = retrofit.create(ApiService::class.java)
        }
        return apiService!!
    }

    fun reset() {
        apiService = null
    }
}
