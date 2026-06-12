package com.undef.superahorrosanchezpucci.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.undef.superahorrosanchezpucci.data.model.ListaCompra
import com.undef.superahorrosanchezpucci.data.model.Presupuesto
import com.undef.superahorrosanchezpucci.data.model.Producto
import kotlinx.coroutines.flow.StateFlow

class ListaViewModel(application: Application) : AndroidViewModel(application) {
    private val store = AppStateStore.get(application)

    val presupuestos: StateFlow<List<Presupuesto>> = store.presupuestos
    val listas: StateFlow<List<ListaCompra>> = store.listas
    val isLoading: StateFlow<Boolean> = store.isLoading

    fun cambiarPresupuestoActivo(id: String) = store.cambiarPresupuestoActivo(id)

    fun cambiarModo(individual: Boolean, onResult: ((Result<Unit>) -> Unit)? = null) = store.cambiarModo(individual, onResult)

    fun agregarProducto(listaId: String, producto: Producto) = store.agregarProducto(listaId, producto)

    fun eliminarProducto(listaId: String, productoId: String) = store.eliminarProducto(listaId, productoId)

    fun toggleProducto(listaId: String, productoId: String) = store.toggleProducto(listaId, productoId)

    suspend fun buscarEnCatalogo(query: String) = store.buscarEnCatalogo(query)
}
