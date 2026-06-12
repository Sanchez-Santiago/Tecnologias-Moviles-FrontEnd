package com.undef.superahorrosanchezpucci.data.repository

import android.app.Application
import android.util.Log
import com.undef.superahorrosanchezpucci.data.local.AppDatabase
import com.undef.superahorrosanchezpucci.data.local.PreferencesDataStore
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

class AppRepository(private val application: Application) {

    private val database = AppDatabase.get(application)
    private val appDao = database.appDao()
    private val apiService by lazy { RetrofitClient.getApiService() }
    private val preferencesDataStore = PreferencesDataStore(application)

    // Sub-repositorios especializados
    val userRepo = UserRepository(apiService, appDao, application)
    val groupRepo = GroupRepository(apiService, appDao)
    val budgetRepo = BudgetRepository(apiService, appDao)
    val listRepo = ShoppingListRepository(apiService, appDao)
    val purchaseRepo = PurchaseRepository(apiService, appDao)
    val productRepo = ProductRepository(apiService, appDao)
    val notifyRepo = NotificationRepository(apiService, appDao)
    val statsRepo = StatsRepository(apiService)

    // Estado en memoria para UI (mantenido para compatibilidad con AppStateStore)
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
    var notifications = mutableListOf<NotificationResponse>()
        private set
    var offers = mutableListOf<OfferResponse>()
        private set
    var usuarioActual: Usuario? = null
        private set
    var themeMode: ThemeMode = ThemeMode.SYSTEM
        private set
    var unreadCount = 0
    var grupoIndividualId: String? = null
    var modoIndividual: Boolean = false
    var grupoRegularActivoId: String? = null

    private var isLoadingData = false

    val gruposVisibles: List<GroupDetailResponse>
        get() = if (grupoIndividualId != null) grupos.filter { it.id != grupoIndividualId } else grupos

    private var productNameToId: Map<String, String> = emptyMap()
    private var barcodeToId: Map<String, String> = emptyMap()
    private var storeNameToId: Map<String, String> = emptyMap()

    // ======================
    // COORDINACIÓN (ORQUESTACIÓN)
    // ======================

    suspend fun cargarTodo() = withContext(Dispatchers.IO) {
        if (isLoadingData) return@withContext
        isLoadingData = true
        
        try {
            cargarDesdeCache()
            
            if (AuthSessionStore.accessToken.isNullOrBlank()) {
                if (presupuestos.isEmpty()) inicializarDatosLocales()
                return@withContext
            }

            themeMode = preferencesDataStore.themeModeFlow.first()
            usuarioActual = userRepo.fetchProfile()
            
            coroutineScope {
                val productsDef = async { productRepo.fetchProducts() }
                val storesDef = async { productRepo.fetchStores() }
                val (nameMap, barcodeMap) = productsDef.await()
                productNameToId = nameMap
                barcodeToId = barcodeMap
                storeNameToId = storesDef.await()
            }
            
            ensureGroupAndBudget()
            
            val targetId = if (modoIndividual) grupoIndividualId else (grupoRegularActivoId ?: gruposVisibles.firstOrNull()?.id)
            if (targetId != null) {
                val budgetList = budgetRepo.getBudgets(targetId, modoIndividual)
                presupuestos = budgetList.toMutableList()
                
                val activeBudget = budgetList.find { it.activo } ?: budgetList.firstOrNull()
                val currentBudgetId = activeBudget?.id ?: ""
                
                coroutineScope {
                    val pDef = async { purchaseRepo.getPurchases(targetId, currentBudgetId) }
                    val lDef = async { listRepo.getShoppingLists(targetId, currentBudgetId, modoIndividual) }
                    val nDef = async { notifyRepo.getNotifications() }
                    val oDef = async { notifyRepo.getOffers() }
                    val iDef = async { groupRepo.getInvitations() }
                    
                    tickets = pDef.await().toMutableList()
                    listas = lDef.await().toMutableList()
                    notifications = nDef.await().toMutableList()
                    offers = oDef.await().toMutableList()
                    invitaciones = iDef.await().toMutableList()
                }
            }
            
            recalcularMontoDisponible()
        } catch (e: Exception) {
            Log.e("AppRepository", "Error en cargarTodo", e)
        } finally {
            isLoadingData = false
        }
    }

