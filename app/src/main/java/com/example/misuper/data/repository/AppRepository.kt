package com.example.misuper.data.repository

import com.example.misuper.data.local.JsonStorage
import com.example.misuper.data.model.*
import com.example.misuper.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(private val storage: JsonStorage) {

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
        val data = storage.cargar()

        if (data != null) {
            presupuestos = data.presupuestos.toMutableList()
            listas = data.listas.toMutableList()
            tickets = data.tickets.toMutableList()
            usuarios = data.usuarios.toMutableList()
            themeMode = data.themeMode
        } else {
            inicializarDatos()
        }
    }

    private suspend fun inicializarDatos() {
        val familiarId = "presupuesto-familiar"
        val individualId = "presupuesto-individual"

        val familiar = Presupuesto(
            id = familiarId,
            tipo = TipoPresupuesto.FAMILIAR,
            nombre = "Familiar",
            montoTotal = 0,
            montoDisponible = 0,
            fechaInicio = System.currentTimeMillis(),
            fechaFin = null,
            activo = true
        )

        val individual = Presupuesto(
            id = individualId,
            tipo = TipoPresupuesto.INDIVIDUAL,
            nombre = "Individual",
            montoTotal = 0,
            montoDisponible = 0,
            fechaInicio = System.currentTimeMillis(),
            fechaFin = null,
            activo = false
        )

        presupuestos.add(familiar)
        presupuestos.add(individual)

        // Inicializar listas base
        listas.add(
            ListaCompra(
                id = "lista-familiar",
                nombre = "Lista Familiar",
                presupuestoId = familiarId,
                esFamiliar = true,
                fechaCreacion = System.currentTimeMillis(),
                productos = mutableListOf()
            )
        )
        listas.add(
            ListaCompra(
                id = "lista-individual",
                nombre = "Lista Individual",
                presupuestoId = individualId,
                esFamiliar = false,
                fechaCreacion = System.currentTimeMillis(),
                productos = mutableListOf()
            )
        )

        guardarTodo()
    }

    // ----------------------
    // GUARDADO
    // ----------------------

    suspend fun guardarTodo() = withContext(Dispatchers.IO) {
        val data = AppData(
            presupuestos = presupuestos.toList(),
            listas = listas.toList(),
            tickets = tickets.toList(),
            usuarios = usuarios.toList(),
            themeMode = themeMode
        )

        storage.guardar(data)
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
        if (index != -1) {
            val actual = presupuestos[index]
            val gastado = actual.montoTotal - actual.montoDisponible
            presupuestos[index] = actual.copy(
                montoTotal = nuevoMonto,
                montoDisponible = nuevoMonto - gastado
            )
        }
        guardarTodo()
    }

    // ----------------------
    // LISTAS
    // ----------------------

    suspend fun agregarLista(lista: ListaCompra) {
        listas.add(lista)
        guardarTodo()
    }

    suspend fun agregarOActualizarProducto(listaId: String, producto: Producto) {
        val indexLista = listas.indexOfFirst { it.id == listaId }
        if (indexLista != -1) {
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

    suspend fun agregarUsuario(usuario: Usuario) {
        usuarios.add(usuario)
        guardarTodo()
    }

    // ----------------------
    // THEME
    // ----------------------

    suspend fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
        guardarTodo()
    }
}