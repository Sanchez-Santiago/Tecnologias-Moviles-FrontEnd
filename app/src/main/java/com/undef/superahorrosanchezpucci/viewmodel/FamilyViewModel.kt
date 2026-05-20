package com.undef.superahorrosanchezpucci.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.undef.superahorrosanchezpucci.data.model.Usuario
import kotlinx.coroutines.flow.StateFlow

class FamilyViewModel(application: Application) : AndroidViewModel(application) {
    private val store = AppStateStore.get(application)

    val usuarios: StateFlow<List<Usuario>> = store.usuarios

    fun agregarUsuario(usuario: Usuario) = store.agregarUsuario(usuario)
}
