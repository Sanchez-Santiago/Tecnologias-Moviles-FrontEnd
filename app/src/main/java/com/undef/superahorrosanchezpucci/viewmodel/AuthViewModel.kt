package com.undef.superahorrosanchezpucci.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.undef.superahorrosanchezpucci.data.model.Usuario

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val store = AppStateStore.get(application)

    fun login(email: String, password: String, onResult: (Result<Usuario>) -> Unit) {
        store.login(email, password, onResult)
    }

    fun register(name: String, email: String, password: String, onResult: (Result<Unit>) -> Unit) {
        store.register(name, email, password, onResult)
    }
}
