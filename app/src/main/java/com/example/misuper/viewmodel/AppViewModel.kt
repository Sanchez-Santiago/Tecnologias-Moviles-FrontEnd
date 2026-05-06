package com.example.misuper.viewmodel

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.misuper.data.local.JsonStorage
import com.example.misuper.data.model.*
import com.example.misuper.data.repository.AppRepository
import com.example.misuper.ui.theme.ThemeMode
import kotlinx.coroutines.launch

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

    var themeMode by mutableStateOf(ThemeMode.SYSTEM)
        private set

    init {
        cargarDatos()
    }

    private fun cargarDatos() {
        viewModelScope.launch {
            repository.cargarTodo()

            presupuestos.clear()
            presupuestos.addAll(repository.presupuestos)

            listas.clear()
            listas.addAll(repository.listas)

            tickets.clear()
            tickets.addAll(repository.tickets)

            usuarios.clear()
            usuarios.addAll(repository.usuarios)

            themeMode = repository.themeMode
        }
    }

    // ----------------------
    // LISTAS
    // ----------------------

    fun agregarProducto(listaId: String, producto: Producto) {
        viewModelScope.launch {
            repository.agregarOActualizarProducto(listaId, producto)
            refrescar()
        }
    }

    fun eliminarProducto(listaId: String, productoId: String) {
        viewModelScope.launch {
            repository.eliminarProducto(listaId, productoId)
            refrescar()
        }
    }

    fun toggleProducto(listaId: String, productoId: String) {
        viewModelScope.launch {
            repository.toggleProducto(listaId, productoId)
            refrescar()
        }
    }

    // ----------------------
    // PRESUPUESTO
    // ----------------------

    fun actualizarPresupuesto(id: String, monto: Int) {
        viewModelScope.launch {
            repository.actualizarPresupuesto(id, monto)
            refrescar()
        }
    }

    fun cambiarPresupuestoActivo(id: String) {
        viewModelScope.launch {
            repository.cambiarPresupuestoActivo(id)
            refrescar()
        }
    }

    // ----------------------
    // TICKETS
    // ----------------------

    fun agregarTicket(ticket: Ticket) {
        viewModelScope.launch {
            repository.agregarTicket(ticket)
            refrescar()
        }
    }

    fun eliminarTicket(id: String) {
        viewModelScope.launch {
            repository.eliminarTicket(id)
            refrescar()
        }
    }

    fun actualizarTicket(ticket: Ticket) {
        viewModelScope.launch {
            repository.actualizarTicket(ticket)
            refrescar()
        }
    }

    // ----------------------
    // USUARIOS / FAMILIA
    // ----------------------

    fun agregarUsuario(usuario: Usuario) {
        viewModelScope.launch {
            repository.agregarUsuario(usuario)
            refrescar()
        }
    }

    // ----------------------
    // THEME
    // ----------------------

    fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
        viewModelScope.launch {
            repository.updateThemeMode(mode)
        }
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