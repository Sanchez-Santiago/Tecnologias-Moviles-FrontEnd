package com.undef.superahorrosanchezpucci.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object ApiConfig {
    private const val TAG = "ApiConfig"
    
    // Lista de URLs para intentar. 
    // 1. Local (para emulador apuntando a localhost)
    // 2. Desarrollo/Producción en Render
    private val BASE_URLS = listOf(
        "http://10.0.2.2:3000",
        "https://tecnologias-moviles-backend.onrender.com"
    )

    private var currentBaseUrl: String? = null

    val activeBaseUrl: String?
        get() = currentBaseUrl

    const val REGISTER_PATH = "/api/auth/register"
    const val LOGIN_PATH = "/api/auth/login"
    const val ME_PATH = "/api/users/me"

    /**
     * Retorna la URL base activa. Si no se ha determinado aún, intenta encontrar una funcional.
     */
    suspend fun getBaseUrl(): String {
        currentBaseUrl?.let { return it }
        
        return withContext(Dispatchers.IO) {
            for (baseUrl in BASE_URLS) {
                if (isReachable(baseUrl)) {
                    Log.d(TAG, "Conectado a: $baseUrl")
                    currentBaseUrl = baseUrl
                    return@withContext baseUrl
                }
            }
            // Si ninguno responde, usamos el último por defecto
            val default = BASE_URLS.last()
            currentBaseUrl = default
            default
        }
    }

    private fun isReachable(baseUrl: String): Boolean {
        return try {
            val connection = URL("$baseUrl/api/health").openConnection() as HttpURLConnection
            connection.connectTimeout = 2000
            connection.readTimeout = 2000
            connection.requestMethod = "GET"
            val responseCode = connection.responseCode
            connection.disconnect()
            responseCode in 200..399
        } catch (e: Exception) {
            // Reintento rápido si el /api/health no existe pero el server sí responde algo
            try {
                val connection = URL(baseUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 1500
                connection.requestMethod = "HEAD"
                val responseCode = connection.responseCode
                connection.disconnect()
                true
            } catch (e2: Exception) {
                false
            }
        }
    }
    
    /**
     * Fuerza la URL base (útil para pruebas o si falla una petición)
     */
    fun setBaseUrl(url: String) {
        currentBaseUrl = url
    }
}