    private suspend fun ensureGroupAndBudget() {
        grupos = groupRepo.getGroups().toMutableList()
        detectarGrupoIndividual()

        if (grupoIndividualId == null && isLoggedIn()) {
            groupRepo.createGroup("${usuarioActual?.nombre ?: "Usuario"} (Individual)", "FAMILIA")
            grupos = groupRepo.getGroups().toMutableList()
            detectarGrupoIndividual()
        }

        if (gruposVisibles.isEmpty() && isLoggedIn()) {
            groupRepo.createGroup("Mi Familia", "FAMILIA")
            grupos = groupRepo.getGroups().toMutableList()
        }

        if (grupoRegularActivoId == null && gruposVisibles.isNotEmpty()) {
            grupoRegularActivoId = gruposVisibles.first().id
        }

        val targetId = if (modoIndividual) grupoIndividualId else (grupoRegularActivoId ?: gruposVisibles.firstOrNull()?.id)
        if (targetId != null) {
            try {
                val resp = apiService.getBudgets(targetId)
                val hasBudgets = resp.isSuccessful && resp.body()?.data?.isNotEmpty() == true
                if (!hasBudgets) budgetRepo.createDefaultBudget(targetId)
            } catch (_: Exception) {
                budgetRepo.createDefaultBudget(targetId)
            }
        }
    }

    private fun detectarGrupoIndividual() {
        val userId = AuthSessionStore.userId
        grupoIndividualId = grupos.find { 
            (it.name.endsWith("(Individual)")) && it.createdBy == userId 
        }?.id
    }

    private fun recalcularMontoDisponible() {
        presupuestos = budgetRepo.calculateAvailable(presupuestos, tickets).toMutableList()
    }

    // ======================
    // MÉTODOS DELEGADOS (Mantenidos para compatibilidad)
    // ======================

    suspend fun login(e: String, p: String) = userRepo.login(e, p).onSuccess { usuarioActual = it; cargarTodo() }
    suspend fun register(n: String, e: String, p: String) = userRepo.register(n, e, p)
    suspend fun logout() { userRepo.logout(); limpiarEstado() }
    fun isLoggedIn() = AuthSessionStore.accessToken != null

    suspend fun crearGrupo(n: String, c: String) = groupRepo.createGroup(n, c).onSuccess { cargarTodo() }
    
    suspend fun agregarLista(l: ListaCompra): String {
        val apiId = if (isLoggedIn()) listRepo.createList((if(modoIndividual) grupoIndividualId else grupoRegularActivoId)!!, l.nombre) else null
        val finalL = if (apiId != null) l.copy(id = apiId) else l
        if (listas.none { it.id == finalL.id }) listas.add(finalL)
        listRepo.localSaveList(finalL)
        return finalL.id
    }

    suspend fun agregarOActualizarProducto(listaId: String, p: Producto) {
        val index = listas.indexOfFirst { it.id == listaId }
        if (index != -1) {
            val isNew = listas[index].productos.none { it.id == p.id }
            val updatedProds = listas[index].productos.toMutableList().apply { 
                val i = indexOfFirst { it.id == p.id }
                if (i != -1) set(i, p) else add(p)
            }
            listas[index] = listas[index].copy(productos = updatedProds)
            listRepo.localSaveProducts(listaId, updatedProds)
            
            val catalogId = barcodeToId[p.codigo] ?: productNameToId[p.nombre.lowercase().trim()]
            listRepo.syncProduct(listaId, p, isNew, catalogId)
        }
    }

    suspend fun eliminarProducto(listaId: String, productoId: String) {
        val index = listas.indexOfFirst { it.id == listaId }
        if (index != -1) {
            val updatedProds = listas[index].productos.filter { it.id != productoId }.toMutableList()
            listas[index] = listas[index].copy(productos = updatedProds)
            listRepo.deleteProduct(listaId, productoId)
        }
    }

    suspend fun toggleProducto(listaId: String, productoId: String) {
        val index = listas.indexOfFirst { it.id == listaId }
        if (index != -1) {
            val pIndex = listas[index].productos.indexOfFirst { it.id == productoId }
            if (pIndex != -1) {
                val p = listas[index].productos[pIndex]
                val updatedP = p.copy(comprado = !p.comprado)
                val updatedProds = listas[index].productos.toMutableList().apply { set(pIndex, updatedP) }
                listas[index] = listas[index].copy(productos = updatedProds)
                listRepo.syncProduct(listaId, updatedP, false, productNameToId[updatedP.nombre.lowercase().trim()])
            }
        }
    }

