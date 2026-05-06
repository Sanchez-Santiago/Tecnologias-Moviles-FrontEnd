package com.example.misuper.viewmodel

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import com.example.misuper.data.local.JsonStorage
import com.example.misuper.data.model.*
import com.example.misuper.data.repository.AppRepository

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(
        JsonStorage(application)
    )

    var presupuestos = mutableStateListOf<Presupuesto>()
        private set

    var listas = mutableStateListOf<ListaCompra>()
        private set

    var tickets = mutableStateListOf<Ticket>()
        private set

    var usuarios = mutableStateListOf<Usuario>()
        private set

    init {
        cargarDatos()
    }

    private fun cargarDatos() {
        repository.cargarTodo()

        presupuestos.clear()
        presupuestos.addAll(repository.presupuestos)

        listas.clear()
        listas.addAll(repository.listas)

        tickets.clear()
        tickets.addAll(repository.tickets)

        usuarios.clear()
        usuarios.addAll(repository.usuarios)
    }

    // ----------------------
    // LISTAS
    // ----------------------

    fun agregarProducto(listaId: String, producto: Producto) {
        repository.agregarOActualizarProducto(listaId, producto)
        refrescar()
    }

    fun eliminarProducto(listaId: String, productoId: String) {
        repository.eliminarProducto(listaId, productoId)
        refrescar()
    }

    fun toggleProducto(listaId: String, productoId: String) {
        repository.toggleProducto(listaId, productoId)
        refrescar()
    }

    // ----------------------
    // PRESUPUESTO
    // ----------------------

    fun actualizarPresupuesto(id: String, monto: Int) {
        repository.actualizarPresupuesto(id, monto)
        refrescar()
    }

    fun cambiarPresupuestoActivo(id: String) {
        repository.cambiarPresupuestoActivo(id)
        refrescar()
    }

    // ----------------------
    // TICKETS
    // ----------------------

    fun agregarTicket(ticket: Ticket) {
        repository.agregarTicket(ticket)
        refrescar()
    }

    fun eliminarTicket(id: String) {
        repository.eliminarTicket(id)
        refrescar()
    }

    fun actualizarTicket(ticket: Ticket) {
        repository.actualizarTicket(ticket)
        refrescar()
    }

    // ----------------------
    // USUARIOS / FAMILIA
    // ----------------------

    fun agregarUsuario(usuario: Usuario) {
        repository.agregarUsuario(usuario)
        refrescar()
    }

    // ----------------------
    // ANALYTICS
    // ----------------------

    fun getGastosPorCategoria(listaId: String): Map<Categoria, Int> {
        val lista = listas.find { it.id == listaId } ?: return emptyMap()
        return lista.productos
            .filter { it.comprado }
            .groupBy { it.categoria }
            .mapValues { entry ->
                entry.value.sumOf { (it.precioReal ?: it.precioEstimado) * it.cantidad }
            }
    }

    fun getEstimadosPorCategoria(listaId: String): Map<Categoria, Int> {
        val lista = listas.find { it.id == listaId } ?: return emptyMap()
        return lista.productos
            .groupBy { it.categoria }
            .mapValues { entry ->
                entry.value.sumOf { it.precioEstimado * it.cantidad }
            }
    }

    // ----------------------
    // REFRESH UI
    // ----------------------

    private fun refrescar() {
        presupuestos.clear()
        presupuestos.addAll(repository.presupuestos)

        listas.clear()
        listas.addAll(repository.listas)

        tickets.clear()
        tickets.addAll(repository.tickets)

        usuarios.clear()
        usuarios.addAll(repository.usuarios)
    }
}