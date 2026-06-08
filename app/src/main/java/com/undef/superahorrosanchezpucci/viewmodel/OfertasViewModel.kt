package com.undef.superahorrosanchezpucci.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.undef.superahorrosanchezpucci.data.model.ListaCompra
import com.undef.superahorrosanchezpucci.data.model.Presupuesto
import com.undef.superahorrosanchezpucci.data.model.Ticket
import kotlinx.coroutines.flow.StateFlow

class OfertasViewModel(application: Application) : AndroidViewModel(application) {
    private val store = AppStateStore.get(application)

    val presupuestos: StateFlow<List<Presupuesto>> = store.presupuestos
    val listas: StateFlow<List<ListaCompra>> = store.listas
    val tickets: StateFlow<List<Ticket>> = store.tickets
    val isLoading: StateFlow<Boolean> = store.isLoading
}
