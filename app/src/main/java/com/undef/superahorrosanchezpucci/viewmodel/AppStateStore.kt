package com.undef.superahorrosanchezpucci.viewmodel

import android.app.Application
import com.undef.superahorrosanchezpucci.data.model.Categoria
import com.undef.superahorrosanchezpucci.data.model.ListaCompra
import com.undef.superahorrosanchezpucci.data.model.Presupuesto
import com.undef.superahorrosanchezpucci.data.model.Producto
import com.undef.superahorrosanchezpucci.data.model.Ticket
import com.undef.superahorrosanchezpucci.data.model.Usuario
import com.undef.superahorrosanchezpucci.data.remote.AuthSessionStore
import com.undef.superahorrosanchezpucci.data.repository.AppRepository
import com.undef.superahorrosanchezpucci.ui.theme.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppStateStore private constructor(application: Application) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val repository = AppRepository(application)

    private val _presupuestos = MutableStateFlow<List<Presupuesto>>(emptyList())
    val presupuestos: StateFlow<List<Presupuesto>> = _presupuestos.asStateFlow()

    private val _listas = MutableStateFlow<List<ListaCompra>>(emptyList())
    val listas: StateFlow<List<ListaCompra>> = _listas.asStateFlow()

    private val _tickets = MutableStateFlow<List<Ticket>>(emptyList())
    val tickets: StateFlow<List<Ticket>> = _tickets.asStateFlow()

    private val _usuarios = MutableStateFlow<List<Usuario>>(emptyList())
    val usuarios: StateFlow<List<Usuario>> = _usuarios.asStateFlow()

    private val _usuarioActual = MutableStateFlow<Usuario?>(null)
    val usuarioActual: StateFlow<Usuario?> = _usuarioActual.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        AuthSessionStore.initialize(application.applicationContext)
        reload()
    }

    fun reload() {
        scope.launch {
            _isLoading.value = true
            runCatching { repository.cargarTodo() }
            refrescar()
            _themeMode.value = repository.themeMode
            _isLoading.value = false
        }
    }

    fun agregarProducto(listaId: String, producto: Producto) {
        scope.launch {
            val listaExiste = repository.listas.any { it.id == listaId }
            if (!listaExiste) {
                val nombreLista = if (listaId == "lista-familiar") "Lista Familiar" else "Lista Individual"
                repository.agregarLista(ListaCompra(id = listaId, nombre = nombreLista))
            }

            repository.agregarOActualizarProducto(listaId, producto)
            refrescar()
        }
    }

    fun eliminarProducto(listaId: String, productoId: String) {
        scope.launch {
            repository.eliminarProducto(listaId, productoId)
            refrescar()
        }
    }

    fun toggleProducto(listaId: String, productoId: String) {
        scope.launch {
            repository.toggleProducto(listaId, productoId)
            refrescar()
        }
    }

    fun actualizarPresupuesto(id: String, monto: Int) {
        scope.launch {
            repository.actualizarPresupuesto(id, monto)
            refrescar()
        }
    }

    fun cambiarPresupuestoActivo(id: String) {
        scope.launch {
            repository.cambiarPresupuestoActivo(id)
            refrescar()
        }
    }

    fun agregarTicket(ticket: Ticket) {
        scope.launch {
            repository.agregarTicket(ticket)
            refrescar()
        }
    }

    fun eliminarTicket(id: String) {
        scope.launch {
            repository.eliminarTicket(id)
            refrescar()
        }
    }

    fun actualizarTicket(ticket: Ticket) {
        scope.launch {
            repository.actualizarTicket(ticket)
            refrescar()
        }
    }

    fun agregarUsuario(usuario: Usuario) {
        scope.launch {
            repository.agregarUsuario(usuario)
            refrescar()
        }
    }

    fun logout() {
        scope.launch {
            repository.logout()
            _presupuestos.value = emptyList()
            _listas.value = emptyList()
            _tickets.value = emptyList()
            _usuarios.value = emptyList()
            _usuarioActual.value = null
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        scope.launch {
            repository.updateThemeMode(mode)
        }
    }

    fun getEstimadosPorCategoria(listaId: String): Map<Categoria, Int> {
        val lista = _listas.value.find { it.id == listaId } ?: return emptyMap()
        return lista.productos
            .groupBy { it.categoria }
            .mapValues { entry ->
                entry.value.sumOf { it.precioEstimado * it.cantidad }
            }
    }

    private fun refrescar() {
        _presupuestos.value = repository.presupuestos.toList()
        _listas.value = repository.listas.toList()
        _tickets.value = repository.tickets.toList()
        _usuarios.value = repository.usuarios.toList()
        _usuarioActual.value = repository.usuarioActual
    }

    companion object {
        @Volatile
        private var instance: AppStateStore? = null

        fun get(application: Application): AppStateStore {
            return instance ?: synchronized(this) {
                instance ?: AppStateStore(application).also { instance = it }
            }
        }
    }
}
