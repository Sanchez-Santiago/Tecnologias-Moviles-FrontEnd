package com.undef.superahorrosanchezpucci.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.undef.superahorrosanchezpucci.data.model.Presupuesto
import com.undef.superahorrosanchezpucci.data.model.Usuario
import com.undef.superahorrosanchezpucci.ui.theme.ThemeMode
import kotlinx.coroutines.flow.StateFlow

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val store = AppStateStore.get(application)

    val themeMode: StateFlow<ThemeMode> = store.themeMode
    val usuarios: StateFlow<List<Usuario>> = store.usuarios
    val presupuestos: StateFlow<List<Presupuesto>> = store.presupuestos

    fun updateThemeMode(mode: ThemeMode) = store.updateThemeMode(mode)
}
