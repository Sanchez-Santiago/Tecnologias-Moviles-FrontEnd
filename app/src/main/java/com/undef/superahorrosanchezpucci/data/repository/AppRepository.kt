package com.undef.superahorrosanchezpucci.data.repository

import android.app.Application
import android.util.Log
import com.undef.superahorrosanchezpucci.data.local.AppDatabase
import com.undef.superahorrosanchezpucci.data.local.CatalogoProductoEntity
import com.undef.superahorrosanchezpucci.data.local.PresupuestoEntity
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

        try {
            fetchUserProfile()
            cargarProductos()
            cargarTiendas()
            
            // Aseguramos que el usuario tenga un grupo y presupuesto en el backend
            ensureGroupAndBudget()
            
            cargarInvitaciones()

            val grupoId = grupos.firstOrNull()?.id
            if (grupoId != null) {
                loadBudgetsFromApi(grupoId)
                cargarCompras(grupoId)
            } else {
                loadDefaultLocalBudgets()
            }
            
            recalcularMontoDisponible()
            inicializarListas()
        } catch (e: Exception) {
            Log.e("AppRepository", "Error en cargarTodo", e)
            inicializarDatos() // Fallback a locales si todo falla
        }
    }

    private suspend fun ensureGroupAndBudget() {
        // 1. Cargar grupos existentes
        cargarGrupos()
        
        // 2. Si no hay grupos, crear uno por defecto
        if (grupos.isEmpty()) {
            Log.d("AppRepository", "No se encontraron grupos, creando uno...")
            val createGroupResp = apiService.createGroup(CreateGroupRequest("Mi Familia", "Grupo familiar de Super Ahorro"))
            if (createGroupResp.isSuccessful && createGroupResp.body()?.success == true) {
                cargarGrupos() // Recargar para tener el detalle
            }
        }
        
        val grupoId = grupos.firstOrNull()?.id ?: return
        
        // 3. Asegurar que el grupo tenga al menos un presupuesto
        val budgetsResp = apiService.getBudgets(grupoId)
        if (budgetsResp.isSuccessful && (budgetsResp.body()?.data == null || budgetsResp.body()?.data!!.isEmpty())) {
            Log.d("AppRepository", "No se encontraron presupuestos en el grupo, creando uno...")
            
            // Necesitamos una categoría para el presupuesto inicial
            val categoriesResp = apiService.getCategories()
            val categoryId = categoriesResp.body()?.data?.firstOrNull()?.id ?: ""
            
            val createBudgetReq = CreateBudgetRequest(
                groupId = grupoId,
                name = "Presupuesto Mensual",
                totalAmount = 0.0,
                period = "MONTHLY",
                startDate = LocalDateTime.now().toString(),
                items = if (categoryId.isNotEmpty()) listOf(CreateBudgetItem(categoryId, 0.0)) else emptyList()
            )
            apiService.createBudget(createBudgetReq)
        }
    }

    private suspend fun cargarProductos() {
        val cached = appDao.getCatalogoProductos()
        if (cached.isNotEmpty()) {
            productNameToId = cached.associate { it.name.lowercase() to it.id }
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
            }
        } catch (e: Exception) {
            Log.w("API_PROD", "cargarProductos failed", e)
        }
    }

    private suspend fun cargarTiendas() {
        val cached = appDao.getTiendas()
        if (cached.isNotEmpty()) {
            storeNameToId = cached.associate { it.name.lowercase() to it.id }
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
            }
        } catch (e: Exception) {
            Log.w("API_STORES", "cargarTiendas failed", e)
        }
    }

    private suspend fun cargarCompras(grupoId: String) {
        try {
            val response = apiService.getPurchases(grupoId)
            if (response.isSuccessful && response.body()?.success == true) {
                val apiPurchases = response.body()!!.data ?: emptyList()
                tickets.clear()
                
                for (apiPurchase in apiPurchases) {
                    val sdf = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                    val millis = try {
                        LocalDateTime.parse(apiPurchase.purchaseDate, sdf)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toInstant().toEpochMilli()
                    } catch (_: Exception) { System.currentTimeMillis() }

                    val ticket = Ticket(
                        id = apiPurchase.id,
                        supermercado = apiPurchase.storeName ?: apiPurchase.notes ?: "Supermercado",
                        direccion = "",
                        fechaHora = millis,
                        total = apiPurchase.total.toInt(),
                        metodoPago = MetodoPago.EFECTIVO,
                        imagenPath = "",
                        presupuestoId = apiPurchase.groupId,
                        productos = apiPurchase.items.map { item ->
                            TicketProducto(item.productName, item.unitPrice.toInt(), item.quantity)
                        }
                    )
                    tickets.add(ticket)
                }
            }
        } catch (e: Exception) {
            Log.e("API_PURCH", "cargarCompras failed", e)
        }
    }

    private suspend fun cargarGrupos() {
        try {
            val response = apiService.getMyGroups()
            if (response.isSuccessful && response.body()?.success == true) {
                val groupsList = response.body()!!.data ?: emptyList()
                grupos.clear()
                for (g in groupsList) {
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
            .distinctBy { it.id }
            .toMutableList()
    }

    suspend fun guardarTodo() = withContext(Dispatchers.IO) {
        // En este punto, preferimos confiar en la API si estamos logueados
        if (isLoggedIn()) return@withContext 

        appDao.clearTicketProductos()
        appDao.clearTickets()
        appDao.clearPresupuestos()
        appDao.clearUsuarios()

        appDao.insertPresupuestos(presupuestos.map { it.toEntity() })
        appDao.insertTickets(tickets.map { it.ticketToEntity() })
        tickets.forEach { ticket ->
            ticket.productos.forEachIndexed { index, tp ->
                appDao.insertTicketProductos(listOf(tp.toEntity(ticket.id, index)))
            }
        }
        appDao.insertUsuarios(usuarios.map { it.toEntity() }.toList())
    }

    private fun inicializarDatos() {
        if (presupuestos.isEmpty()) {
            presupuestos.add(Presupuesto("presupuesto-familiar", TipoPresupuesto.FAMILIAR, "Familiar", 0, 0, System.currentTimeMillis(), null, true))
            presupuestos.add(Presupuesto("presupuesto-individual", TipoPresupuesto.INDIVIDUAL, "Individual", 0, 0, System.currentTimeMillis(), null, false))
        }
        inicializarListas()
    }

    private fun inicializarListas() {
        val famId = presupuestos.find { it.tipo == TipoPresupuesto.FAMILIAR }?.id ?: "presupuesto-familiar"
        val indId = presupuestos.find { it.tipo == TipoPresupuesto.INDIVIDUAL }?.id ?: "presupuesto-individual"
        
        if (listas.isEmpty()) {
            listas.add(ListaCompra("lista-familiar", "Lista Familiar", famId, true, System.currentTimeMillis(), "", "", 0, mutableListOf()))
            listas.add(ListaCompra("lista-individual", "Lista Individual", indId, false, System.currentTimeMillis(), "", "", 0, mutableListOf()))
        } else {
            // Actualizar IDs de presupuesto en las listas si cambiaron (por login)
            val indexFam = listas.indexOfFirst { it.id == "lista-familiar" }
            if (indexFam != -1) listas[indexFam] = listas[indexFam].copy(presupuestoId = famId)
            
            val indexInd = listas.indexOfFirst { it.id == "lista-individual" }
            if (indexInd != -1) listas[indexInd] = listas[indexInd].copy(presupuestoId = indId)
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
                    val presupuesto = Presupuesto(
                        id = b.id,
                        tipo = if (index == 0) TipoPresupuesto.FAMILIAR else TipoPresupuesto.INDIVIDUAL,
                        nombre = b.name,
                        montoTotal = b.totalAmount.toInt(),
                        montoDisponible = 0, // Se recalcula luego
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
            }
        } catch (e: Exception) {
            Log.e("API_BUDGET", "loadBudgetsFromApi failed", e)
        }
    }

    private suspend fun loadDefaultLocalBudgets() {
        val cached = appDao.getPresupuestos()
        if (cached.isNotEmpty()) {
            presupuestos = cached.map { it.toModel() }.toMutableList()
        } else {
            inicializarDatos()
        }
    }

    private fun recalcularMontoDisponible() {
        val updated = presupuestos.map { p ->
            val spent = tickets.filter { it.presupuestoId == p.id }.sumOf { it.total }
            p.copy(montoDisponible = maxOf(0, p.montoTotal - spent))
        }
        presupuestos.clear()
        presupuestos.addAll(updated)
    }

    private suspend fun syncBudgetToApi(presupuesto: Presupuesto) {
        if (presupuesto.id.startsWith("presupuesto-")) return // Ignorar locales

        try {
            val request = UpdateBudgetRequest(
                name = presupuesto.nombre,
                totalAmount = presupuesto.montoTotal.toDouble()
            )
            apiService.updateBudget(presupuesto.id, request)
        } catch (e: Exception) {
            Log.e("API_BUDGET", "syncBudgetToApi failed", e)
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
                AuthSessionStore.save(application.applicationContext, authData.accessToken, authData.refreshToken)

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
                cargarTodo() // Recargar todo tras el login exitoso
                Result.success(usuario)
            } else {
                val msg = response.body()?.message ?: "Error de autenticación"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
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
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
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

    suspend fun actualizarPresupuesto(id: String, nuevoMonto: Int) {
        withContext(Dispatchers.IO) {
            val index = presupuestos.indexOfFirst { it.id == id }
            if (index != -1) {
                val actual = presupuestos[index]
                val gastado = actual.montoTotal - actual.montoDisponible
                val updated = actual.copy(
                    montoTotal = nuevoMonto,
                    montoDisponible = maxOf(0, nuevoMonto - gastado)
                )
                presupuestos[index] = updated
                syncBudgetToApi(updated)
            }
        }
    }

    // ======================
    // LISTAS
    // ======================

    suspend fun agregarLista(lista: ListaCompra) {
        withContext(Dispatchers.IO) {
            if (listas.none { it.id == lista.id }) {
                listas.add(lista)
            }
        }
    }

    suspend fun agregarOActualizarProducto(listaId: String, producto: Producto) {
        withContext(Dispatchers.IO) {
            val indexLista = listas.indexOfFirst { it.id == listaId }
            if (indexLista != -1) {
                val lista = listas[indexLista]
                val nuevosProductos = lista.productos.toMutableList()
                val indexProd = nuevosProductos.indexOfFirst { it.id == producto.id }
                if (indexProd != -1) nuevosProductos[indexProd] = producto
                else nuevosProductos.add(producto)
                
                listas[indexLista] = lista.copy(productos = nuevosProductos, total = nuevosProductos.sumOf { it.precioEstimado * it.cantidad })
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
            nuevosProductos[indexProd] = producto.copy(comprado = !producto.comprado)
            listas[indexLista] = lista.copy(productos = nuevosProductos)
        }
    }

    // ======================
    // TICKETS
    // ======================

    suspend fun agregarTicket(ticket: Ticket) {
        withContext(Dispatchers.IO) {
            val presupuestoActivo = presupuestos.find { it.activo }
            val ticketVinculado = if (presupuestoActivo != null) {
                ticket.copy(presupuestoId = presupuestoActivo.id)
            } else ticket

            tickets.add(0, ticketVinculado)
            
            // Sincronizar con backend si es posible
            if (!ticketVinculado.presupuestoId.startsWith("presupuesto-")) {
                try {
                    val items = ticketVinculado.productos.mapNotNull { tp ->
                        val pid = productNameToId[tp.nombre.lowercase()] ?: return@mapNotNull null
                        CreatePurchaseItem(pid, tp.cantidad)
                    }
                    if (items.isNotEmpty()) {
                        val groupId = grupos.firstOrNull()?.id ?: return@withContext
                        val storeId = storeNameToId[ticketVinculado.supermercado.lowercase()]
                        apiService.createPurchase(CreatePurchaseRequest(groupId, storeId, if (storeId == null) ticketVinculado.supermercado else null, items))
                        Log.d("API_PURCH", "Ticket sincronizado con éxito")
                    }
                } catch (e: Exception) {
                    Log.w("API_PURCH", "Fallo al sincronizar ticket", e)
                }
            }
            
            recalcularMontoDisponible()
        }
    }

    suspend fun eliminarTicket(id: String) {
        withContext(Dispatchers.IO) {
            tickets.removeAll { it.id == id }
            try {
                apiService.deletePurchase(id)
            } catch (_: Exception) {}
            recalcularMontoDisponible()
        }
    }

    suspend fun actualizarTicket(ticket: Ticket) {
        withContext(Dispatchers.IO) {
            val index = tickets.indexOfFirst { it.id == ticket.id }
            if (index != -1) {
                tickets[index] = ticket
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
        }
    }

    suspend fun actualizarUsuario(usuario: Usuario) {
        withContext(Dispatchers.IO) {
            if (usuario.id == usuarioActual?.id) {
                usuarioActual = usuario
                try {
                    apiService.updateProfile(mapOf("fullName" to usuario.nombre))
                } catch (_: Exception) {}
            }
        }
    }

    private suspend fun fetchUserProfile() {
        try {
            val response = apiService.getProfile()
            if (response.isSuccessful && response.body()?.success == true) {
                val profile = response.body()!!.data!!
                usuarioActual = Usuario(
                    id = profile.id,
                    nombre = profile.fullName,
                    email = profile.email,
                    rol = if (profile.role == "ADMIN") RolUsuario.ADMIN else RolUsuario.MIEMBRO,
                    activo = true
                )
            }
        } catch (_: Exception) {}
    }

    suspend fun inviteMember(groupId: String, email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.inviteMember(groupId, InviteRequest(email))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error al invitar"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptInvitation(token: String) = withContext(Dispatchers.IO) {
        apiService.acceptInvitation(token)
    }

    suspend fun rejectInvitation(token: String) = withContext(Dispatchers.IO) {
        apiService.rejectInvitation(token)
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
    }

    suspend fun analizarTicketImagen(imageBytes: ByteArray, mimeType: String): TicketImageAnalysis = withContext(Dispatchers.IO) {
        // TODO: Implementar análisis de imagen (OCR/AI)
        TicketImageAnalysis(null, null, null, emptyList())
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

private fun GroupMemberResponse.toUsuario() = Usuario(
    id = id, nombre = fullName, email = email,
    rol = if (role == "ADMIN") RolUsuario.ADMIN else RolUsuario.MIEMBRO,
    activo = true
)
