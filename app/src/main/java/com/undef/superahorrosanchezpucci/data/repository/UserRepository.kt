package com.undef.superahorrosanchezpucci.data.repository

import android.content.Context
import com.undef.superahorrosanchezpucci.data.local.AppDao
import com.undef.superahorrosanchezpucci.data.local.UsuarioEntity
import com.undef.superahorrosanchezpucci.data.model.RolUsuario
import com.undef.superahorrosanchezpucci.data.model.Usuario
import com.undef.superahorrosanchezpucci.data.remote.ApiService
import com.undef.superahorrosanchezpucci.data.remote.AuthSessionStore
import com.undef.superahorrosanchezpucci.data.remote.RetrofitClient
import com.undef.superahorrosanchezpucci.data.remote.dto.LoginRequest
import com.undef.superahorrosanchezpucci.data.remote.dto.RegisterRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository(
    private val apiService: ApiService,
    private val appDao: AppDao,
    private val context: Context
) {
    suspend fun login(email: String, password: String): Result<Usuario> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.login(LoginRequest(email, password))
            if (response.isSuccessful && response.body()?.success == true) {
                val authData = response.body()!!.data!!
                AuthSessionStore.save(context, authData.accessToken, authData.refreshToken, authData.user.id)

                val usuario = Usuario(
                    id = authData.user.id,
                    nombre = authData.user.fullName,
                    email = authData.user.email,
                    rol = if (authData.user.role == "ADMIN") RolUsuario.ADMIN else RolUsuario.MIEMBRO,
                    activo = true
                )

                appDao.clearUsuarios()
                appDao.insertUsuarios(listOf(usuario.toEntity()))
                Result.success(usuario)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error de autenticación"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(fullName: String, email: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.register(RegisterRequest(email, password, fullName))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error de registro"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchProfile(): Usuario? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getProfile()
            if (response.isSuccessful && response.body()?.success == true) {
                val profile = response.body()!!.data!!
                val usuario = Usuario(
                    id = profile.id,
                    nombre = profile.fullName,
                    email = profile.email,
                    rol = if (profile.role == "ADMIN") RolUsuario.ADMIN else RolUsuario.MIEMBRO,
                    activo = true
                )
                appDao.clearUsuarios()
                appDao.insertUsuarios(listOf(usuario.toEntity()))
                return@withContext usuario
            }
        } catch (_: Exception) {}
        
        appDao.getUsuarios().firstOrNull()?.let { u ->
            Usuario(id = u.id, nombre = u.nombre, email = u.email,
                rol = try { RolUsuario.valueOf(u.rol) } catch (_: Exception) { RolUsuario.MIEMBRO },
                activo = u.activo)
        }
    }

    suspend fun updateProfile(nombre: String, email: String): Usuario? = withContext(Dispatchers.IO) {
        try {
            val body = mapOf("fullName" to nombre, "email" to email)
            val response = apiService.updateProfile(body)
            if (response.isSuccessful) {
                val profile = response.body()?.data
                if (profile != null) {
                    val usuario = Usuario(
                        id = profile.id,
                        nombre = profile.fullName,
                        email = profile.email,
                        rol = if (profile.role == "ADMIN") RolUsuario.ADMIN else RolUsuario.MIEMBRO,
                        activo = true
                    )
                    appDao.clearUsuarios()
                    appDao.insertUsuarios(listOf(usuario.toEntity()))
                    return@withContext usuario
                }
            }
        } catch (_: Exception) {}
        null
    }

    suspend fun changePassword(current: String, new: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.changePassword(mapOf("currentPassword" to current, "newPassword" to new))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data?.get("message") ?: "Contraseña actualizada")
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error al cambiar la contraseña"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        AuthSessionStore.clear(context)
        RetrofitClient.reset()
    }

    private fun Usuario.toEntity() = UsuarioEntity(
        id = id, nombre = nombre, email = email, rol = rol.name, activo = activo
    )
}
