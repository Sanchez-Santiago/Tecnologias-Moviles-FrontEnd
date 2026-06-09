package com.undef.superahorrosanchezpucci.data.repository

import android.app.Application
import android.util.Log
import com.undef.superahorrosanchezpucci.data.local.AppDatabase
import com.undef.superahorrosanchezpucci.data.local.CatalogoProductoEntity
import com.undef.superahorrosanchezpucci.data.local.TiendaEntity
import com.undef.superahorrosanchezpucci.data.local.toEntity as ticketToEntity
import com.undef.superahorrosanchezpucci.data.local.toModel as entityToModel
import com.undef.superahorrosanchezpucci.data.model.*
import com.undef.superahorrosanchezpucci.data.remote.AuthSessionStore
import com.undef.superahorrosanchezpucci.data.remote.RetrofitClient
import com.undef.superahorrosanchezpucci.data.remote.dto.*
import com.undef.superahorrosanchezpucci.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AppRepository(private val application: Application) {

    private val database = AppDatabase.get(application)
    private val appDao = database.appDao()
    private val apiService by lazy { RetrofitClient.getApiService() }

    var presupuestos = mutableListOf<Presupuesto>()
        private set

    var listas = mutableListOf<ListaCompra>()
        private set

    var tickets = mutableListOf<Ticket>()
        private set

    var usuarios = mutableListOf<Usuario>()
        private set

    var grupos = mutableListOf<GroupDetailResponse>()
        private set

    var invitaciones = mutableListOf<InvitationResponse>()
        private set

    var themeMode: ThemeMode = ThemeMode.SYSTEM
        private set

    var usuarioActual: Usuario? = null
        private set

    private var productNameToId: Map<String, String> = emptyMap()
    private var storeNameToId: Map<String, String> = emptyMap()

    // ======================
    // INIT
    // ======================

    suspend fun cargarTodo() = withContext(Dispatchers.IO) {
        if (AuthSessionStore.accessToken.isNullOrBlank()) {
            inicializarDatos()
            return@withContext
        }

        val loggedUser = appDao.getUsuarios().firstOrNull()
        usuarioActual = loggedUser?.toModel()

        fetchUserProfile()
        cargarProductos()
        cargarTiendas()
        cargarGrupos()
        cargarInvitaciones()

        val grupoId = grupos.firstOrNull()?.id
        if (grupoId != null) {
            loadBudgetsFromApi(grupoId)
        }
        if (presupuestos.isEmpty()) {
            loadDefaultLocalBudgets()
        }
        cargarCompras(grupoId)
        recalcularMontoDisponible()
        inicializarDatos()
    }

    private suspend fun cargarProductos() {
        val cached = appDao.getCatalogoProductos()
        if (cached.isNotEmpty()) {
            productNameToId = cached.associate { it.name.lowercase() to it.id }
            Log.d("API_PROD", "cargarProductos: loaded ${cached.size} products from Room cache")
        }

        try {
            val response = apiService.getProducts()
            if (response.isSuccessful && response.body()?.success == true) {
                val products = response.body()!!.data ?: emptyList()
                appDao.clearCatalogoProductos()
                appDao.insertCatalogoProductos(products.map { p ->
                    CatalogoProductoEntity(
                        id = p.id, name = p.name, price = p.price,
                        categoryId = p.categoryId, categoryName = p.categoryName,
                        description = p.description, imageUrl = p.imageUrl,
                        barcode = p.barcode, priority = p.priority, active = p.active
                    )
                })
                productNameToId = products.associate { it.name.lowercase() to it.id }
                Log.d("API_PROD", "cargarProductos: refreshed ${products.size} products from API")
            }
        } catch (e: Exception) {
            Log.w("API_PROD", "cargarProductos: API refresh failed, using cache", e)
        }
    }

    private suspend fun cargarTiendas() {
        val cached = appDao.getTiendas()
        if (cached.isNotEmpty()) {
            storeNameToId = cached.associate { it.name.lowercase() to it.id }
            Log.d("API_STORES", "cargarTiendas: loaded ${cached.size} stores from Room cache")
        }

        try {
            val response = apiService.getStores()
            if (response.isSuccessful && response.body()?.success == true) {
                val stores = response.body()!!.data ?: emptyList()
                appDao.clearTiendas()
                appDao.insertTiendas(stores.map { s ->
                    TiendaEntity(
                        id = s.id, name = s.name, address = s.address,
                        phone = s.phone, latitude = s.latitude,
                        longitude = s.longitude, active = s.active
                    )
                })
                storeNameToId = stores.associate { it.name.lowercase() to it.id }
                Log.d("API_STORES", "cargarTiendas: refreshed ${stores.size} stores from API")
            }
        } catch (e: Exception) {
            Log.w("API_STORES", "cargarTiendas: API refresh failed, using cache", e)
        }
    }

    private suspend fun cargarCompras(grupoId: String? = null) {
        if (grupoId != null) {
            try {
                val response = apiService.getPurchases(grupoId)
                if (response.isSuccessful && response.body()?.success == true) {
                    val apiPurchases = response.body()!!.data ?: emptyList()
                    Log.d("API_PURCH", "cargarCompras: loaded ${apiPurchases.size} purchases from API")
                    for (apiPurchase in apiPurchases) {
                        val sdf = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                        val millis = try {
                            LocalDateTime.parse(apiPurchase.purchaseDate, sdf)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toInstant().toEpochMilli()
                        } catch (_: Exception) { System.currentTimeMillis() }

                        val ticketEntity = com.undef.superahorrosanchezpucci.data.local.TicketEntity(
                            id = apiPurchase.id,
                            supermercado = apiPurchase.storeName ?: apiPurchase.notes ?: "",
                            direccion = "",
                            fechaHora = millis,
                            total = apiPurchase.total.toInt(),
                            metodoPago = "TARJETA_DEBITO",
                            imagenPath = "",
                            presupuestoId = apiPurchase.groupId,
                            storeId = apiPurchase.storeId,
                            notes = apiPurchase.notes,
                            userId = apiPurchase.userId,
                            synced = true
                        )
                        appDao.clearTicketProductos()
                        val existing = appDao.getTickets().find { it.id == apiPurchase.id }
                        if (existing == null) {
                            val productEntities = apiPurchase.items.mapIndexed { index, item ->
                                com.undef.superahorrosanchezpucci.data.local.TicketProductoEntity(
                                    ticketId = apiPurchase.id,
                                    posicion = index,
                                    nombre = item.productName,
                                    precio = item.unitPrice.toInt(),
                                    cantidad = item.quantity
                                )
                            }
                            appDao.insertTicketProductos(productEntities)
                        }
                    }
                } else {
                    Log.w("API_PURCH", "cargarCompras: API returned ${response.code()}")
                }
            } catch (e: Exception) {
                Log.w("API_PURCH", "cargarCompras: API fetch failed, falling back to local", e)
            }
        }

        val compras = appDao.getTickets()
        listas.clear()
        tickets.clear()
        for (compra in compras) {
            val productos = appDao.getTicketProductosByTicketId(compra.id).map { it.toModel() }
            val budgetId = if (presupuestos.any { it.id == compra.presupuestoId }) {
                compra.presupuestoId
            } else {
                presupuestos.firstOrNull()?.id ?: compra.presupuestoId
            }
            val ticket = Ticket(
                id = compra.id,
                supermercado = compra.supermercado,
                direccion = compra.direccion,
                fechaHora = compra.fechaHora,
                total = compra.total,
                metodoPago = try { MetodoPago.valueOf(compra.metodoPago) } catch (_: Exception) { MetodoPago.EFECTIVO },
                imagenPath = compra.imagenPath,
                presupuestoId = budgetId,
                productos = productos
            )

            if (compra.id.startsWith("lista-")) {
                val listBudgetId = when (compra.id) {
                    "lista-familiar" -> presupuestos.find { it.tipo == TipoPresupuesto.FAMILIAR }?.id
                    "lista-individual" -> presupuestos.find { it.tipo == TipoPresupuesto.INDIVIDUAL }?.id
                    else -> null
                }
                listas.add(
                    ListaCompra(
                        id = compra.id,
                        nombre = compra.supermercado.ifBlank { if (compra.id == "lista-familiar") "Lista Familiar" else "Lista Individual" },
                        presupuestoId = listBudgetId ?: compra.presupuestoId,
                        esFamiliar = compra.id == "lista-familiar",
                        fechaCreacion = compra.fechaHora,
                        hora = "",
                        supermercado = compra.supermercado,
                        total = compra.total,
                        productos = productos.toMutableList()
                    )
                )
            } else {
                tickets.add(ticket)
            }
        }
    }

    private suspend fun cargarGrupos() {
        try {
            val response = apiService.getMyGroups()
            if (response.isSuccessful && response.body()?.success == true) {
                val groups = response.body()!!.data ?: emptyList()
                grupos.clear()
                for (g in groups) {
                    val detailResponse = apiService.getGroupById(g.id)
                    if (detailResponse.isSuccessful && detailResponse.body()?.success == true) {
                        detailResponse.body()!!.data?.let { grupos.add(it) }
                    }
                }
                actualizarUsuariosDesdeGrupos()
            }
        } catch (e: Exception) {
            Log.e("API_GROUPS", "cargarGrupos failed", e)
        }
    }

    private suspend fun cargarInvitaciones() {
        try {
            val response = apiService.getMyInvitations()
            if (response.isSuccessful && response.body()?.success == true) {
                invitaciones = (response.body()!!.data ?: emptyList()).toMutableList()
            }
        } catch (e: Exception) {
            Log.e("API_INVITE", "cargarInvitaciones failed", e)
        }
    }

    private suspend fun actualizarUsuariosDesdeGrupos() {
        val allMembers = grupos.flatMap { it.members }
        usuarios = allMembers
            .filter { it.id != usuarioActual?.id }
            .map { it.toUsuario() }
            .toMutableList()
    }

    suspend fun guardarTodo() = withContext(Dispatchers.IO) {
        appDao.clearTicketProductos()
        appDao.clearProductos()
        appDao.clearTickets()
        appDao.clearListas()
        appDao.clearPresupuestos()
        appDao.clearUsuarios()

        appDao.insertPresupuestos(presupuestos.map { it.toEntity() })
        appDao.insertListas(listas.map { it.toEntity() })
        appDao.insertTickets(tickets.map { it.ticketToEntity() })
        tickets.forEach { ticket ->
            ticket.productos.forEachIndexed { index, tp ->
                appDao.insertTicketProductos(listOf(tp.toEntity(ticket.id, index)))
            }
        }
        appDao.insertUsuarios(usuarios.map { it.toEntity() }.toList())
        appDao.insertConfig(themeMode.toEntity())
    }

    private fun inicializarDatos() {
        val presupuestoFamiliar = Presupuesto(
            "presupuesto-familiar", TipoPresupuesto.FAMILIAR, "Familiar",
            0, 0, System.currentTimeMillis(), null, true
        )
        val presupuestoIndividual = Presupuesto(
            "presupuesto-individual", TipoPresupuesto.INDIVIDUAL, "Individual",
            0, 0, System.currentTimeMillis(), null, false
        )

        if (presupuestos.isEmpty()) {
            presupuestos.add(presupuestoFamiliar)
            presupuestos.add(presupuestoIndividual)
        }

        if (listas.isEmpty()) {
            listas.add(
                ListaCompra(
                    id = "lista-familiar", nombre = "Lista Familiar",
                    presupuestoId = presupuestoFamiliar.id, esFamiliar = true,
                    fechaCreacion = System.currentTimeMillis(), hora = "",
                    supermercado = "", total = 0, productos = mutableListOf()
                )
            )
            listas.add(
                ListaCompra(
                    id = "lista-individual", nombre = "Lista Individual",
                    presupuestoId = presupuestoIndividual.id, esFamiliar = false,
                    fechaCreacion = System.currentTimeMillis(), hora = "",
                    supermercado = "", total = 0, productos = mutableListOf()
                )
            )
        }
    }

    // ======================
    // BUDGETS API
    // ======================

    private suspend fun loadBudgetsFromApi(groupId: String) {
        try {
            val response = apiService.getBudgets(groupId)
            if (response.isSuccessful && response.body()?.success == true) {
                val budgets = response.body()!!.data ?: emptyList()
                if (budgets.isEmpty()) return

                presupuestos.clear()
                for ((index, b) in budgets.withIndex()) {
                    val spent = tickets.filter { it.presupuestoId == b.id }.sumOf { it.total }
                    val presupuesto = Presupuesto(
                        id = b.id,
                        tipo = if (index == 0) TipoPresupuesto.FAMILIAR else TipoPresupuesto.INDIVIDUAL,
                        nombre = b.name,
                        montoTotal = b.totalAmount.toInt(),
                        montoDisponible = maxOf(0, b.totalAmount.toInt() - spent),
                        fechaInicio = try {
                            LocalDateTime.parse(b.startDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                        } catch (_: Exception) { System.currentTimeMillis() },
                        fechaFin = b.endDate?.let {
                            try {
                                LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                    .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                            } catch (_: Exception) { null }
                        },
                        activo = index == 0
                    )
                    presupuestos.add(presupuesto)
                }
                Log.d("API_BUDGET", "loadBudgetsFromApi: ${presupuestos.size} budgets loaded")
            }
        } catch (e: Exception) {
            Log.e("API_BUDGET", "loadBudgetsFromApi failed", e)
        }
    }

    private suspend fun loadDefaultLocalBudgets() {
        val presupuestosEntities = appDao.getPresupuestos()
        presupuestos = presupuestosEntities.map { it.toModel() }.toMutableList()
        if (presupuestos.isEmpty()) {
            appDao.insertPresupuestos(
                listOf(
                    PresupuestoEntity(
                        id = "presupuesto-familiar", tipo = "FAMILIAR", nombre = "Familiar",
                        montoTotal = 0, montoDisponible = 0, fechaInicio = System.currentTimeMillis(),
                        fechaFin = null, activo = true
                    ),
                    PresupuestoEntity(
                        id = "presupuesto-individual", tipo = "INDIVIDUAL", nombre = "Individual",
                        montoTotal = 0, montoDisponible = 0, fechaInicio = System.currentTimeMillis(),
                        fechaFin = null, activo = false
                    )
                )
            )
            presupuestos = appDao.getPresupuestos().map { it.toModel() }.toMutableList()
        }
    }

    private suspend fun recalcularMontoDisponible() {
        val updated = presupuestos.map { p ->
            val spent = tickets.filter { it.presupuestoId == p.id }.sumOf { it.total }
            p.copy(montoDisponible = maxOf(0, p.montoTotal - spent))
        }
        presupuestos.clear()
        presupuestos.addAll(updated)
    }

    private suspend fun syncBudgetToApi(presupuesto: Presupuesto) {
        try {
            val request = UpdateBudgetRequest(
                name = presupuesto.nombre,
                totalAmount = presupuesto.montoTotal.toDouble(),
                period = "MONTHLY",
                startDate = LocalDateTime.now().toString(),
                items = emptyList()
            )
            val response = apiService.updateBudget(presupuesto.id, request)
            if (response.isSuccessful) {
                Log.d("API_BUDGET", "syncBudgetToApi success: ${presupuesto.id}")
            } else {
                Log.w("API_BUDGET", "syncBudgetToApi failed: code=${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("API_BUDGET", "syncBudgetToApi exception", e)
        }
    }

    // ======================
    // AUTH
    // ======================

    suspend fun login(email: String, password: String): Result<Usuario> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.login(LoginRequest(email, password))
            if (response.isSuccessful && response.body()?.success == true) {
                val authData = response.body()!!.data!!
                AuthSessionStore.save(
                    application.applicationContext,
                    authData.accessToken,
                    authData.refreshToken
                )

                val usuario = Usuario(
                    id = authData.user.id,
                    nombre = authData.user.fullName,
                    email = authData.user.email,
                    rol = if (authData.user.role == "ADMIN") RolUsuario.ADMIN else RolUsuario.MIEMBRO,
                    activo = true
                )

                appDao.clearUsuarios()
                appDao.insertUsuarios(listOf(usuario.toEntity()))
                usuarioActual = usuario
                Result.success(usuario)
            } else {
                val msg = response.body()?.message ?: "Error de autenticación"
                Log.w("API_AUTH", "login failed: code=${response.code()} msg=$msg")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Log.e("API_AUTH", "login exception", e)
            Result.failure(e)
        }
    }

    suspend fun register(fullName: String, email: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.register(RegisterRequest(email, password, fullName))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                val msg = response.body()?.message ?: "Error de registro"
                Log.w("API_AUTH", "register failed: code=${response.code()} msg=$msg")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Log.e("API_AUTH", "register exception", e)
            Result.failure(e)
        }
    }

    suspend fun logout() {
        withContext(Dispatchers.IO) {
            AuthSessionStore.clear(application.applicationContext)
            RetrofitClient.reset()
            usuarioActual = null
            grupos.clear()
            invitaciones.clear()
            usuarios.clear()
            presupuestos.clear()
            listas.clear()
            tickets.clear()
        }
    }

    fun isLoggedIn(): Boolean = AuthSessionStore.accessToken != null

    // ======================
    // PRESUPUESTO
    // ======================

    suspend fun cambiarPresupuestoActivo(id: String) {
        withContext(Dispatchers.IO) {
            val updated = presupuestos.map { it.copy(activo = it.id == id) }
            presupuestos.clear()
            presupuestos.addAll(updated)
        }
    }

    fun gastar(presupuestoId: String, monto: Int) {
        val index = presupuestos.indexOfFirst { it.id == presupuestoId }
        if (index != -1) {
            val actual = presupuestos[index]
            presupuestos[index] = actual.copy(montoDisponible = actual.montoDisponible - monto)
        }
    }

    suspend fun actualizarPresupuesto(id: String, nuevoMonto: Int) {
        withContext(Dispatchers.IO) {
            val index = presupuestos.indexOfFirst { it.id == id }
            if (index != -1) {
                val actual = presupuestos[index]
                val gastado = actual.montoTotal - actual.montoDisponible
                val updated = actual.copy(
                    montoTotal = nuevoMonto,
                    montoDisponible = nuevoMonto - gastado
                )
                presupuestos[index] = updated
                syncBudgetToApi(updated)
            }
        }
    }

    // ======================
    // LISTAS (Shopping Lists)
    // ======================

    suspend fun agregarLista(lista: ListaCompra) {
        withContext(Dispatchers.IO) {
            listas.add(lista)
        }
    }

    suspend fun agregarOActualizarProducto(listaId: String, producto: Producto) {
        withContext(Dispatchers.IO) {
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

                val total = nuevosProductos.sumOf { it.precioEstimado * it.cantidad }
                listas[indexLista] = listas[indexLista].copy(total = total)
            }
        }
    }

    suspend fun eliminarProducto(listaId: String, productoId: String) {
        withContext(Dispatchers.IO) {
            val indexLista = listas.indexOfFirst { it.id == listaId }
            if (indexLista != -1) {
                val lista = listas[indexLista]
                val nuevosProductos = lista.productos.toMutableList()
                nuevosProductos.removeAll { it.id == productoId }
                listas[indexLista] = lista.copy(productos = nuevosProductos)
            }
        }
    }

    suspend fun toggleProducto(listaId: String, productoId: String) {
        withContext(Dispatchers.IO) {
            val indexLista = listas.indexOfFirst { it.id == listaId }
            if (indexLista == -1) return@withContext

            val lista = listas[indexLista]
            val nuevosProductos = lista.productos.toMutableList()
            val indexProd = nuevosProductos.indexOfFirst { it.id == productoId }
            if (indexProd == -1) return@withContext

            val producto = nuevosProductos[indexProd]
            val actualizado = producto.copy(comprado = !producto.comprado)
            nuevosProductos[indexProd] = actualizado
            listas[indexLista] = lista.copy(productos = nuevosProductos)

            val presupuestoId = lista.presupuestoId
            if (actualizado.comprado) {
                gastar(presupuestoId, actualizado.precioEstimado * actualizado.cantidad)
            } else {
                gastar(presupuestoId, -(actualizado.precioEstimado * actualizado.cantidad))
            }
        }
    }

    // ======================
    // TICKETS (Completed Purchases)
    // ======================

    suspend fun agregarTicket(ticket: Ticket) {
        withContext(Dispatchers.IO) {
            val presupuestoActivo = presupuestos.find { it.activo }
            val ticketVinculado = if (presupuestoActivo != null) {
                ticket.copy(presupuestoId = presupuestoActivo.id)
            } else ticket

            tickets.add(0, ticketVinculado)
            recalcularMontoDisponible()

            var synced = false
            var apiId: String? = null
            try {
                val grupoId = grupos.firstOrNull()?.id
                if (grupoId != null) {
                    val missingProducts = mutableListOf<String>()
                    val items = ticketVinculado.productos.mapNotNull { tp ->
                        val productId = productNameToId[tp.nombre.lowercase()]
                        if (productId == null) {
                            missingProducts.add(tp.nombre)
                            null
                        } else {
                            CreatePurchaseItem(productId = productId, quantity = tp.cantidad)
                        }
                    }
                    if (missingProducts.isNotEmpty()) {
                        Log.w("API_PURCH", "agregarTicket: products not in catalog: $missingProducts")
                    }
                    if (items.isNotEmpty()) {
                        val storeId = storeNameToId[ticketVinculado.supermercado.lowercase()]
                        val request = CreatePurchaseRequest(
                            groupId = grupoId,
                            storeId = storeId,
                            notes = if (storeId != null) null else ticketVinculado.supermercado,
                            items = items
                        )
                        val response = apiService.createPurchase(request)
                        if (response.isSuccessful) {
                            val created = response.body()?.data
                            if (created != null) {
                                apiId = created.id
                                synced = true
                            }
                        }
                    } else {
                        Log.w("API_PURCH", "agregarTicket: no mappable products, skipping API sync")
                    }
                }
            } catch (e: Exception) {
                Log.w("API_PURCH", "agregarTicket sync failed, will retry later", e)
            }

            val ticketEntity = com.undef.superahorrosanchezpucci.data.local.TicketEntity(
                id = apiId ?: ticketVinculado.id,
                supermercado = ticketVinculado.supermercado,
                direccion = ticketVinculado.direccion,
                fechaHora = ticketVinculado.fechaHora,
                total = ticketVinculado.total,
                metodoPago = ticketVinculado.metodoPago.name,
                imagenPath = ticketVinculado.imagenPath,
                presupuestoId = ticketVinculado.presupuestoId,
                synced = synced
            )
            appDao.clearTicketProductos()
            val productEntities = ticketVinculado.productos.mapIndexed { index, tp ->
                com.undef.superahorrosanchezpucci.data.local.TicketProductoEntity(
                    ticketId = ticketEntity.id,
                    posicion = index,
                    nombre = tp.nombre,
                    precio = tp.precio,
                    cantidad = tp.cantidad
                )
            }
            appDao.insertTicketProductos(productEntities)
        }
    }

    suspend fun eliminarTicket(id: String) {
        withContext(Dispatchers.IO) {
            val ticket = tickets.find { it.id == id } ?: return@withContext
            tickets.removeAll { it.id == id }

            try {
                val entity = appDao.getTickets().find { it.id == id }
                if (entity != null && entity.synced) {
                    apiService.deletePurchase(id)
                    Log.d("API_PURCH", "eliminarTicket: deleted $id from API")
                }
            } catch (e: Exception) {
                Log.w("API_PURCH", "eliminarTicket: API delete failed for $id", e)
            }

            recalcularMontoDisponible()
        }
    }

    suspend fun actualizarTicket(ticket: Ticket) {
        withContext(Dispatchers.IO) {
            val index = tickets.indexOfFirst { it.id == ticket.id }
            if (index != -1) {
                tickets[index] = ticket

                try {
                    val entity = appDao.getTickets().find { it.id == ticket.id }
                    if (entity != null && entity.synced) {
                        val request = UpdatePurchaseRequest(
                            storeId = entity.storeId,
                            notes = ticket.supermercado.ifBlank { null }
                        )
                        apiService.updatePurchase(ticket.id, request)
                        Log.d("API_PURCH", "actualizarTicket: updated ${ticket.id} in API")
                    }
                } catch (e: Exception) {
                    Log.w("API_PURCH", "actualizarTicket: API update failed for ${ticket.id}", e)
                }

                recalcularMontoDisponible()
            }
        }
    }

    // ======================
    // USUARIOS
    // ======================

    suspend fun agregarUsuario(usuario: Usuario) {
        withContext(Dispatchers.IO) {
            usuarios.add(usuario)
            appDao.insertUsuarios(listOf(usuario.toEntity()))
        }
    }

    suspend fun actualizarUsuario(usuario: Usuario) {
        withContext(Dispatchers.IO) {
            if (usuario.id == usuarioActual?.id) {
                usuarioActual = usuario
                appDao.clearUsuarios()
                appDao.insertUsuarios(listOf(usuario.toEntity().copy(rol = usuario.rol.name)))

                try {
                    val response = apiService.updateProfile(mapOf("fullName" to usuario.nombre))
                    if (response.isSuccessful && response.body()?.success == true) {
                        Log.d("API_USER", "Profile updated in API")
                    }
                } catch (e: Exception) {
                    Log.e("API_USER", "Failed to update profile in API", e)
                }
            } else {
                val index = usuarios.indexOfFirst { it.id == usuario.id }
                if (index != -1) {
                    usuarios[index] = usuario
                }
            }
        }
    }

    private suspend fun fetchUserProfile() {
        try {
            val response = apiService.getProfile()
            if (response.isSuccessful && response.body()?.success == true) {
                val profile = response.body()!!.data!!
                val usuario = Usuario(
                    id = profile.id,
                    nombre = profile.fullName,
                    email = profile.email,
                    rol = if (profile.role == "ADMIN") RolUsuario.ADMIN else RolUsuario.MIEMBRO,
                    activo = true
                )
                usuarioActual = usuario
                appDao.clearUsuarios()
                appDao.insertUsuarios(listOf(usuario.toEntity()))
                Log.d("API_USER", "Profile refreshed from API: ${usuario.nombre}")
            }
        } catch (e: Exception) {
            Log.w("API_USER", "Failed to fetch profile from API", e)
        }
    }

    // ======================
    // GROUPS & INVITATIONS API
    // ======================

    suspend fun inviteMember(groupId: String, email: String): Result<InvitationResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.inviteMember(groupId, InviteRequest(email))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                val msg = response.body()?.message ?: "Error al invitar"
                Log.w("API_INVITE", "inviteMember failed: code=${response.code()} msg=$msg")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Log.e("API_INVITE", "inviteMember exception", e)
            Result.failure(e)
        }
    }

    suspend fun acceptInvitation(token: String): Result<InvitationResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.acceptInvitation(token)
            if (response.isSuccessful && response.body()?.success == true) {
                invitaciones.removeAll { it.token == token }
                Result.success(response.body()!!.data!!)
            } else {
                val msg = response.body()?.message ?: "Error al aceptar"
                Log.w("API_INVITE", "acceptInvitation failed: code=${response.code()} msg=$msg")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Log.e("API_INVITE", "acceptInvitation exception", e)
            Result.failure(e)
        }
    }

    suspend fun rejectInvitation(token: String): Result<InvitationResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.rejectInvitation(token)
            if (response.isSuccessful && response.body()?.success == true) {
                invitaciones.removeAll { it.token == token }
                Result.success(response.body()!!.data!!)
            } else {
                val msg = response.body()?.message ?: "Error al rechazar"
                Log.w("API_INVITE", "rejectInvitation failed: code=${response.code()} msg=$msg")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Log.e("API_INVITE", "rejectInvitation exception", e)
            Result.failure(e)
        }
    }

    suspend fun createGroup(name: String, description: String?): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.createGroup(CreateGroupRequest(name, description))
            Log.d("API_GROUPS", "createGroup response: code=${response.code()} body=${response.body()}")
            if (response.isSuccessful && response.body()?.success == true) {
                val groupId = response.body()!!.data!!.id
                cargarGrupos()
                Result.success(groupId)
            } else {
                val msg = response.body()?.message ?: "Error al crear grupo"
                Log.w("API_GROUPS", "createGroup failed: code=${response.code()} msg=$msg")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Log.e("API_GROUPS", "createGroup exception", e)
            Result.failure(e)
        }
    }

    suspend fun refreshGroups() {
        withContext(Dispatchers.IO) { cargarGrupos() }
    }

    suspend fun refreshInvitations() {
        withContext(Dispatchers.IO) { cargarInvitaciones() }
    }

    // ======================
    // THEME
    // ======================

    suspend fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
    }

    // ======================
    // API - Stores
    // ======================

    suspend fun getStoresFromApi(): Result<List<StoreResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getStores()
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data ?: emptyList())
            } else {
                val msg = response.body()?.message ?: "Error al cargar supermercados"
                Log.w("API_STORES", "getStores failed: code=${response.code()} msg=$msg")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Log.e("API_STORES", "getStores exception", e)
            Result.failure(e)
        }
    }

    // ======================
    // API - Products
    // ======================

    suspend fun getProductsFromApi(categoryId: String? = null): Result<List<ProductResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getProducts(categoryId)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data ?: emptyList())
            } else {
                val msg = response.body()?.message ?: "Error al cargar productos"
                Log.w("API_PROD", "getProducts failed: code=${response.code()} msg=$msg")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Log.e("API_PROD", "getProducts exception", e)
            Result.failure(e)
        }
    }

    // ======================
    // API - Statistics
    // ======================

    suspend fun getSpendingByCategory(groupId: String): Result<List<SpendingByCategory>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getSpendingByCategory(groupId)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data ?: emptyList())
            } else {
                val msg = response.body()?.message ?: "Error al cargar estadísticas"
                Log.w("API_STATS", "spendingByCategory failed: code=${response.code()} msg=$msg")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Log.e("API_STATS", "spendingByCategory exception", e)
            Result.failure(e)
        }
    }

    suspend fun getSpendingByStore(groupId: String): Result<List<SpendingByStore>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getSpendingByStore(groupId)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data ?: emptyList())
            } else {
                val msg = response.body()?.message ?: "Error al cargar estadísticas"
                Log.w("API_STATS", "spendingByStore failed: code=${response.code()} msg=$msg")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Log.e("API_STATS", "spendingByStore exception", e)
            Result.failure(e)
        }
    }

    suspend fun getMonthlySummary(groupId: String): Result<List<MonthlySummary>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMonthlySummary(groupId)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data ?: emptyList())
            } else {
                val msg = response.body()?.message ?: "Error al cargar estadísticas"
                Log.w("API_STATS", "monthlySummary failed: code=${response.code()} msg=$msg")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Log.e("API_STATS", "monthlySummary exception", e)
            Result.failure(e)
        }
    }

    // ======================
    // API - Sync purchases
    // ======================

    suspend fun syncUnsyncedPurchases() {
        withContext(Dispatchers.IO) {
            try {
                val unsynced = appDao.getUnsyncedTickets()
                for (ticketEntity in unsynced) {
                    val productos = appDao.getTicketProductosByTicketId(ticketEntity.id)
                    val missingProducts = mutableListOf<String>()
                    val items = productos.mapNotNull { prod ->
                        val productId = productNameToId[prod.nombre.lowercase()]
                        if (productId == null) {
                            missingProducts.add(prod.nombre)
                            null
                        } else {
                            CreatePurchaseItem(productId = productId, quantity = prod.cantidad)
                        }
                    }
                    if (missingProducts.isNotEmpty()) {
                        Log.w("API_PURCH", "syncUnsyncedPurchases: ${ticketEntity.id} not in catalog: $missingProducts")
                    }
                    if (items.isNotEmpty()) {
                        val grupoId = grupos.firstOrNull()?.id ?: continue
                        val request = CreatePurchaseRequest(
                            groupId = grupoId,
                            storeId = storeNameToId[ticketEntity.supermercado.lowercase()],
                            notes = ticketEntity.notes,
                            items = items
                        )
                        val response = apiService.createPurchase(request)
                        if (response.isSuccessful) {
                            appDao.markTicketSynced(ticketEntity.id)
                            Log.d("API_PURCH", "syncUnsyncedPurchases: synced ${ticketEntity.id}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("API_PURCH", "syncUnsyncedPurchases failed", e)
            }
        }
    }
}

// ======================
// MAPPING EXTENSIONS
// ======================

private fun com.undef.superahorrosanchezpucci.data.local.PresupuestoEntity.toModel() = Presupuesto(
    id = id, tipo = if (tipo == "FAMILIAR") TipoPresupuesto.FAMILIAR else TipoPresupuesto.INDIVIDUAL,
    nombre = nombre, montoTotal = montoTotal, montoDisponible = montoDisponible,
    fechaInicio = fechaInicio, fechaFin = fechaFin, activo = activo
)

private fun Presupuesto.toEntity() = com.undef.superahorrosanchezpucci.data.local.PresupuestoEntity(
    id = id, tipo = if (tipo == TipoPresupuesto.FAMILIAR) "FAMILIAR" else "INDIVIDUAL",
    nombre = nombre, montoTotal = montoTotal, montoDisponible = montoDisponible,
    fechaInicio = fechaInicio, fechaFin = fechaFin, activo = activo
)

private fun ListaCompra.toEntity() = com.undef.superahorrosanchezpucci.data.local.ListaCompraEntity(
    id = id, nombre = nombre, presupuestoId = presupuestoId, esFamiliar = esFamiliar,
    fechaCreacion = fechaCreacion, hora = hora, supermercado = supermercado, total = total
)

private fun com.undef.superahorrosanchezpucci.data.local.TicketEntity.toModel(productos: List<TicketProducto>) = Ticket(
    id = id, supermercado = supermercado, direccion = direccion, fechaHora = fechaHora,
    total = total, metodoPago = try { MetodoPago.valueOf(metodoPago) } catch (_: Exception) { MetodoPago.EFECTIVO },
    imagenPath = imagenPath, presupuestoId = presupuestoId, productos = productos
)

private fun com.undef.superahorrosanchezpucci.data.local.TicketProductoEntity.toModel() = TicketProducto(
    nombre = nombre, precio = precio, cantidad = cantidad
)

private fun Ticket.toEntity() = com.undef.superahorrosanchezpucci.data.local.TicketEntity(
    id = id, supermercado = supermercado, direccion = direccion, fechaHora = fechaHora,
    total = total, metodoPago = metodoPago.name, imagenPath = imagenPath,
    presupuestoId = presupuestoId
)

private fun TicketProducto.toEntity(ticketId: String, posicion: Int) = com.undef.superahorrosanchezpucci.data.local.TicketProductoEntity(
    ticketId = ticketId, posicion = posicion, nombre = nombre, precio = precio, cantidad = cantidad
)

private fun Usuario.toEntity() = com.undef.superahorrosanchezpucci.data.local.UsuarioEntity(
    id = id, nombre = nombre, email = email, rol = rol.name, activo = activo
)

private fun com.undef.superahorrosanchezpucci.data.local.UsuarioEntity.toModel() = Usuario(
    id = id, nombre = nombre, email = email,
    rol = try { RolUsuario.valueOf(rol) } catch (_: Exception) { RolUsuario.MIEMBRO },
    activo = activo
)

private fun ThemeMode.toEntity() = com.undef.superahorrosanchezpucci.data.local.AppConfigEntity(themeMode = name)

private fun GroupMemberResponse.toUsuario() = Usuario(
    id = id, nombre = fullName, email = email,
    rol = if (role == "ADMIN") RolUsuario.ADMIN else RolUsuario.MIEMBRO,
    activo = true
)
