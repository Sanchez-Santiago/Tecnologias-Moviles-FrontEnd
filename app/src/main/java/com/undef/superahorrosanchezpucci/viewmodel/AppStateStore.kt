package com.undef.superahorrosanchezpucci.viewmodel

import android.app.Application
import com.undef.superahorrosanchezpucci.data.model.Categoria
import com.undef.superahorrosanchezpucci.data.model.ListaCompra
import com.undef.superahorrosanchezpucci.data.model.Presupuesto
import com.undef.superahorrosanchezpucci.data.model.Producto
import com.undef.superahorrosanchezpucci.data.model.Ticket
import com.undef.superahorrosanchezpucci.data.model.TicketImageAnalysis
import com.undef.superahorrosanchezpucci.data.model.Usuario
import com.undef.superahorrosanchezpucci.data.remote.dto.GroupDetailResponse
import com.undef.superahorrosanchezpucci.data.remote.dto.InvitationResponse
import com.undef.superahorrosanchezpucci.data.remote.dto.MonthlySummary
import com.undef.superahorrosanchezpucci.data.remote.dto.NotificationResponse
import com.undef.superahorrosanchezpucci.data.remote.dto.AiOfferSuggestion
import com.undef.superahorrosanchezpucci.data.remote.dto.OfferResponse
import com.undef.superahorrosanchezpucci.data.remote.dto.SpendingByCategory
import com.undef.superahorrosanchezpucci.data.remote.dto.SpendingByImportance
import com.undef.superahorrosanchezpucci.data.remote.dto.SpendingByStore
import com.undef.superahorrosanchezpucci.data.remote.dto.StoreFrequency
import com.undef.superahorrosanchezpucci.data.remote.dto.MostPurchasedProduct
import com.undef.superahorrosanchezpucci.data.remote.dto.MemberSpending
import com.undef.superahorrosanchezpucci.data.remote.dto.BudgetProgress
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
import kotlinx.coroutines.withContext

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

    private val _grupos = MutableStateFlow<List<GroupDetailResponse>>(emptyList())
    val grupos: StateFlow<List<GroupDetailResponse>> = _grupos.asStateFlow()

    private val _invitaciones = MutableStateFlow<List<InvitationResponse>>(emptyList())
    val invitaciones: StateFlow<List<InvitationResponse>> = _invitaciones.asStateFlow()

    private val _spendingByCategory = MutableStateFlow<List<SpendingByCategory>>(emptyList())
    val spendingByCategory: StateFlow<List<SpendingByCategory>> = _spendingByCategory.asStateFlow()

    private val _spendingByStore = MutableStateFlow<List<SpendingByStore>>(emptyList())
    val spendingByStore: StateFlow<List<SpendingByStore>> = _spendingByStore.asStateFlow()

    private val _spendingByImportance = MutableStateFlow<List<SpendingByImportance>>(emptyList())
    val spendingByImportance: StateFlow<List<SpendingByImportance>> = _spendingByImportance.asStateFlow()

    private val _monthlySummary = MutableStateFlow<List<MonthlySummary>>(emptyList())
    val monthlySummary: StateFlow<List<MonthlySummary>> = _monthlySummary.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationResponse>>(emptyList())
    val notifications: StateFlow<List<NotificationResponse>> = _notifications.asStateFlow()

    private val _offers = MutableStateFlow<List<OfferResponse>>(emptyList())
    val offers: StateFlow<List<OfferResponse>> = _offers.asStateFlow()

    private val _storeFrequency = MutableStateFlow<List<StoreFrequency>>(emptyList())
    val storeFrequency: StateFlow<List<StoreFrequency>> = _storeFrequency.asStateFlow()

    private val _mostPurchasedProducts = MutableStateFlow<List<MostPurchasedProduct>>(emptyList())
    val mostPurchasedProducts: StateFlow<List<MostPurchasedProduct>> = _mostPurchasedProducts.asStateFlow()

    private val _memberSpending = MutableStateFlow<List<MemberSpending>>(emptyList())
    val memberSpending: StateFlow<List<MemberSpending>> = _memberSpending.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _modoIndividual = MutableStateFlow(false)
    val modoIndividual: StateFlow<Boolean> = _modoIndividual.asStateFlow()

    private val _grupoActivoId = MutableStateFlow<String?>(null)
    val grupoActivoId: StateFlow<String?> = _grupoActivoId.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        AuthSessionStore.initialize(application.applicationContext)
        reload()
    }

    fun reload() {
        scope.launch {
            _isLoading.value = true
            // repository.cargarTodo() ya carga el usuario, grupos, presupuestos, compras, listas, etc.
            runCatching { repository.cargarTodo() }
            runCatching { repository.loadUnreadCount() }
            refrescar()
            _themeMode.value = repository.themeMode
            _isLoading.value = false
        }
    }

    fun login(email: String, password: String, onResult: (Result<Usuario>) -> Unit) {
        scope.launch {
            _isLoading.value = true
            val result = withContext(Dispatchers.IO) { repository.login(email, password) }
            result.onSuccess { 
                // Tras login exitoso, refrescar todo
                repository.cargarTodo()
                refrescar() 
            }
            _isLoading.value = false
            onResult(result)
        }
    }

    fun register(name: String, email: String, password: String, onResult: (Result<Unit>) -> Unit) {
        scope.launch {
            val result = withContext(Dispatchers.IO) { repository.register(name, email, password) }
            onResult(result)
        }
    }

    fun agregarProducto(listaId: String, producto: Producto) {
        scope.launch {
            // Si el ID es uno de los hardcodeados de la UI, intentamos mapearlo a la lista real del repositorio
            val finalListaId = if (listaId == "lista-familiar" || listaId == "lista-individual") {
                val esFamiliar = listaId == "lista-familiar"
                repository.listas.find { it.esFamiliar == esFamiliar }?.id ?: listaId
            } else {
                listaId
            }

            var effectiveId = finalListaId
            val listaExiste = repository.listas.any { it.id == finalListaId }
            if (!listaExiste) {
                val nombreLista = if (listaId == "lista-familiar") "Lista Familiar" else "Lista Individual"
                val esFamiliar = listaId == "lista-familiar"
                effectiveId = repository.agregarLista(ListaCompra(id = finalListaId, nombre = nombreLista, esFamiliar = esFamiliar))
                refrescar()
            }

            repository.agregarOActualizarProducto(effectiveId, producto)
            refrescar()
        }
    }

    fun eliminarProducto(listaId: String, productoId: String) {
        scope.launch {
            val finalId = if (listaId == "lista-familiar" || listaId == "lista-individual") {
                val esFamiliar = listaId == "lista-familiar"
                repository.listas.find { it.esFamiliar == esFamiliar }?.id ?: listaId
            } else listaId
            repository.eliminarProducto(finalId, productoId)
            refrescar()
        }
    }

    fun toggleProducto(listaId: String, productoId: String) {
        scope.launch {
            val finalId = if (listaId == "lista-familiar" || listaId == "lista-individual") {
                val esFamiliar = listaId == "lista-familiar"
                repository.listas.find { it.esFamiliar == esFamiliar }?.id ?: listaId
            } else listaId
            repository.toggleProducto(finalId, productoId)
            refrescar()
        }
    }

    fun actualizarPresupuesto(id: String, monto: Int) {
        scope.launch {
            // 1. Actualización local rápida
            repository.actualizarPresupuesto(id, monto)
            refrescar() 
            
            // 2. Sincronización asíncrona con la API
            launch { repository.sincronizarPresupuesto(id) }
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

    fun actualizarUsuario(usuario: Usuario) {
        scope.launch {
            repository.actualizarUsuario(usuario)
            refrescar()
        }
    }

    fun aceptarInvitacion(token: String) {
        scope.launch {
            val result = repository.acceptInvitation(token)
            result.onSuccess { reload() }
        }
    }

    fun rechazarInvitacion(token: String) {
        scope.launch {
            val result = repository.rejectInvitation(token)
            result.onSuccess { reload() }
        }
    }

    fun invitarMiembro(groupId: String, email: String, onResult: (Result<Unit>) -> Unit) {
        scope.launch {
            val result = repository.inviteMember(groupId, email)
            result.onSuccess { reload() }
            onResult(result)
        }
    }

    suspend fun cambiarPassword(currentPassword: String, newPassword: String): Result<String> {
        return withContext(Dispatchers.IO) { repository.cambiarPassword(currentPassword, newPassword) }
    }

    fun loadNotifications() {
        scope.launch {
            repository.loadNotifications()
            _notifications.value = repository.notifications.toList()
        }
    }

    fun loadUnreadCount() {
        scope.launch {
            repository.loadUnreadCount()
            _unreadCount.value = repository.unreadCount
        }
    }

    fun markNotificationRead(id: String) {
        scope.launch {
            repository.markNotificationRead(id)
            _notifications.value = repository.notifications.toList()
            _unreadCount.value = repository.unreadCount
        }
    }

    fun markAllNotificationsRead() {
        scope.launch {
            repository.markAllNotificationsRead()
            _notifications.value = repository.notifications.toList()
            _unreadCount.value = repository.unreadCount
        }
    }

    fun deleteNotification(id: String) {
        scope.launch {
            repository.deleteNotification(id)
            _notifications.value = repository.notifications.toList()
        }
    }

    fun crearGrupo(nombre: String, categoria: String, onResult: (Result<Unit>) -> Unit) {
        scope.launch {
            _isLoading.value = true
            val result = repository.crearGrupo(nombre, categoria)
            refrescar()
            _isLoading.value = false
            onResult(result)
        }
    }

    fun cambiarGrupoActivo(grupoId: String) {
        scope.launch {
            _isLoading.value = true
            repository.cambiarGrupoActivo(grupoId)
            refrescar()
            _isLoading.value = false
        }
    }

    fun cambiarModo(individual: Boolean, onResult: ((Result<Unit>) -> Unit)? = null) {
        scope.launch {
            _isLoading.value = true
            val result = repository.switchModo(individual)
            refrescar()
            _isLoading.value = false
            onResult?.invoke(result)
        }
    }

    fun loadStats(grupoId: String) {
        scope.launch {
            repository.loadSpendingByCategory(grupoId).onSuccess { _spendingByCategory.value = it }
            repository.loadSpendingByStore(grupoId).onSuccess { _spendingByStore.value = it }
            repository.loadMonthlySummary(grupoId).onSuccess { _monthlySummary.value = it }
            repository.loadSpendingByImportance(grupoId).onSuccess { _spendingByImportance.value = it }
            repository.loadStoreFrequency(grupoId).onSuccess { _storeFrequency.value = it }
            repository.loadMostPurchasedProducts(grupoId).onSuccess { _mostPurchasedProducts.value = it }
            repository.loadMemberSpending(grupoId).onSuccess { _memberSpending.value = it }
        }
    }

    fun loadActiveOffers() {
        scope.launch {
            repository.loadActiveOffers()
            _offers.value = repository.offers.toList()
        }
    }

    fun loadBudgetProgress(groupId: String, onResult: (Result<List<BudgetProgress>>) -> Unit) {
        scope.launch {
            val result = repository.loadBudgetProgress(groupId)
            onResult(result)
        }
    }

    fun logout() {
        scope.launch {
            repository.logout()
            _presupuestos.value = emptyList()
            _listas.value = emptyList()
            _tickets.value = emptyList()
            _usuarios.value = emptyList()
            _grupos.value = emptyList()
            _invitaciones.value = emptyList()
            _spendingByCategory.value = emptyList()
            _spendingByStore.value = emptyList()
            _spendingByImportance.value = emptyList()
            _monthlySummary.value = emptyList()
            _storeFrequency.value = emptyList()
            _notifications.value = emptyList()
            _unreadCount.value = 0
            _offers.value = emptyList()
            _usuarioActual.value = null
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        scope.launch {
            repository.updateThemeMode(mode)
        }
    }

    suspend fun aiSuggestOffers(productNames: List<String>, storeId: String? = null): Result<List<AiOfferSuggestion>> {
        return repository.aiSuggestOffers(productNames, storeId)
    }

    suspend fun analizarTicketImagen(imageBytes: ByteArray, mimeType: String): TicketImageAnalysis {
        return repository.analizarTicketImagen(imageBytes, mimeType)
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
        _grupos.value = repository.gruposVisibles.toList()
        _invitaciones.value = repository.invitaciones.toList()
        _notifications.value = repository.notifications.toList()
        _unreadCount.value = repository.unreadCount
        _offers.value = repository.offers.toList()
        _modoIndividual.value = repository.modoIndividual
        _grupoActivoId.value = if (repository.modoIndividual) repository.grupoIndividualId else repository.grupoRegularActivoId
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
