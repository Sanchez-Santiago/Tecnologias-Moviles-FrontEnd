package com.undef.superahorrosanchezpucci.data.repository

import android.app.Application
import android.util.Log
import com.undef.superahorrosanchezpucci.data.local.AppDatabase
import com.undef.superahorrosanchezpucci.data.local.CatalogoProductoEntity
import com.undef.superahorrosanchezpucci.data.local.GrupoEntity
import com.undef.superahorrosanchezpucci.data.local.InvitacionEntity
import com.undef.superahorrosanchezpucci.data.local.NotificationCacheEntity
import com.undef.superahorrosanchezpucci.data.local.OfferCacheEntity
import com.undef.superahorrosanchezpucci.data.local.PreferencesDataStore
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AppRepository(private val application: Application) {

    private val database = AppDatabase.get(application)
    private val appDao = database.appDao()
    private val apiService by lazy { RetrofitClient.getApiService() }
    private val preferencesDataStore = PreferencesDataStore(application)

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
        cargarDesdeCache()
        
        if (AuthSessionStore.accessToken.isNullOrBlank()) {
            if (presupuestos.isEmpty()) inicializarDatos()
            return@withContext
        }

        try {
            themeMode = preferencesDataStore.themeModeFlow.first()
            fetchUserProfile()
            
            coroutineScope {
                val productsDef = async { cargarProductos() }
                val storesDef = async { cargarTiendas() }
                productsDef.await()
                storesDef.await()
            }
            
            ensureGroupAndBudget()
            
            val grupoId = grupos.firstOrNull()?.id
            if (grupoId != null) {
                coroutineScope {
                    val budgetsDef = async { loadBudgetsFromApi(grupoId) }
                    val purchasesDef = async { cargarCompras(grupoId) }
                    val listsDef = async { cargarListas(grupoId) }
                    val notificationsDef = async { loadNotifications() }
                    val offersDef = async { loadActiveOffers() }
                    val invitationsDef = async { cargarInvitaciones() }
                    budgetsDef.await()
                    purchasesDef.await()
                    listsDef.await()
                    notificationsDef.await()
                    offersDef.await()
                    invitationsDef.await()
                }
            } else {
                coroutineScope {
                    val notificationsDef = async { loadNotifications() }
                    val offersDef = async { loadActiveOffers() }
                    val invitationsDef = async { cargarInvitaciones() }
                    notificationsDef.await()
                    offersDef.await()
                    invitationsDef.await()
                }
            }
            
            recalcularMontoDisponible()
        } catch (e: Exception) {
            Log.e("AppRepository", "Error en cargarTodo", e)
        }
    }

    private suspend fun ensureGroupAndBudget() {
        // 1. Cargar grupos existentes
        cargarGrupos()
        
        // 2. Si no hay grupos, crear uno por defecto
        if (grupos.isEmpty()) {
            Log.d("AppRepository", "No se encontraron grupos, creando uno...")
            val createGroupResp = apiService.createGroup(CreateGroupRequest("Mi Grupo", "Grupo de Super Ahorro"))
            if (createGroupResp.isSuccessful && createGroupResp.body()?.success == true) {
                cargarGrupos() // Recargar para tener el detalle
            }
        }
        
        val grupoId = grupos.firstOrNull()?.id ?: return
        
        // 3. Asegurar que el grupo tenga al menos un presupuesto
        val budgetsResp = apiService.getBudgets(grupoId)
        if (budgetsResp.isSuccessful) {
            val budgetsData = budgetsResp.body()?.data
            if (budgetsData == null || budgetsData.isEmpty()) {
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
                val apiPurchases = response.body()?.data ?: emptyList()
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
                // Cachear en Room
                appDao.clearTickets()
                appDao.clearTicketProductos()
                appDao.insertTickets(tickets.map { it.ticketToEntity() })
                tickets.forEach { ticket ->
                    ticket.productos.forEachIndexed { index, tp ->
                        appDao.insertTicketProductos(listOf(tp.toEntity(ticket.id, index)))
                    }
                }
                return
            }
        } catch (e: Exception) {
            Log.e("API_PURCH", "cargarCompras failed", e)
        }
        // Fallback: leer tickets de Room
        val cached = appDao.getTickets()
        if (cached.isNotEmpty()) {
            tickets = cached.map { entity ->
                val productos = appDao.getTicketProductosByTicketId(entity.id).map { it.toModel() }
                entity.toModel(productos)
            }.toMutableList()
        }
    }

    private suspend fun cargarListas(grupoId: String) {
        if (!isLoggedIn()) {
            inicializarListas()
            return
        }
        try {
            val response = apiService.getShoppingLists(grupoId)
            if (response.isSuccessful && response.body()?.success == true) {
                val apiLists = response.body()!!.data ?: emptyList()
                if (apiLists.isNotEmpty()) {
                    listas.clear()
                    for (apiList in apiLists) {
                        val presupuestoId = presupuestos.firstOrNull()?.id ?: ""
                        val productos = apiList.products.map { prod ->
                            Producto(
                                id = prod.productId,
                                nombre = prod.productName,
                                codigo = prod.productId,
                                precio = prod.finalPrice?.toInt() ?: 0,
                                precioEstimado = prod.finalPrice?.toInt() ?: 0,
                                cantidad = prod.finalQuantity?.toInt() ?: 1,
                                comprado = prod.checked,
                                categoria = Categoria.ESENCIAL
                            )
                        }
                        listas.add(ListaCompra(
                            id = apiList.id,
                            nombre = apiList.name,
                            presupuestoId = presupuestoId,
                            esFamiliar = presupuestos.any { it.tipo == TipoPresupuesto.FAMILIAR && it.activo },
                            fechaCreacion = try {
                                LocalDateTime.parse(apiList.createdAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                    .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                            } catch (_: Exception) { System.currentTimeMillis() },
                            total = productos.sumOf { it.precioEstimado * it.cantidad },
                            productos = productos.toMutableList()
                        ))
                    }
                    persistirListas()
                    return
                }
            }
        } catch (e: Exception) {
            Log.e("API_LIST", "cargarListas failed", e)
        }
        // Fallback: keep existing local lists
        inicializarListas()
    }

    private suspend fun syncListToApi(lista: ListaCompra): String {
        if (!isLoggedIn()) return lista.id
        if (!lista.id.startsWith("lista-")) return lista.id

        val grupoId = grupos.firstOrNull()?.id ?: return lista.id
        return try {
            val response = apiService.createShoppingList(CreateShoppingListRequest(
                groupId = grupoId,
                name = lista.nombre,
                description = null
            ))
            if (response.isSuccessful && response.body()?.success == true) {
                response.body()!!.data!!.id
            } else {
                lista.id
            }
        } catch (e: Exception) {
            Log.e("API_LIST", "syncListToApi failed", e)
            lista.id
        }
    }

    private suspend fun cargarGrupos() {
        try {
            val response = apiService.getMyGroups()
            if (response.isSuccessful && response.body()?.success == true) {
                val groupsList = response.body()?.data ?: emptyList()
                grupos.clear()
                for (g in groupsList) {
                    val detailResponse = apiService.getGroupById(g.id)
                    if (detailResponse.isSuccessful && detailResponse.body()?.success == true) {
                        detailResponse.body()!!.data?.let { grupos.add(it) }
                    }
                }
                actualizarUsuariosDesdeGrupos()
                // Cachear en Room
                appDao.clearGrupos()
                appDao.insertGrupos(grupos.map { it.toGrupoEntity() })
                return
            }
        } catch (e: Exception) {
            Log.e("API_GROUPS", "cargarGrupos failed", e)
        }
        // Fallback: leer grupos de Room
        val cached = appDao.getGrupos()
        if (cached.isNotEmpty()) {
            grupos = cached.map { it.toGroupDetailResponse() }.toMutableList()
            actualizarUsuariosDesdeGrupos()
        }
    }

    private suspend fun cargarInvitaciones() {
        try {
            val response = apiService.getMyInvitations()
            if (response.isSuccessful && response.body()?.success == true) {
                invitaciones = (response.body()!!.data ?: emptyList()).toMutableList()
                appDao.clearInvitaciones()
                appDao.insertInvitaciones(invitaciones.map { it.toInvitacionEntity() })
                return
            }
        } catch (e: Exception) {
            Log.e("API_INVITE", "cargarInvitaciones failed", e)
        }
        val cached = appDao.getInvitaciones()
        if (cached.isNotEmpty()) {
            invitaciones = cached.map { it.toInvitationResponse() }.toMutableList()
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
        persistirTickets()
        appDao.clearPresupuestos()
        appDao.clearUsuarios()
        appDao.insertPresupuestos(presupuestos.map { it.toEntity() })
        appDao.insertUsuarios(usuarios.map { it.toEntity() }.toList())
        persistirListas()
    }

    private suspend fun persistirListas() {
        appDao.clearListas()
        appDao.clearProductos()
        appDao.insertListas(listas.map { it.toEntity() })
        listas.forEach { lista ->
            appDao.insertProductos(lista.productos.map { it.toEntity(lista.id) })
        }
    }

    private suspend fun persistirTickets() {
        appDao.clearTicketProductos()
        appDao.clearTickets()
        appDao.insertTickets(tickets.map { it.ticketToEntity() })
        tickets.forEach { ticket ->
            ticket.productos.forEachIndexed { index, tp ->
                appDao.insertTicketProductos(listOf(tp.toEntity(ticket.id, index)))
            }
        }
    }

    private suspend fun cargarDesdeCache() {
        val cachedPresupuestos = appDao.getPresupuestos()
        if (cachedPresupuestos.isNotEmpty()) {
            presupuestos = cachedPresupuestos.map { it.toModel() }.toMutableList()
        }
        val cachedTickets = appDao.getTickets()
        if (cachedTickets.isNotEmpty()) {
            tickets = cachedTickets.map { entity ->
                val productos = appDao.getTicketProductosByTicketId(entity.id).map { it.toModel() }
                entity.toModel(productos)
            }.toMutableList()
        }
        val cachedUsuarios = appDao.getUsuarios()
        if (cachedUsuarios.isNotEmpty()) {
            val u = cachedUsuarios.first()
            usuarioActual = Usuario(id = u.id, nombre = u.nombre, email = u.email,
                rol = try { RolUsuario.valueOf(u.rol) } catch (_: Exception) { RolUsuario.MIEMBRO },
                activo = u.activo)
            usuarios = cachedUsuarios.map { ue ->
                Usuario(id = ue.id, nombre = ue.nombre, email = ue.email,
                    rol = try { RolUsuario.valueOf(ue.rol) } catch (_: Exception) { RolUsuario.MIEMBRO },
                    activo = ue.activo)
            }.toMutableList()
        }
        val cachedGrupos = appDao.getGrupos()
        if (cachedGrupos.isNotEmpty()) {
            grupos = cachedGrupos.map { it.toGroupDetailResponse() }.toMutableList()
        }
        val cachedInvitaciones = appDao.getInvitaciones()
        if (cachedInvitaciones.isNotEmpty()) {
            invitaciones = cachedInvitaciones.map { it.toInvitationResponse() }.toMutableList()
        }
        val cachedNotifications = appDao.getCachedNotifications()
        if (cachedNotifications.isNotEmpty()) {
            notifications = cachedNotifications.map { it.toNotificationResponse() }.toMutableList()
        }
        val cachedOffers = appDao.getCachedOffers()
        if (cachedOffers.isNotEmpty()) {
            offers = cachedOffers.map { it.toOfferResponse() }.toMutableList()
        }
        val cachedListas = appDao.getListas()
        if (cachedListas.isNotEmpty()) {
            listas = cachedListas.map { entity ->
                val productos = appDao.getProductosByListaId(entity.id).map { it.toModel() }
                entity.toModel(productos)
            }.toMutableList()
        }
        recalcularMontoDisponible()
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
                        montoDisponible = 0,
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
                if (presupuestos.none { it.tipo == TipoPresupuesto.INDIVIDUAL }) {
                    val familiar = presupuestos.first()
                    presupuestos.add(Presupuesto(
                        id = "individual_${familiar.id}",
                        tipo = TipoPresupuesto.INDIVIDUAL,
                        nombre = "Individual",
                        montoTotal = familiar.montoTotal,
                        montoDisponible = 0,
                        fechaInicio = familiar.fechaInicio,
                        fechaFin = familiar.fechaFin,
                        activo = false
                    ))
                }
                appDao.clearPresupuestos()
                appDao.insertPresupuestos(presupuestos.map { it.toEntity() })
                return
            }
        } catch (e: Exception) {
            Log.e("API_BUDGET", "loadBudgetsFromApi failed", e)
        }
        loadDefaultLocalBudgets()
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
                
                // Intentamos recargar todo, pero capturamos errores para no romper el login
                try {
                    cargarTodo()
                } catch (e: Exception) {
                    Log.e("AppRepository", "Error al cargar datos tras login", e)
                }

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
            notifications.clear()
            unreadCount = 0
            offers.clear()
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
            if (presupuestos.none { it.id == id }) {
                val otroTipo = if (presupuestos.any { it.tipo == TipoPresupuesto.FAMILIAR })
                    TipoPresupuesto.INDIVIDUAL else TipoPresupuesto.FAMILIAR
                val existing = presupuestos.firstOrNull()
                presupuestos.add(Presupuesto(
                    id = id,
                    tipo = otroTipo,
                    nombre = if (otroTipo == TipoPresupuesto.FAMILIAR) "Familiar" else "Individual",
                    montoTotal = existing?.montoTotal ?: 0,
                    montoDisponible = 0,
                    fechaInicio = System.currentTimeMillis(),
                    fechaFin = null,
                    activo = false
                ))
            }
            val updated = presupuestos.map { it.copy(activo = it.id == id) }
            presupuestos.clear()
            presupuestos.addAll(updated)

            appDao.clearPresupuestos()
            appDao.insertPresupuestos(presupuestos.map { it.toEntity() })

            if (!id.startsWith("presupuesto-") && !id.startsWith("individual_")) {
                activarPresupuestoEnApi(id)
            }

            recalcularMontoDisponible()
        }
    }

    private suspend fun activarPresupuestoEnApi(id: String) {
        if (!isLoggedIn()) return
        try {
            apiService.activateBudget(id)
        } catch (e: Exception) {
            Log.e("API_BUDGET", "activateBudget failed", e)
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
                val apiId = syncListToApi(lista)
                val finalLista = if (apiId != lista.id) lista.copy(id = apiId) else lista
                listas.add(finalLista)
                appDao.insertListas(listOf(finalLista.toEntity()))
            }
        }
    }

    private suspend fun syncProductToApi(listaId: String, producto: Producto, isNew: Boolean) {
        if (!isLoggedIn()) return
        val effectiveListId = if (listaId.startsWith("lista-")) return else listaId
        val productPid = productNameToId[producto.nombre.lowercase()]
        if (productPid == null) {
            Log.w("API_LIST", "Producto '${producto.nombre}' no encontrado en catálogo, saltando sync")
            return
        }
        try {
            if (isNew) {
                apiService.addProductToList(effectiveListId, AddProductRequest(
                    productId = productPid,
                    quantity = producto.cantidad.toDouble(),
                    notes = null
                ))
            } else {
                apiService.updateProductInList(effectiveListId, producto.id, UpdateProductRequest(
                    checked = producto.comprado,
                    finalPrice = producto.precioEstimado.toDouble(),
                    finalQuantity = producto.cantidad.toDouble(),
                    notes = null
                ))
            }
        } catch (e: Exception) {
            Log.e("API_LIST", "syncProductToApi failed", e)
        }
    }

    suspend fun agregarOActualizarProducto(listaId: String, producto: Producto) {
        withContext(Dispatchers.IO) {
            val indexLista = listas.indexOfFirst { it.id == listaId }
            if (indexLista != -1) {
                val lista = listas[indexLista]
                val nuevosProductos = lista.productos.toMutableList()
                val indexProd = nuevosProductos.indexOfFirst { it.id == producto.id }
                val isNew = indexProd == -1
                if (!isNew) nuevosProductos[indexProd] = producto
                else nuevosProductos.add(producto)
                
                listas[indexLista] = lista.copy(productos = nuevosProductos, total = nuevosProductos.sumOf { it.precioEstimado * it.cantidad })
                appDao.deleteProductosByListaId(listaId)
                appDao.insertProductos(nuevosProductos.map { it.toEntity(listaId) })

                syncProductToApi(listaId, producto, isNew)
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
                appDao.deleteProductosByListaId(listaId)
                appDao.insertProductos(nuevosProductos.map { it.toEntity(listaId) })

                if (isLoggedIn() && !listaId.startsWith("lista-")) {
                    try {
                        apiService.deleteProductFromList(listaId, productoId)
                    } catch (e: Exception) {
                        Log.e("API_LIST", "eliminarProducto API failed", e)
                    }
                }
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
            val updated = producto.copy(comprado = !producto.comprado)
            nuevosProductos[indexProd] = updated
            listas[indexLista] = lista.copy(productos = nuevosProductos)
            appDao.deleteProductosByListaId(listaId)
            appDao.insertProductos(nuevosProductos.map { it.toEntity(listaId) })

            if (isLoggedIn() && !listaId.startsWith("lista-")) {
                try {
                    apiService.updateProductInList(listaId, productoId, UpdateProductRequest(
                        checked = updated.comprado
                    ))
                } catch (e: Exception) {
                    Log.e("API_LIST", "toggleProducto API failed", e)
                }
            }
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
            persistirTickets()

            if (!ticketVinculado.presupuestoId.startsWith("presupuesto-")) {
                try {
                    val items = ticketVinculado.productos.mapNotNull { tp ->
                        val pid = productNameToId[tp.nombre.lowercase()]
                        if (pid != null) CreatePurchaseItem(pid, tp.cantidad) else null
                    }
                    val groupId = grupos.firstOrNull()?.id ?: return@withContext
                    val storeId = storeNameToId[ticketVinculado.supermercado.lowercase()]
                    if (items.isNotEmpty()) {
                        apiService.createPurchase(CreatePurchaseRequest(groupId, storeId, if (storeId == null) ticketVinculado.supermercado else null, items))
                        Log.d("API_PURCH", "Ticket sincronizado con éxito")
                    } else {
                        Log.w("API_PURCH", "Ticket guardado localmente (sin productos compatibles con API)")
                    }
                } catch (e: Exception) {
                    Log.w("API_PURCH", "Fallo al sincronizar ticket, guardado en Room", e)
                }
            }

            recalcularMontoDisponible()
        }
    }

    suspend fun eliminarTicket(id: String) {
        withContext(Dispatchers.IO) {
            tickets.removeAll { it.id == id }
            persistirTickets()
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
                persistirTickets()
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
                    val body = mutableMapOf("fullName" to usuario.nombre)
                    body["email"] = usuario.email
                    val response = apiService.updateProfile(body)
                    if (response.isSuccessful) {
                        response.body()?.data?.let { profile ->
                            usuarioActual = Usuario(
                                id = profile.id,
                                nombre = profile.fullName,
                                email = profile.email,
                                rol = if (profile.role == "ADMIN") RolUsuario.ADMIN else RolUsuario.MIEMBRO,
                                activo = true
                            )
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun cambiarPassword(currentPassword: String, newPassword: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.changePassword(mapOf("currentPassword" to currentPassword, "newPassword" to newPassword))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data?.get("message") ?: "Contraseña actualizada")
            } else {
                val msg = response.body()?.message ?: "Error al cambiar la contraseña"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
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
                appDao.clearUsuarios()
                appDao.insertUsuarios(listOf(usuarioActual!!.toEntity()))
                return
            }
        } catch (_: Exception) {}
        // Fallback: leer usuario de Room
        val cached = appDao.getUsuarios()
        if (cached.isNotEmpty()) {
            val u = cached.first()
            usuarioActual = Usuario(id = u.id, nombre = u.nombre, email = u.email,
                rol = try { RolUsuario.valueOf(u.rol) } catch (_: Exception) { RolUsuario.MIEMBRO },
                activo = u.activo)
        }
    }

    suspend fun crearGrupo(nombre: String, categoria: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.createGroup(CreateGroupRequest(nombre, categoria = categoria))
            if (response.isSuccessful && response.body()?.success == true) {
                cargarGrupos()
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error al crear grupo"))
            }
        } catch (e: Exception) {
            Log.e("API_GROUPS", "crearGrupo failed", e)
            Result.failure(e)
        }
    }

    suspend fun cambiarGrupoActivo(grupoId: String) = withContext(Dispatchers.IO) {
        val grupo = grupos.find { it.id == grupoId } ?: return@withContext
        loadBudgetsFromApi(grupoId)
        cargarCompras(grupoId)
        cargarListas(grupoId)
        recalcularMontoDisponible()
    }

    var notifications = mutableListOf<NotificationResponse>()
    var unreadCount = 0
    var offers = mutableListOf<OfferResponse>()

    suspend fun loadActiveOffers() = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getActiveOffers()
            if (response.isSuccessful && response.body()?.success == true) {
                offers = (response.body()?.data ?: emptyList()).toMutableList()
                appDao.clearCachedOffers()
                appDao.insertCachedOffers(offers.map { o ->
                    OfferCacheEntity(
                        id = o.id, storeId = o.storeId, storeName = o.storeName,
                        title = o.title, description = o.description,
                        discountType = o.discountType, discountValue = o.discountValue,
                        startDate = o.startDate ?: "", endDate = o.endDate ?: "",
                        imageUrl = o.imageUrl
                    )
                })
                return@withContext
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "Error loading offers", e)
        }
        val cached = appDao.getCachedOffers()
        if (cached.isNotEmpty()) {
            offers = cached.map { it.toOfferResponse() }.toMutableList()
        }
    }

    suspend fun loadNotifications() = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getNotifications()
            if (response.isSuccessful && response.body()?.success == true) {
                notifications = (response.body()?.data ?: emptyList()).toMutableList()
                appDao.clearCachedNotifications()
                appDao.insertCachedNotifications(notifications.map { n ->
                    NotificationCacheEntity(
                        id = n.id, type = n.type, title = n.title,
                        message = n.message, data = n.data, read = n.read,
                        createdAt = n.createdAt
                    )
                })
                return@withContext
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "Error loading notifications", e)
        }
        val cached = appDao.getCachedNotifications()
        if (cached.isNotEmpty()) {
            notifications = cached.map { it.toNotificationResponse() }.toMutableList()
        }
    }

    suspend fun loadUnreadCount() = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getUnreadNotificationsCount()
            if (response.isSuccessful && response.body()?.success == true) {
                unreadCount = response.body()?.data?.count ?: 0
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "Error loading unread count", e)
        }
    }

    suspend fun markNotificationRead(id: String) = withContext(Dispatchers.IO) {
        try {
            apiService.markNotificationRead(id)
            val idx = notifications.indexOfFirst { it.id == id }
            if (idx >= 0) {
                notifications[idx] = notifications[idx].copy(read = true)
                unreadCount = (unreadCount - 1).coerceAtLeast(0)
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "Error marking notification read", e)
        }
    }

    suspend fun markAllNotificationsRead() = withContext(Dispatchers.IO) {
        try {
            apiService.markAllNotificationsRead()
            notifications = notifications.map { it.copy(read = true) }.toMutableList()
            unreadCount = 0
        } catch (e: Exception) {
            Log.e("AppRepository", "Error marking all notifications read", e)
        }
    }

    suspend fun deleteNotification(id: String) = withContext(Dispatchers.IO) {
        try {
            apiService.deleteNotification(id)
            notifications.removeAll { it.id == id }
        } catch (e: Exception) {
            Log.e("AppRepository", "Error deleting notification", e)
        }
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

    suspend fun acceptInvitation(token: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.acceptInvitation(token)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error al aceptar invitación"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectInvitation(token: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.rejectInvitation(token)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error al rechazar invitación"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
        preferencesDataStore.saveThemeMode(mode)
    }

    suspend fun loadBudgetProgress(groupId: String): Result<List<BudgetProgress>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getBudgetProgress(groupId)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error al cargar progreso del presupuesto"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadSpendingByCategory(groupId: String): Result<List<SpendingByCategory>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getSpendingByCategory(groupId)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error al cargar gastos por categoría"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadSpendingByStore(groupId: String): Result<List<SpendingByStore>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getSpendingByStore(groupId)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error al cargar gastos por tienda"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadSpendingByImportance(groupId: String): Result<List<SpendingByImportance>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getSpendingByImportance(groupId)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error al cargar gastos por importancia"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadStoreFrequency(groupId: String): Result<List<StoreFrequency>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMostFrequentStore(groupId)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                // Fallback local calculation
                val groupTickets = tickets.filter { it.presupuestoId == groupId }
                if (groupTickets.isEmpty()) return@withContext Result.success(emptyList())
                
                val totalSpent = groupTickets.sumOf { it.total }.toDouble()
                val stats = groupTickets.groupBy { it.supermercado }
                    .map { (name, list) ->
                        val spent = list.sumOf { it.total }.toDouble()
                        StoreFrequency(null, name, list.size, spent, (spent/totalSpent)*100)
                    }
                    .sortedByDescending { it.purchaseCount }
                Result.success(stats)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadMostPurchasedProducts(groupId: String): Result<List<MostPurchasedProduct>> = withContext(Dispatchers.IO) {
        try {
            val groupTickets = tickets.filter { it.presupuestoId == groupId }
            val products = groupTickets.flatMap { it.productos }
            val stats = products.groupBy { it.nombre }
                .map { (name, list) ->
                    MostPurchasedProduct(name, list.sumOf { it.cantidad }, list.sumOf { (it.precio * it.cantidad).toDouble() })
                }
                .sortedByDescending { it.count }
                .take(5)
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadMemberSpending(groupId: String): Result<List<MemberSpending>> = withContext(Dispatchers.IO) {
        try {
            // Nota: El modelo Ticket actual no tiene userId, pero el backend sí lo maneja.
            // Por ahora devolvemos vacío o intentamos cargar de una API si existiera.
            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadMonthlySummary(groupId: String): Result<List<MonthlySummary>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMonthlySummary(groupId)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error al cargar resumen mensual"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun aiSuggestOffers(productNames: List<String>, storeId: String? = null): Result<List<AiOfferSuggestion>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.aiSuggestOffers(AiOfferSuggestionRequest(productNames, storeId))
            if (response.isSuccessful && response.body()?.success == true) {
                val suggestions = response.body()?.data?.suggestions ?: emptyList()
                Result.success(suggestions)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error al obtener sugerencias IA"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun analizarTicketImagen(imageBytes: ByteArray, mimeType: String): TicketImageAnalysis = withContext(Dispatchers.IO) {
        try {
            val base64 = android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP)
            val request = AnalyzeTicketImageRequest(imageBase64 = base64, mimeType = mimeType)
            val response = apiService.analyzeTicketImage(request)
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                TicketImageAnalysis(
                    storeName = data?.storeName,
                    purchaseDate = data?.purchaseDate,
                    total = data?.total?.toInt(),
                    products = data?.products?.map { p ->
                        TicketProducto(
                            nombre = p.name,
                            precio = (p.totalPrice ?: p.unitPrice ?: 0.0).toInt(),
                            cantidad = (p.quantity ?: 1.0).toInt()
                        )
                    } ?: emptyList()
                )
            } else {
                TicketImageAnalysis(null, null, null, emptyList())
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "Error analyzing ticket image", e)
            TicketImageAnalysis(null, null, null, emptyList())
        }
    }
}

// ======================
// HELPER: JSON serialize/deserialize for GroupMemberResponse
// ======================

private fun List<GroupMemberResponse>.toMembersJson(): String = JSONArray(
    map { member ->
        JSONObject().apply {
            put("id", member.id)
            put("fullName", member.fullName)
            put("email", member.email)
            put("role", member.role)
            put("joinedAt", member.joinedAt)
        }
    }
).toString()

private fun String.parseMembers(): List<GroupMemberResponse> = try {
    val arr = JSONArray(this)
    (0 until arr.length()).map { i ->
        val obj = arr.getJSONObject(i)
        GroupMemberResponse(
            id = obj.getString("id"),
            fullName = obj.getString("fullName"),
            email = obj.getString("email"),
            role = obj.getString("role"),
            joinedAt = obj.optString("joinedAt", "")
        )
    }
} catch (_: Exception) { emptyList() }

// ======================
// GROUP <-> ROOM MAPPING
// ======================

private fun GroupDetailResponse.toGrupoEntity() = GrupoEntity(
    id = id,
    name = name,
    description = description,
    categoria = categoria ?: "FAMILIA",
    createdBy = createdBy,
    membersJson = members.toMembersJson(),
    createdAt = createdAt
)

private fun GrupoEntity.toGroupDetailResponse() = GroupDetailResponse(
    id = id,
    name = name,
    description = description,
    categoria = categoria ?: "FAMILIA",
    createdBy = createdBy,
    members = membersJson.parseMembers(),
    createdAt = createdAt
)

// ======================
// INVITATION <-> ROOM MAPPING
// ======================

private fun InvitationResponse.toInvitacionEntity() = InvitacionEntity(
    id = id,
    groupId = groupId,
    groupName = groupName,
    invitedBy = invitedBy,
    invitedByEmail = invitedByEmail,
    status = status,
    token = token,
    expiresAt = expiresAt,
    createdAt = createdAt
)

private fun InvitacionEntity.toInvitationResponse() = InvitationResponse(
    id = id,
    groupId = groupId,
    groupName = groupName,
    invitedBy = invitedBy,
    invitedByEmail = invitedByEmail,
    status = status,
    token = token,
    expiresAt = expiresAt,
    createdAt = createdAt
)

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

private fun ListaCompra.toEntity() = com.undef.superahorrosanchezpucci.data.local.ListaCompraEntity(
    id = id, nombre = nombre, presupuestoId = presupuestoId, esFamiliar = esFamiliar,
    fechaCreacion = fechaCreacion, hora = hora, supermercado = supermercado, total = total
)

private fun com.undef.superahorrosanchezpucci.data.local.ListaCompraEntity.toModel(productos: List<com.undef.superahorrosanchezpucci.data.model.Producto>) = ListaCompra(
    id = id, nombre = nombre, presupuestoId = presupuestoId, esFamiliar = esFamiliar,
    fechaCreacion = fechaCreacion, hora = hora, supermercado = supermercado, total = total,
    productos = productos.toMutableList()
)

private fun com.undef.superahorrosanchezpucci.data.model.Producto.toEntity(listaId: String) = com.undef.superahorrosanchezpucci.data.local.ProductoEntity(
    id = id, listaId = listaId, codigo = codigo, nombre = nombre, descripcion = descripcion,
    precio = precio, marca = marca, precioEstimado = precioEstimado, precioReal = precioReal,
    cantidad = cantidad, comprado = comprado, categoria = categoria.name
)

// ======================
// CACHE ENTITY MAPPINGS
// ======================

private fun com.undef.superahorrosanchezpucci.data.local.NotificationCacheEntity.toNotificationResponse() = NotificationResponse(
    id = id, type = type, title = title, message = message,
    data = data, read = read, createdAt = createdAt
)

private fun com.undef.superahorrosanchezpucci.data.local.OfferCacheEntity.toOfferResponse() = OfferResponse(
    id = id, storeId = storeId, storeName = storeName,
    title = title, description = description,
    discountType = discountType, discountValue = discountValue,
    startDate = startDate, endDate = endDate,
    imageUrl = imageUrl
)

private fun com.undef.superahorrosanchezpucci.data.local.ProductoEntity.toModel() = com.undef.superahorrosanchezpucci.data.model.Producto(
    id = id, codigo = codigo, nombre = nombre, descripcion = descripcion, precio = precio,
    marca = marca, precioEstimado = precioEstimado, precioReal = precioReal,
    cantidad = cantidad, comprado = comprado, categoria = try { com.undef.superahorrosanchezpucci.data.model.Categoria.valueOf(categoria) } catch (_: Exception) { com.undef.superahorrosanchezpucci.data.model.Categoria.ESENCIAL }
)
