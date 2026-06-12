package com.undef.superahorrosanchezpucci.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.undef.superahorrosanchezpucci.data.model.Categoria
import com.undef.superahorrosanchezpucci.data.model.ListaCompra
import com.undef.superahorrosanchezpucci.data.model.Presupuesto
import com.undef.superahorrosanchezpucci.data.model.Ticket
import com.undef.superahorrosanchezpucci.data.model.Usuario
import com.undef.superahorrosanchezpucci.data.remote.dto.*
import kotlinx.coroutines.flow.StateFlow

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val store = AppStateStore.get(application)

    val presupuestos: StateFlow<List<Presupuesto>> = store.presupuestos
    val tickets: StateFlow<List<Ticket>> = store.tickets
    val listas: StateFlow<List<ListaCompra>> = store.listas
    val usuarios: StateFlow<List<Usuario>> = store.usuarios
    val grupos: StateFlow<List<GroupDetailResponse>> = store.grupos
    val isLoading: StateFlow<Boolean> = store.isLoading
    val usuarioActual: StateFlow<Usuario?> = store.usuarioActual
    val unreadCount: StateFlow<Int> = store.unreadCount
    
    val spendingByStore: StateFlow<List<SpendingByStore>> = store.spendingByStore
    val mostPurchasedProducts: StateFlow<List<MostPurchasedProduct>> = store.mostPurchasedProducts
    val memberSpending: StateFlow<List<MemberSpending>> = store.memberSpending
    val modoIndividual: StateFlow<Boolean> = store.modoIndividual
    val grupoActivoId: StateFlow<String?> = store.grupoActivoId

    fun cambiarPresupuestoActivo(id: String) = store.cambiarPresupuestoActivo(id)
    fun actualizarPresupuesto(id: String, monto: Int) = store.actualizarPresupuesto(id, monto)
    fun getEstimadosPorCategoria(listaId: String): Map<Categoria, Int> = store.getEstimadosPorCategoria(listaId)
    fun cambiarGrupoActivo(grupoId: String) = store.cambiarGrupoActivo(grupoId)
    fun cambiarModo(individual: Boolean, onResult: ((Result<Unit>) -> Unit)? = null) = store.cambiarModo(individual, onResult)
    fun loadStats(grupoId: String) = store.loadStats(grupoId)
}
