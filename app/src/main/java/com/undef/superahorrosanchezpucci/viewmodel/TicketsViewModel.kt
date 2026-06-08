package com.undef.superahorrosanchezpucci.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.undef.superahorrosanchezpucci.data.model.Presupuesto
import com.undef.superahorrosanchezpucci.data.model.Ticket
import com.undef.superahorrosanchezpucci.data.model.TicketImageAnalysis
import kotlinx.coroutines.flow.StateFlow

class TicketsViewModel(application: Application) : AndroidViewModel(application) {
    private val store = AppStateStore.get(application)

    val presupuestos: StateFlow<List<Presupuesto>> = store.presupuestos
    val tickets: StateFlow<List<Ticket>> = store.tickets
    val isLoading: StateFlow<Boolean> = store.isLoading

    fun cambiarPresupuestoActivo(id: String) = store.cambiarPresupuestoActivo(id)

    fun agregarTicket(ticket: Ticket) = store.agregarTicket(ticket)

    fun eliminarTicket(id: String) = store.eliminarTicket(id)

    fun actualizarTicket(ticket: Ticket) = store.actualizarTicket(ticket)

    suspend fun analizarTicketImagen(imageBytes: ByteArray, mimeType: String): TicketImageAnalysis {
        return store.analizarTicketImagen(imageBytes, mimeType)
    }
}