    suspend fun agregarTicket(t: Ticket) {
        val activo = presupuestos.find { it.activo }
        val vinculado = if (activo != null) t.copy(presupuestoId = activo.id) else t
        tickets.add(0, vinculado)
        purchaseRepo.localSaveTickets(tickets)
        
        if (!vinculado.presupuestoId.startsWith("presupuesto-")) {
            val items = vinculado.productos.mapNotNull { tp ->
                val pid = productNameToId[tp.nombre.lowercase().trim()]
                if (pid != null) CreatePurchaseItem(pid, tp.cantidad) else null
            }
            val gid = (if(modoIndividual) grupoIndividualId else grupoRegularActivoId)
            val sid = storeNameToId[vinculado.supermercado.lowercase().trim()]
            if (gid != null && items.isNotEmpty()) purchaseRepo.createPurchase(gid, sid, vinculado.supermercado, items)
        }
        recalcularMontoDisponible()
    }

    suspend fun eliminarTicket(id: String) {
        tickets.removeAll { it.id == id }
        purchaseRepo.deletePurchase(id)
        recalcularMontoDisponible()
    }

    suspend fun actualizarTicket(t: Ticket) {
        val i = tickets.indexOfFirst { it.id == t.id }
        if (i != -1) {
            tickets[i] = t
            // purchaseRepo.updatePurchase(t) // To be implemented
            recalcularMontoDisponible()
        }
    }

    suspend fun switchModo(individual: Boolean): Result<Unit> {
        modoIndividual = individual
        limpiarEstadoExceptoUsuario()
        cargarTodo()
        return Result.success(Unit)
    }

    // Resto de métodos delegados simples...
    suspend fun loadUnreadCount() { unreadCount = notifyRepo.getUnreadCount() }
    suspend fun markNotificationRead(id: String) { notifyRepo.markRead(id); loadNotifications() }
    suspend fun markAllNotificationsRead() { notifyRepo.markAllRead(); loadNotifications() }
    suspend fun deleteNotification(id: String) { notifyRepo.delete(id); loadNotifications() }
    suspend fun loadNotifications() { notifications = notifyRepo.getNotifications().toMutableList() }
    suspend fun loadActiveOffers() { offers = notifyRepo.getOffers().toMutableList() }
    suspend fun acceptInvitation(t: String) = groupRepo.acceptInvitation(t).onSuccess { cargarTodo() }
    suspend fun rejectInvitation(t: String) = groupRepo.rejectInvitation(t).onSuccess { cargarTodo() }
    suspend fun inviteMember(gid: String, e: String) = groupRepo.inviteMember(gid, e)
    suspend fun updateThemeMode(m: ThemeMode) { themeMode = m; preferencesDataStore.saveThemeMode(m) }
    suspend fun analizarTicketImagen(b: ByteArray, m: String) = purchaseRepo.analyzeTicket(b, m)
    suspend fun actualizarPresupuesto(id: String, monto: Int) {
        val i = presupuestos.indexOfFirst { it.id == id }
        if (i != -1) {
            presupuestos[i] = presupuestos[i].copy(montoTotal = monto)
            budgetRepo.updateBudget(presupuestos[i])
            recalcularMontoDisponible()
        }
    }
    suspend fun sincronizarPresupuesto(id: String) {
        val p = presupuestos.find { it.id == id }
        if (p != null) budgetRepo.updateBudget(p)
    }
    suspend fun cambiarPresupuestoActivo(id: String) {
        budgetRepo.activateBudget(id)
        cargarTodo()
    }

    suspend fun agregarUsuario(u: Usuario) { /* local only or not needed */ }
    suspend fun actualizarUsuario(u: Usuario) {
        usuarioActual = userRepo.updateProfile(u.nombre, u.email)
    }
    suspend fun cambiarPassword(c: String, n: String) = userRepo.changePassword(c, n)

    suspend fun cambiarGrupoActivo(gid: String) {
        grupoRegularActivoId = gid
        limpiarEstadoExceptoUsuario()
        cargarTodo()
    }

