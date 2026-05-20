package com.undef.superahorrosanchezpucci.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.undef.superahorrosanchezpucci.data.model.Categoria
import com.undef.superahorrosanchezpucci.data.model.Presupuesto
import com.undef.superahorrosanchezpucci.data.model.Ticket
import com.undef.superahorrosanchezpucci.data.model.Usuario
import kotlinx.coroutines.flow.StateFlow

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val store = AppStateStore.get(application)

    val presupuestos: StateFlow<List<Presupuesto>> = store.presupuestos
    val tickets: StateFlow<List<Ticket>> = store.tickets
    val usuarios: StateFlow<List<Usuario>> = store.usuarios
    val isLoading: StateFlow<Boolean> = store.isLoading

    fun cambiarPresupuestoActivo(id: String) = store.cambiarPresupuestoActivo(id)

    fun actualizarPresupuesto(id: String, monto: Int) = store.actualizarPresupuesto(id, monto)

    fun getEstimadosPorCategoria(listaId: String): Map<Categoria, Int> {
        return store.getEstimadosPorCategoria(listaId)
    }
}
