package com.undef.superahorrosanchezpucci.data.repository

import android.app.Application
import android.util.Log
import com.undef.superahorrosanchezpucci.data.local.*
import com.undef.superahorrosanchezpucci.data.model.*
import com.undef.superahorrosanchezpucci.data.remote.AuthSessionStore
import com.undef.superahorrosanchezpucci.data.remote.RetrofitClient
import com.undef.superahorrosanchezpucci.data.remote.dto.*
import com.undef.superahorrosanchezpucci.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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

    // Estado en memoria para UI
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
    private var isCreatingIndividualGroup = false
    
    var onDataChanged: (() -> Unit)? = null

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
            // 1. Cargar preferencias y IDs de grupos previos
            themeMode = preferencesDataStore.themeModeFlow.first()
            grupoIndividualId = preferencesDataStore.individualGroupIdFlow.first()
            grupoRegularActivoId = preferencesDataStore.activeGroupIdFlow.first()

            // 2. Cargar lo que tengamos en caché de inmediato para UI instantánea
            if (tickets.isEmpty() && presupuestos.isEmpty()) {
                cargarDesdeCache()
                withContext(Dispatchers.Main) { onDataChanged?.invoke() }
            }
            
            if (AuthSessionStore.accessToken.isNullOrBlank()) {
                if (presupuestos.isEmpty()) inicializarDatosLocales()
                isLoadingData = false
                return@withContext
            }

            // 3. Cargar perfil
            usuarioActual = userRepo.fetchProfile()
            
            // 4. Catálogo de productos y tiendas (en paralelo)
            coroutineScope {
                launch {
                    val result = productRepo.fetchProducts()
                    productNameToId = result.first
                    barcodeToId = result.second
                }
                launch {
                    storeNameToId = productRepo.fetchStores()
                }
            }
            
            // 5. Asegurar que existan grupos y presupuestos
            ensureGroupAndBudget()
            
            // 6. Cargar datos específicos del grupo activo
            val targetId = if (modoIndividual) grupoIndividualId else (grupoRegularActivoId ?: gruposVisibles.firstOrNull()?.id)
            if (targetId != null) {
                // Guardar los IDs detectados para la próxima vez
                preferencesDataStore.saveGroupIds(grupoIndividualId, grupoRegularActivoId)

                // Cargar desde caché para este grupo específico antes de ir a la API
                cargarDesdeCache(targetId)
                withContext(Dispatchers.Main) { onDataChanged?.invoke() }

                // Sincronizar con la API
                coroutineScope {
                    val budgetListDef = async { budgetRepo.getBudgets(targetId, modoIndividual) }
                    val nDef = async { notifyRepo.getNotifications() }
                    val oDef = async { notifyRepo.getOffers() }
                    val iDef = async { groupRepo.getInvitations() }
                    
                    val budgetList = budgetListDef.await()
                    presupuestos = budgetList.toMutableList()
                    val activeBudget = budgetList.find { it.activo } ?: budgetList.firstOrNull()
                    val currentBudgetId = activeBudget?.id ?: ""
                    
                    val pDef = async { purchaseRepo.getPurchases(targetId, currentBudgetId) }
                    val lDef = async { listRepo.getShoppingLists(targetId, currentBudgetId, modoIndividual) }
                    
                    val apiTickets = pDef.await()
                    fusionarTickets(apiTickets, targetId)
                    
                    listas = lDef.await().toMutableList()
                    notifications = nDef.await().toMutableList()
                    offers = oDef.await().toMutableList()
                    invitaciones = iDef.await().toMutableList()
                }
            }
            
            recalcularMontoDisponible()
            withContext(Dispatchers.Main) { onDataChanged?.invoke() }
        } catch (e: Exception) {
            Log.e("AppRepository", "Error en cargarTodo: ${e.message}", e)
        } finally {
            isLoadingData = false
        }
    }

    private fun fusionarTickets(apiTickets: List<Ticket>, currentGroupId: String) {
        // Mantenemos tickets locales no sincronizados (id temporal) que pertenezcan al grupo
        val unsynced = tickets.filter { it.id.startsWith("temp-") && it.groupId == currentGroupId }
        val apiIds = apiTickets.map { it.id }.toSet()
        val uniqueUnsynced = unsynced.filter { it.id !in apiIds }
        
        // Combinar y ordenar por fecha descendente
        tickets = (apiTickets + uniqueUnsynced).sortedByDescending { it.fechaHora }.toMutableList()
    }

    private suspend fun ensureGroupAndBudget() {
        Log.d("AppRepository", "Asegurando grupos y presupuestos... LoggedIn: ${isLoggedIn()}")
        grupos = groupRepo.getGroups().toMutableList()
        detectarGrupoIndividual()

        // Crear grupo individual si no existe
        if (grupoIndividualId == null && isLoggedIn() && !isCreatingIndividualGroup) {
            val groupName = "${usuarioActual?.nombre ?: "Usuario"} (Individual)"
            Log.d("AppRepository", "Intentando crear grupo individual: $groupName")
            isCreatingIndividualGroup = true
            try {
                groupRepo.createGroup(groupName, "INDIVIDUAL") // Cambiado a INDIVIDUAL
                grupos = groupRepo.getGroups().toMutableList()
                detectarGrupoIndividual()
            } finally {
                isCreatingIndividualGroup = false
            }
        }

        if (gruposVisibles.isEmpty() && isLoggedIn()) {
            Log.d("AppRepository", "No hay grupos visibles, creando 'Mi Familia'")
            groupRepo.createGroup("Mi Familia", "FAMILIA")
            grupos = groupRepo.getGroups().toMutableList()
        }

        if (grupoRegularActivoId == null && gruposVisibles.isNotEmpty()) {
            grupoRegularActivoId = gruposVisibles.first().id
        }

        Log.d("AppRepository", "IDs - Individual: $grupoIndividualId, Regular: $grupoRegularActivoId, ModoIndividual: $modoIndividual")

        // Migración de datos locales "huérfanos" (sin groupId) al grupo actual
        val targetId = if (modoIndividual) grupoIndividualId else (grupoRegularActivoId ?: gruposVisibles.firstOrNull()?.id)
        if (targetId != null) {
            appDao.migrateTicketsToGroup(targetId)
            appDao.migratePresupuestosToGroup(targetId)
            appDao.migrateListasToGroup(targetId)
            
            // Asegurar que el grupo tenga al menos un presupuesto
            try {
                val resp = apiService.getBudgets(targetId)
                if (!resp.isSuccessful || resp.body()?.data.isNullOrEmpty()) {
                    Log.d("AppRepository", "Creando presupuesto por defecto para grupo $targetId")
                    budgetRepo.createDefaultBudget(targetId)
                }
            } catch (_: Exception) {
                budgetRepo.createDefaultBudget(targetId)
            }
        }
    }

    private fun detectarGrupoIndividual() {
        val userId = AuthSessionStore.userId ?: return
        // Buscamos un grupo que diga "Individual" en el nombre O en la categoría
        val individual = grupos.find { 
            (it.name.contains("(Individual)", ignoreCase = true) || it.categoria == "INDIVIDUAL") 
            && it.createdBy == userId
        }
        
        if (individual != null) {
            grupoIndividualId = individual.id
            Log.d("AppRepository", "Grupo individual detectado: ${individual.name} (${individual.id})")
        } else {
            Log.w("AppRepository", "No se encontró grupo individual para el usuario $userId")
        }
    }

    private fun recalcularMontoDisponible() {
        presupuestos = budgetRepo.calculateAvailable(presupuestos, tickets).toMutableList()
    }

    // ======================
    // MÉTODOS DELEGADOS
    // ======================

    suspend fun login(e: String, p: String) = userRepo.login(e, p).onSuccess { usuarioActual = it; cargarTodo() }
    suspend fun register(n: String, e: String, p: String) = userRepo.register(n, e, p)
    suspend fun logout() { userRepo.logout(); limpiarEstado() }
    fun isLoggedIn() = AuthSessionStore.accessToken != null

    suspend fun crearGrupo(n: String, c: String) = groupRepo.createGroup(n, c).onSuccess { cargarTodo() }
    
    suspend fun agregarLista(l: ListaCompra): String {
        val currentGroupId = (if(modoIndividual) grupoIndividualId else grupoRegularActivoId) ?: ""
        Log.d("AppRepository", "Agregando lista: ${l.nombre}, Grupo: $currentGroupId")
        
        val apiId = if (isLoggedIn() && currentGroupId.isNotBlank()) {
            Log.d("AppRepository", "Llamando a createList en API...")
            listRepo.createList(currentGroupId, l.nombre)
        } else null
        
        val finalL = if (apiId != null) {
            Log.d("AppRepository", "Lista creada en API con ID: $apiId")
            l.copy(id = apiId, groupId = currentGroupId)
        } else {
            l.copy(groupId = currentGroupId)
        }
        
        if (listas.none { it.id == finalL.id }) listas.add(finalL)
        listRepo.localSaveList(finalL)
        return finalL.id
    }

    suspend fun agregarOActualizarProducto(listaId: String, p: Producto) {
        val index = listas.indexOfFirst { it.id == listaId }
        Log.d("AppRepository", "agregarOActualizarProducto - Lista: $listaId, Producto: ${p.nombre}")
        if (index != -1) {
            val isNew = listas[index].productos.none { it.id == p.id }
            val updatedProds = listas[index].productos.toMutableList().apply { 
                val i = indexOfFirst { it.id == p.id }
                if (i != -1) set(i, p) else add(p)
            }
            listas[index] = listas[index].copy(productos = updatedProds)
            listRepo.localSaveProducts(listaId, updatedProds)
            
            val cleanName = p.nombre.lowercase().trim()
            val catalogId = barcodeToId[p.codigo] 
                ?: productNameToId[cleanName]
                ?: productNameToId.entries.find { it.key.startsWith(cleanName) || cleanName.startsWith(it.key) }?.value

            Log.d("AppRepository", "Sincronizando producto con API... CatalogId: $catalogId, IsLoggedIn: ${isLoggedIn()}")
            listRepo.syncProduct(listaId, p, isNew, catalogId)
            withContext(Dispatchers.Main) { onDataChanged?.invoke() }
        } else {
            Log.e("AppRepository", "No se encontró la lista $listaId para agregar el producto")
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
        val currentGroupId = (if(modoIndividual) grupoIndividualId else grupoRegularActivoId) ?: ""
        val activo = presupuestos.find { it.activo } ?: presupuestos.firstOrNull()
        val budgetId = activo?.id ?: ""
        
        val vinculado = t.copy(
            presupuestoId = if (t.presupuestoId.isBlank()) budgetId else t.presupuestoId,
            groupId = if (t.groupId.isBlank()) currentGroupId else t.groupId
        )
        
        val finalTicket = if (vinculado.id.isBlank() || vinculado.id.length < 5) 
            vinculado.copy(id = "temp-${System.currentTimeMillis()}") else vinculado
        
        Log.d("AppRepository", "Agregando ticket: ${finalTicket.supermercado}, total: ${finalTicket.total}, grupo: ${finalTicket.groupId}, presupuesto: ${finalTicket.presupuestoId}")

        // 1. Guardar localmente de inmediato para UI instantánea
        synchronized(tickets) {
            val existingIdx = tickets.indexOfFirst { it.id == finalTicket.id }
            if (existingIdx != -1) tickets[existingIdx] = finalTicket else tickets.add(0, finalTicket)
        }
        
        purchaseRepo.localSaveSingleTicket(finalTicket)
        recalcularMontoDisponible()
        withContext(Dispatchers.Main) { onDataChanged?.invoke() }
        
        // 2. Sincronizar con API en segundo plano
        if (isLoggedIn() && !finalTicket.presupuestoId.startsWith("presupuesto-")) {
            val items = finalTicket.productos.mapNotNull { tp ->
                val cleanName = tp.nombre.lowercase().trim()
                val pid = productNameToId[cleanName]
                    ?: productNameToId.entries.find { it.key.startsWith(cleanName) || cleanName.startsWith(it.key) }?.value
                if (pid != null) CreatePurchaseItem(pid, tp.cantidad) else null
            }
            
            val cleanStoreName = finalTicket.supermercado.lowercase().trim()
            val sid = storeNameToId[cleanStoreName]
                ?: storeNameToId.entries.find { it.key.startsWith(cleanStoreName) || cleanStoreName.startsWith(it.key) }?.value

            if (finalTicket.groupId.isNotBlank()) {
                try {
                    Log.d("AppRepository", "Sincronizando con API... items: ${items.size}")
                    val apiTicket = purchaseRepo.createPurchase(finalTicket.groupId, sid, finalTicket.supermercado, items, finalTicket.presupuestoId)
                    if (apiTicket != null) {
                        Log.d("AppRepository", "Ticket sincronizado con éxito. Nuevo ID: ${apiTicket.id}")
                        // Reemplazar el temporal con el real en memoria
                        synchronized(tickets) {
                            val idx = tickets.indexOfFirst { it.id == finalTicket.id }
                            if (idx != -1) tickets[idx] = apiTicket else tickets.add(0, apiTicket)
                        }
                        
                        // Limpiar el temporal de Room
                        purchaseRepo.deletePurchase(finalTicket.id) 
                        
                        recalcularMontoDisponible()
                        withContext(Dispatchers.Main) { onDataChanged?.invoke() }
                    } else {
                        Log.e("AppRepository", "La API no devolvió un ticket válido")
                    }
                } catch (e: Exception) {
                    Log.e("AppRepository", "Error al sincronizar ticket con API: ${e.message}", e)
                }
            }
        }
    }

    suspend fun eliminarTicket(id: String) {
        tickets.removeAll { it.id == id }
        purchaseRepo.deletePurchase(id)
        recalcularMontoDisponible()
        withContext(Dispatchers.Main) { onDataChanged?.invoke() }
    }

    suspend fun actualizarTicket(t: Ticket) {
        val i = tickets.indexOfFirst { it.id == t.id }
        if (i != -1) {
            tickets[i] = t
            purchaseRepo.updatePurchase(t)
            recalcularMontoDisponible()
            withContext(Dispatchers.Main) { onDataChanged?.invoke() }
        }
    }

    suspend fun switchModo(individual: Boolean): Result<Unit> {
        modoIndividual = individual
        limpiarEstadoExceptoUsuario()
        cargarTodo()
        return Result.success(Unit)
    }

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

    suspend fun agregarUsuario(u: Usuario) {
        if (usuarios.none { it.id == u.id }) {
            usuarios.add(u)
        }
    }

    suspend fun actualizarUsuario(u: Usuario) {
        userRepo.updateProfile(u.nombre, u.email)?.let { updated ->
            usuarioActual = updated
            val i = usuarios.indexOfFirst { it.id == updated.id }
            if (i != -1) usuarios[i] = updated
        }
    }
    suspend fun cambiarPassword(c: String, n: String) = userRepo.changePassword(c, n)

    suspend fun cambiarGrupoActivo(gid: String) {
        grupoRegularActivoId = gid
        limpiarEstadoExceptoUsuario()
        cargarTodo()
    }

    suspend fun loadBudgetProgress(gid: String) = statsRepo.getBudgetProgress(gid)
    suspend fun loadSpendingByCategory(gid: String) = statsRepo.getSpendingByCategory(gid)
    suspend fun loadSpendingByStore(gid: String) = statsRepo.getSpendingByStore(gid)
    suspend fun loadSpendingByImportance(gid: String) = statsRepo.getSpendingByImportance(gid)
    suspend fun loadStoreFrequency(gid: String) = statsRepo.getMostFrequentStore(gid)
    suspend fun loadMonthlySummary(gid: String) = statsRepo.getMonthlySummary(gid)
    suspend fun loadMostPurchasedProducts(gid: String) = statsRepo.getMostPurchasedProducts(gid)
    suspend fun loadMemberSpending(gid: String) = statsRepo.getMemberSpending(gid)
    suspend fun aiSuggestOffers(p: List<String>, s: String?) = notifyRepo.aiSuggestOffers(p, s)

    suspend fun buscarEnCatalogo(query: String) = appDao.searchCatalogo(query)

    private suspend fun cargarDesdeCache(specificGroupId: String? = null) {
        val currentGroupId = specificGroupId ?: (if(modoIndividual) grupoIndividualId else grupoRegularActivoId) ?: ""
        
        // Solo mostramos datos con groupId "" si no estamos en ningún grupo (offline sin login)
        val filter: (String) -> Boolean = { gid ->
            if (currentGroupId.isEmpty()) gid.isEmpty() else gid == currentGroupId
        }

        presupuestos = appDao.getPresupuestos().filter { filter(it.groupId) }.map { it.toModel() }.toMutableList()
        tickets = appDao.getTickets().filter { filter(it.groupId) }.map { t -> 
            t.toModel(appDao.getTicketProductosByTicketId(t.id).map { it.toModel() }) 
        }.toMutableList()
        listas = appDao.getListas().filter { filter(it.groupId) }.map { it.toModel(
            appDao.getProductosByListaId(it.id).map { p -> p.toModel() }
        ) }.toMutableList()
        
        grupos = appDao.getGrupos().map { it.toModel() }.toMutableList()
        recalcularMontoDisponible()
    }

    private fun inicializarDatosLocales() {
        if (presupuestos.isEmpty()) {
            presupuestos.add(Presupuesto("presupuesto-familiar", "", TipoPresupuesto.FAMILIAR, "Familiar", 0, 0, System.currentTimeMillis(), null, true))
            presupuestos.add(Presupuesto("presupuesto-individual", "", TipoPresupuesto.INDIVIDUAL, "Individual", 0, 0, System.currentTimeMillis(), null, false))
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

    private fun GroupDetailResponse.toEntity() = com.undef.superahorrosanchezpucci.data.local.GrupoEntity(id, name, description, categoria ?: "FAMILIA", createdBy, "", createdAt)
    private fun com.undef.superahorrosanchezpucci.data.local.GrupoEntity.toModel() = GroupDetailResponse(id, name, description, categoria, createdBy, emptyList(), createdAt)
}
