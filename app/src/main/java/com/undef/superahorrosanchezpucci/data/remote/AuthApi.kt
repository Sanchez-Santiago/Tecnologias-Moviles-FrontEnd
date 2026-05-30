package com.undef.superahorrosanchezpucci.data.remote

import android.content.Context
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object ApiConfig {
    const val BASE_URL = "https://tecnologias-moviles-backend.onrender.com"
    const val REGISTER_PATH = "/api/auth/register"
    const val LOGIN_PATH = "/api/auth/login"
    const val ME_PATH = "/api/users/me"
}

data class AuthSession(
    val token: String?,
    val refreshToken: String?,
    val user: RemoteUser?
)

data class RemoteUser(
    val id: String?,
    val name: String?,
    val email: String?
)

class AuthApi(private val context: Context? = null) {

    fun register(name: String, email: String, password: String): AuthSession {
        val body = JSONObject()
            .put("fullName", name)
            .put("email", email)
            .put("password", password)

        return request(ApiConfig.REGISTER_PATH, "POST", body).toAuthSession().also { saveSession(it) }
    }

    fun login(email: String, password: String): AuthSession {
        val body = JSONObject()
            .put("email", email)
            .put("password", password)

        val session = request(ApiConfig.LOGIN_PATH, "POST", body).toAuthSession()
        val token = session.token

        val resolvedSession = if (token.isNullOrBlank()) {
            session
        } else {
            session.copy(user = session.user ?: me(token))
        }
        saveSession(resolvedSession)
        return resolvedSession
    }

    fun me(token: String): RemoteUser? {
        return request(ApiConfig.ME_PATH, "GET", token = token).extractUser()
    }

    private fun request(
        path: String,
        method: String,
        body: JSONObject? = null,
        token: String? = null
    ): JSONObject {
        val connection = (URL("${ApiConfig.BASE_URL}$path").openConnection() as HttpURLConnection)
        connection.requestMethod = method
        connection.setRequestProperty("Accept", "application/json")
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000

        if (!token.isNullOrBlank()) {
            connection.setRequestProperty("Authorization", "Bearer $token")
        }

        if (body != null) {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(body.toString())
            }
        }

        val responseCode = connection.responseCode
        val responseText = readResponse(connection, responseCode)
        connection.disconnect()

        val json = responseText
            .takeIf { it.isNotBlank() }
            ?.let { JSONObject(it) }
            ?: JSONObject()

        if (responseCode !in 200..299) {
            val message = json.optString("message")
                .ifBlank { json.optString("error") }
                .ifBlank { "Error del servidor ($responseCode)" }
            throw IllegalStateException(message)
        }

        return json
    }

    private fun readResponse(connection: HttpURLConnection, responseCode: Int): String {
        val stream = if (responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream ?: connection.inputStream
        }

        return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
            reader.readText()
        }
    }

    private fun JSONObject.toAuthSession(): AuthSession {
        val token = optString("token")
            .ifBlank { optString("accessToken") }
            .ifBlank { optJSONObject("data")?.optString("token").orEmpty() }
            .ifBlank { optJSONObject("data")?.optString("accessToken").orEmpty() }
            .takeIf { it.isNotBlank() }

        val refreshToken = optString("refreshToken")
            .ifBlank { optJSONObject("data")?.optString("refreshToken").orEmpty() }
            .takeIf { it.isNotBlank() }

        return AuthSession(token = token, refreshToken = refreshToken, user = extractUser())
    }

    private fun JSONObject.extractUser(): RemoteUser? {
        val source = optJSONObject("user")
            ?: optJSONObject("usuario")
            ?: optJSONObject("data")?.optJSONObject("user")
            ?: optJSONObject("data")?.optJSONObject("usuario")
            ?: this

        val email = source.optString("email").takeIf { it.isNotBlank() }
        val name = source.optString("name")
            .ifBlank { source.optString("nombre") }
            .ifBlank { source.optString("fullName") }
            .takeIf { it.isNotBlank() }
        val id = source.optString("id")
            .ifBlank { source.optString("_id") }
            .takeIf { it.isNotBlank() }

        return if (email == null && name == null && id == null) {
            null
        } else {
            RemoteUser(id = id, name = name, email = email)
        }
    }

    private fun saveSession(session: AuthSession) {
        val appContext = context?.applicationContext ?: return
        AuthSessionStore.save(appContext, session.token, session.refreshToken)
    }
}