    // Métodos de estadísticas delegados
    suspend fun loadBudgetProgress(gid: String) = statsRepo.getBudgetProgress(gid)
    suspend fun loadSpendingByCategory(gid: String) = statsRepo.getSpendingByCategory(gid)
    suspend fun loadSpendingByStore(gid: String) = statsRepo.getSpendingByStore(gid)
    suspend fun loadSpendingByImportance(gid: String) = statsRepo.getSpendingByImportance(gid)
    suspend fun loadStoreFrequency(gid: String) = statsRepo.getMostFrequentStore(gid)
    suspend fun loadMonthlySummary(gid: String) = statsRepo.getMonthlySummary(gid)
    suspend fun loadMostPurchasedProducts(gid: String): Result<List<MostPurchasedProduct>> = Result.success(emptyList()) // Stats not in API
    suspend fun loadMemberSpending(gid: String): Result<List<MemberSpending>> = Result.success(emptyList()) // Stats not in API
    suspend fun aiSuggestOffers(p: List<String>, s: String?) = notifyRepo.aiSuggestOffers(p, s)

    private suspend fun cargarDesdeCache() {
        presupuestos = appDao.getPresupuestos().map { it.toModel() }.toMutableList()
        tickets = appDao.getTickets().map { t -> t.toModel(appDao.getTicketProductosByTicketId(t.id).map { it.toModel() }) }.toMutableList()
        grupos = appDao.getGrupos().map { it.toModel() }.toMutableList()
        listas = appDao.getListas().map { it.toModel(appDao.getProductosByListaId(it.id).map { p -> p.toModel() }) }.toMutableList()
        recalcularMontoDisponible()
    }

    private fun inicializarDatosLocales() {
        if (presupuestos.isEmpty()) {
            presupuestos.add(Presupuesto("presupuesto-familiar", TipoPresupuesto.FAMILIAR, "Familiar", 0, 0, System.currentTimeMillis(), null, true))
            presupuestos.add(Presupuesto("presupuesto-individual", TipoPresupuesto.INDIVIDUAL, "Individual", 0, 0, System.currentTimeMillis(), null, false))
        }
    }

    private fun limpiarEstado() {
        usuarioActual = null
        limpiarEstadoExceptoUsuario()
    }

    private fun limpiarEstadoExceptoUsuario() {
        grupos.clear(); listas.clear(); tickets.clear(); presupuestos.clear()
        notifications.clear(); invitaciones.clear(); offers.clear()
    }

    // Mappings para compatibilidad local
    private fun com.undef.superahorrosanchezpucci.data.local.PresupuestoEntity.toModel() = Presupuesto(id, if(tipo=="FAMILIAR") TipoPresupuesto.FAMILIAR else TipoPresupuesto.INDIVIDUAL, nombre, montoTotal, montoDisponible, fechaInicio, fechaFin, activo)
    private fun com.undef.superahorrosanchezpucci.data.local.TicketEntity.toModel(prods: List<TicketProducto>) = Ticket(id, supermercado, direccion, fechaHora, total, try { MetodoPago.valueOf(metodoPago) } catch (_: Exception) { MetodoPago.EFECTIVO }, imagenPath, presupuestoId, prods)
    private fun com.undef.superahorrosanchezpucci.data.local.TicketProductoEntity.toModel() = TicketProducto(nombre, precio, cantidad)
    private fun com.undef.superahorrosanchezpucci.data.local.ListaCompraEntity.toModel(productos: List<Producto>) = ListaCompra(id, nombre, presupuestoId, esFamiliar, fechaCreacion, hora, supermercado, total, productos.toMutableList())
    private fun com.undef.superahorrosanchezpucci.data.local.ProductoEntity.toModel() = Producto(id, codigo, nombre, descripcion, precio, marca, precioEstimado, precioReal, cantidad, comprado, try { Categoria.valueOf(categoria) } catch (_: Exception) { Categoria.ESENCIAL })
    private fun GroupDetailResponse.toEntity() = com.undef.superahorrosanchezpucci.data.local.GrupoEntity(id, name, description, categoria ?: "FAMILIA", createdBy, "", createdAt)
    private fun com.undef.superahorrosanchezpucci.data.local.GrupoEntity.toModel() = GroupDetailResponse(id, name, description, categoria, createdBy, emptyList(), createdAt)
}
