package com.undef.superahorrosanchezpucci.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.undef.superahorrosanchezpucci.data.model.Usuario
import com.undef.superahorrosanchezpucci.data.remote.dto.BudgetProgress
import com.undef.superahorrosanchezpucci.data.remote.dto.GroupDetailResponse
import com.undef.superahorrosanchezpucci.data.remote.dto.InvitationResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FamilyViewModel(application: Application) : AndroidViewModel(application) {
    private val store = AppStateStore.get(application)

    val usuarios: StateFlow<List<Usuario>> = store.usuarios
    val grupos: StateFlow<List<GroupDetailResponse>> = store.grupos
    val invitaciones: StateFlow<List<InvitationResponse>> = store.invitaciones
    val usuarioActual: StateFlow<Usuario?> = store.usuarioActual
    val isLoading: StateFlow<Boolean> = store.isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _successMsg = MutableStateFlow<String?>(null)
    val successMsg: StateFlow<String?> = _successMsg.asStateFlow()

    private val _budgetProgress = MutableStateFlow<List<BudgetProgress>>(emptyList())
    val budgetProgress: StateFlow<List<BudgetProgress>> = _budgetProgress.asStateFlow()

    fun agregarUsuario(usuario: Usuario) = store.agregarUsuario(usuario)

    fun crearGrupo(nombre: String, categoria: String) {
        store.crearGrupo(nombre, categoria) { result ->
            result.onSuccess { _successMsg.value = "Grupo '$nombre' creado con éxito" }
            result.onFailure { _error.value = it.message }
        }
    }

    fun clearError() { _error.value = null }
    fun clearSuccess() { _successMsg.value = null }

    fun invitarMiembro(groupId: String, email: String) {
        store.invitarMiembro(groupId, email) { result ->
            result.onSuccess { _successMsg.value = "Invitación enviada a $email" }
            result.onFailure {
                val msg = it.message ?: "Error al enviar invitación"
                _error.value = msg
            }
        }
    }

    fun aceptarInvitacion(token: String) = store.aceptarInvitacion(token)
    fun rechazarInvitacion(token: String) = store.rechazarInvitacion(token)

    fun loadBudgetProgress(groupId: String) {
        store.loadBudgetProgress(groupId) { result ->
            result.onSuccess { _budgetProgress.value = it }
        }
    }
}
