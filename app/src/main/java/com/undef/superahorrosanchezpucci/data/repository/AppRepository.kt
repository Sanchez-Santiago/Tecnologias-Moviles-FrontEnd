package com.undef.superahorrosanchezpucci.data.repository

import android.content.Context
import com.undef.superahorrosanchezpucci.data.local.AppDatabase
import com.undef.superahorrosanchezpucci.data.local.toEntity
import com.undef.superahorrosanchezpucci.data.local.toModel
import com.undef.superahorrosanchezpucci.data.local.toThemeMode
import com.undef.superahorrosanchezpucci.data.model.*
import com.undef.superahorrosanchezpucci.data.remote.AuthSessionStore
import com.undef.superahorrosanchezpucci.data.remote.RemoteAppApi
import com.undef.superahorrosanchezpucci.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(context: Context) {
    private val prefs = context.getSharedPreferences("repository_migrations", Context.MODE_PRIVATE)
    private val remoteApi = RemoteAppApi()
    private val dao = AppDatabase.get(context).appDao()

    var presupuestos = mutableListOf<Presupuesto>()
        private set

    var listas = mutableListOf<ListaCompra>()
        private set

    var tickets = mutableListOf<Ticket>()
        private set

    var usuarios = mutableListOf<Usuario>()
        private set

    var themeMode: ThemeMode = ThemeMode.SYSTEM
        private set

    // ----------------------
    // CARGA INICIAL
    // ----------------------

    suspend fun cargarTodo() = withContext(Dispatchers.IO) {
        cargarLocal()

        if (!AuthSessionStore.accessToken.isNullOrBlank()) {
            val needsListCleanup = !prefs.getBoolean(REMOTE_PRODUCTS_CLEANUP_DONE, false)
            val remoteState = remoteApi.loadState()
            presupuestos = mergePresupuestos(locales = presupuestos, remotos = remoteState.presupuestos).toMutableList()
            listas = if (needsListCleanup) {
                prefs.edit().putBoolean(REMOTE_PRODUCTS_CLEANUP_DONE, true).apply()
                listasBase().toMutableList()
            } else {
                listas.takeIf { it.isNotEmpty() } ?: listasBase().toMutableList()
            }
            tickets = remoteState.tickets.toMutableList()
            usuarios = mergeUsuarios(locales = usuarios, remotos = remoteState.usuarios).toMutableList()
            guardarTodo()
            return@withContext
        }

        if (presupuestos.isEmpty() || listas.isEmpty()) {
            inicializarDatos()
        }
    }

    private suspend fun inicializarDatos() {
        presupuestos.clear()
        listas.clear()
        presupuestos.addAll(presupuestosBase())
        listas.addAll(listasBase())

        guardarTodo()
    }

    // ----------------------
    // GUARDADO
    // ----------------------

    suspend fun guardarTodo() = withContext(Dispatchers.IO) {
        val productos = listas.flatMap { lista ->
            lista.productos.map { producto -> producto.toEntity(lista.id) }
        }
        val ticketProductos = tickets.flatMap { ticket ->
            ticket.productos.mapIndexed { index, producto -> producto.toEntity(ticket.id, index) }
        }
        dao.replaceAll(
            presupuestos = presupuestos.map { it.toEntity() },
            listas = listas.map { it.toEntity() },
            productos = productos,
            tickets = tickets.map { it.toEntity() },
            ticketProductos = ticketProductos,
            usuarios = usuarios.map { it.toEntity() },
            config = themeMode.toEntity()
        )
    }

    private suspend fun cargarLocal() {
        val productosPorLista = dao.getProductos()
            .groupBy { it.listaId }
            .mapValues { entry -> entry.value.map { it.toModel() } }
        listas = dao.getListas()
            .map { lista -> lista.toModel(productosPorLista[lista.id].orEmpty()) }
            .toMutableList()

        presupuestos = dao.getPresupuestos().map { it.toModel() }.toMutableList()

        val ticketProductos = dao.getTicketProductos()
            .groupBy { it.ticketId }
            .mapValues { entry -> entry.value.map { it.toModel() } }
        tickets = dao.getTickets()
            .map { ticket -> ticket.toModel(ticketProductos[ticket.id].orEmpty()) }
            .toMutableList()

        usuarios = dao.getUsuarios().map { it.toModel() }.toMutableList()
        themeMode = dao.getConfig()?.toThemeMode() ?: ThemeMode.SYSTEM
    }

    // ----------------------
    // PRESUPUESTO
    // ----------------------

    suspend fun cambiarPresupuestoActivo(id: String) {
        val nuevas = presupuestos.map {
            it.copy(activo = it.id == id)
        }
        presupuestos.clear()
        presupuestos.addAll(nuevas)
        guardarTodo()
    }

    fun gastar(presupuestoId: String, monto: Int) {
        val index = presupuestos.indexOfFirst { it.id == presupuestoId }
        if (index != -1) {
            val actual = presupuestos[index]
            presupuestos[index] = actual.copy(
                montoDisponible = actual.montoDisponible - monto
            )
        }
    }

    suspend fun actualizarPresupuesto(id: String, nuevoMonto: Int) {
        val index = presupuestos.indexOfFirst { it.id == id }
        if (index == -1) return

        val actual = presupuestos[index]
        val gastado = actual.montoTotal - actual.montoDisponible
        val localActualizado = actual.copy(
            montoTotal = nuevoMonto,
            montoDisponible = nuevoMonto - gastado
        )

        if (!AuthSessionStore.accessToken.isNullOrBlank()) {
            val esPresupuestoLocal = id == "presupuesto-familiar" || id == "presupuesto-individual"
            val remoto = runCatching {
                if (esPresupuestoLocal) {
                    remoteApi.createBudget(actual.nombre, nuevoMonto)
                } else {
                    remoteApi.updateBudget(id, nuevoMonto)
                }
            }.getOrNull()

            presupuestos[index] = if (remoto != null && !esPresupuestoLocal) {
                remoto.copy(activo = actual.activo)
            } else {
                localActualizado
            }
            guardarTodo()
            return
        }

        presupuestos[index] = localActualizado
        guardarTodo()
    }

    // ----------------------
    // LISTAS
    // ----------------------

    suspend fun agregarLista(lista: ListaCompra) {
        listas.add(lista)
        guardarTodo()
    }

    suspend fun agregarOActualizarProducto(listaId: String, producto: Producto) = withContext(Dispatchers.IO) {
        val indexLista = listas.indexOfFirst { it.id == listaId }
        if (indexLista == -1) return@withContext

        val lista = listas[indexLista]
        val nuevosProductos = lista.productos.toMutableList()
        val indexProd = nuevosProductos.indexOfFirst { it.id == producto.id }

        if (indexProd != -1) {
            nuevosProductos[indexProd] = producto
        } else {
            nuevosProductos.add(producto)
        }

        listas[indexLista] = lista.copy(productos = nuevosProductos)
        guardarTodo()
    }

    suspend fun eliminarProducto(listaId: String, productoId: String) {
        val indexLista = listas.indexOfFirst { it.id == listaId }
        if (indexLista != -1) {
            val lista = listas[indexLista]
            val nuevosProductos = lista.productos.toMutableList()
            nuevosProductos.removeAll { it.id == productoId }
            
            listas[indexLista] = lista.copy(productos = nuevosProductos)
            guardarTodo()
        }
    }

    suspend fun toggleProducto(listaId: String, productoId: String) {
        val indexLista = listas.indexOfFirst { it.id == listaId }
        if (indexLista == -1) return
        
        val lista = listas[indexLista]
        val nuevosProductos = lista.productos.toMutableList()
        val indexProd = nuevosProductos.indexOfFirst { it.id == productoId }
        if (indexProd == -1) return

        val producto = nuevosProductos[indexProd]
        val actualizado = producto.copy(comprado = !producto.comprado)
        nuevosProductos[indexProd] = actualizado

        listas[indexLista] = lista.copy(productos = nuevosProductos)

        // 🔥 impacto en presupuesto
        val presupuestoId = lista.presupuestoId
        if (actualizado.comprado) {
            gastar(presupuestoId, actualizado.precioEstimado * actualizado.cantidad)
        } else {
            gastar(presupuestoId, -(actualizado.precioEstimado * actualizado.cantidad))
        }

        guardarTodo()
    }

    // ----------------------
    // TICKETS
    // ----------------------

    suspend fun agregarTicket(ticket: Ticket) {
        if (!AuthSessionStore.accessToken.isNullOrBlank()) {
            val remoteTicket = remoteApi.addPurchase(ticket)
            tickets.add(remoteTicket)
            return
        }

        val presupuestoActivo = presupuestos.find { it.activo }
        val ticketVinculado = if (presupuestoActivo != null) {
            ticket.copy(presupuestoId = presupuestoActivo.id)
        } else ticket

        tickets.add(ticketVinculado)
        
        // Descontar del presupuesto correspondiente
        if (presupuestoActivo != null) {
            gastar(presupuestoActivo.id, ticketVinculado.total)
        }
        guardarTodo()
    }

    suspend fun eliminarTicket(id: String) {
        val ticket = tickets.find { it.id == id } ?: return
        
        // Devolver dinero al presupuesto al que estaba vinculado
        gastar(ticket.presupuestoId, -ticket.total)
        
        tickets.removeAll { it.id == id }
        guardarTodo()
    }

    suspend fun actualizarTicket(ticket: Ticket) {
        val index = tickets.indexOfFirst { it.id == ticket.id }
        if (index != -1) {
            val anterior = tickets[index]
            val diferencia = ticket.total - anterior.total
            
            // Impactar en el presupuesto vinculado al ticket
            gastar(ticket.presupuestoId, diferencia)
            
            tickets[index] = ticket
            guardarTodo()
        }
    }

    // ----------------------
    // USUARIOS
    // ----------------------

    suspend fun agregarUsuario(usuario: Usuario) = withContext(Dispatchers.IO) {
        val usuarioFinal = if (!AuthSessionStore.accessToken.isNullOrBlank()) {
            runCatching { remoteApi.addMember(usuario) }.getOrElse { usuario }
        } else {
            usuario
        }
        usuarios.removeAll { it.id == usuarioFinal.id || it.email.equals(usuarioFinal.email, ignoreCase = true) }
        usuarios.add(usuarioFinal)
        guardarTodo()
    }

    suspend fun cerrarSesion() {
        guardarTodo()
    }

    // ----------------------
    // THEME
    // ----------------------

    suspend fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
        guardarTodo()
    }

    private fun presupuestosBase(): List<Presupuesto> {
        return listOf(
            Presupuesto(
                id = "presupuesto-familiar",
                tipo = TipoPresupuesto.FAMILIAR,
                nombre = "Familiar",
                montoTotal = 0,
                montoDisponible = 0,
                fechaInicio = System.currentTimeMillis(),
                fechaFin = null,
                activo = true
            ),
            Presupuesto(
                id = "presupuesto-individual",
                tipo = TipoPresupuesto.INDIVIDUAL,
                nombre = "Individual",
                montoTotal = 0,
                montoDisponible = 0,
                fechaInicio = System.currentTimeMillis(),
                fechaFin = null,
                activo = false
            )
        )
    }

    private fun listasBase(): List<ListaCompra> {
        return listOf(
            ListaCompra(
                id = "lista-familiar",
                nombre = "Lista Familiar",
                presupuestoId = "presupuesto-familiar",
                esFamiliar = true,
                fechaCreacion = System.currentTimeMillis(),
                productos = mutableListOf()
            ),
            ListaCompra(
                id = "lista-individual",
                nombre = "Lista Individual",
                presupuestoId = "presupuesto-individual",
                esFamiliar = false,
                fechaCreacion = System.currentTimeMillis(),
                productos = mutableListOf()
            )
        )
    }

    private fun mergeUsuarios(locales: List<Usuario>, remotos: List<Usuario>): List<Usuario> {
        val merged = linkedMapOf<String, Usuario>()
        (remotos + locales).forEach { usuario ->
            val key = usuario.email.lowercase().ifBlank { usuario.id }
            if (key.isNotBlank()) {
                merged[key] = usuario
            }
        }
        return merged.values.toList()
    }

    private fun mergePresupuestos(locales: List<Presupuesto>, remotos: List<Presupuesto>): List<Presupuesto> {
        val base = if (remotos.isNotEmpty()) remotos else presupuestosBase()
        return base.map { remoto ->
            locales.firstOrNull { local ->
                local.id == remoto.id || local.tipo == remoto.tipo
            } ?: remoto
        }.ifEmpty { locales.ifEmpty { presupuestosBase() } }
    }

    companion object {
        private const val REMOTE_PRODUCTS_CLEANUP_DONE = "remote_products_cleanup_done"
    }
}
