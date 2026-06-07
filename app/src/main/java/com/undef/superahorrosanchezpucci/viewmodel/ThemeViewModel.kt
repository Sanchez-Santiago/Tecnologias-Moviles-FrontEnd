package com.undef.superahorrosanchezpucci.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.undef.superahorrosanchezpucci.ui.theme.ThemeMode
import kotlinx.coroutines.flow.StateFlow

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val store = AppStateStore.get(application)

    val themeMode: StateFlow<ThemeMode> = store.themeMode

    fun updateThemeMode(mode: ThemeMode) = store.updateThemeMode(mode)

    fun cerrarSesion() = store.cerrarSesion()
}
