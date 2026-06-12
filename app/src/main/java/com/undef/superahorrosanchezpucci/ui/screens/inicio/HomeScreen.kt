package com.undef.superahorrosanchezpucci.ui.screens.inicio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.undef.superahorrosanchezpucci.R
import com.undef.superahorrosanchezpucci.data.model.Categoria
import com.undef.superahorrosanchezpucci.data.model.Presupuesto
import com.undef.superahorrosanchezpucci.data.model.Usuario
import com.undef.superahorrosanchezpucci.data.remote.dto.*
import com.undef.superahorrosanchezpucci.ui.components.HomeSkeleton
import com.undef.superahorrosanchezpucci.ui.theme.*
import com.undef.superahorrosanchezpucci.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToNotifications: () -> Unit,
    onNavigateToFamily: () -> Unit
) {
    val presupuestos by viewModel.presupuestos.collectAsStateWithLifecycle()
    val allTickets by viewModel.tickets.collectAsStateWithLifecycle()
    val listas by viewModel.listas.collectAsStateWithLifecycle()
    val usuarios by viewModel.usuarios.collectAsStateWithLifecycle()
    val grupos by viewModel.grupos.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val usuarioActual by viewModel.usuarioActual.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadCount.collectAsStateWithLifecycle()
    val isModoIndividual by viewModel.modoIndividual.collectAsStateWithLifecycle()
    val grupoActivoId by viewModel.grupoActivoId.collectAsStateWithLifecycle()
    
    // Stats for detailed view at bottom
    val spendingByStore by viewModel.spendingByStore.collectAsStateWithLifecycle()
    val mostPurchasedProducts by viewModel.mostPurchasedProducts.collectAsStateWithLifecycle()
    val memberSpending by viewModel.memberSpending.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val presupuestoActivo = presupuestos.find { it.activo }
    val tickets = allTickets
        .filter { it.presupuestoId == presupuestoActivo?.id }
        .sortedByDescending { it.fechaHora }
        .take(3)

    LaunchedEffect(grupoActivoId) {
        grupoActivoId?.let { viewModel.loadStats(it) }
    }

    // Calculate budget warning state
    val isCritical = remember(presupuestoActivo) {
        derivedStateOf {
            if (presupuestoActivo != null && presupuestoActivo.montoTotal > 0) {
                val gastado = presupuestoActivo.montoTotal - presupuestoActivo.montoDisponible
                val porcentajeGastado = (gastado.toFloat() / presupuestoActivo.montoTotal) * 100
                porcentajeGastado > 90f // Less than 10% available
            } else false
        }
    }.value

    Scaffold(
        topBar = { Header(usuarioActual?.nombre ?: "Usuario", onNavigateToNotifications, unreadCount, isCritical) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        // No usaremos Box con fillMaxSize aquí si dentro hay un LazyColumn con scroll infinito.
        // La causa del crash es medir un componente con altura infinita.
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                GroupSelector(
                    grupos = grupos,
                    selectedGroupId = grupoActivoId,
                    onGroupSelected = { grupoId -> viewModel.cambiarGrupoActivo(grupoId) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            if (isLoading) {
                // Modo loading: mostramos el selector de modo activo y el esqueleto debajo
                item {
                    ModeSelector(
                        isModoIndividual = isModoIndividual,
                        onModeChange = { individual -> 
                            viewModel.cambiarModo(individual) { result ->
                                if (result.isFailure) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(result.exceptionOrNull()?.message ?: "Error al cambiar de modo")
                                    }
                                }
                            }
                        }
                    )
                }
                item {
                    HomeSkeleton()
                }
            } else {
                item {
                    ModeSelector(
                        isModoIndividual = isModoIndividual,
                        onModeChange = { individual -> 
                            viewModel.cambiarModo(individual) { result ->
                                if (result.isFailure) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(result.exceptionOrNull()?.message ?: "Error al cambiar de modo")
                                    }
                                }
                            }
                        }
                    )
                }

                item {
                    val presupuesto = presupuestoActivo
                    val lista = listas.find { it.presupuestoId == presupuesto?.id }
                    val estimados = if (lista != null) viewModel.getEstimadosPorCategoria(lista.id) else emptyMap()

                    BudgetHero(
                        presupuesto = presupuesto,
                        estimados = estimados,
                        onEditBudget = { nuevoMonto ->
                            if (presupuesto != null) {
                                viewModel.actualizarPresupuesto(presupuesto.id, nuevoMonto)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Presupuesto actualizado")
                                }
                            }
                        }
                    )
                }

                item { 
                    NewMembersSection(
                        members = usuarios,
                        onAddClick = onNavigateToFamily
                    )
                }

                item { 
                    val totalGastado = allTickets.filter { it.presupuestoId == presupuestoActivo?.id }.sumOf { it.total }
                    AccumulatedSavingsCard(totalGastado) 
                }

                item { AITipCard() }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "ÚLTIMAS COMPRAS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Black
                            )
                        )
                        if (tickets.isEmpty()) {
                            Text("No hay compras registradas", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        } else {
                            tickets.forEach { ticket ->
                                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                PurchaseRow(
                                    ticket.supermercado, 
                                    sdf.format(Date(ticket.fechaHora)), 
                                    formatPrice(ticket.total)
                                )
                            }
                        }
                    }
                }

                // DETAILED STATISTICS SECTION
                item {
                    Text(
                        text = "ESTADÍSTICAS DETALLADAS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Black
                        )
                    )
                }

                item {
                    DetailedStatsCard("Gasto por Supermercado") {
                        Column {
                            if (spendingByStore.isEmpty()) {
                                Text("Sin datos", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                StatRow("Sin compras", "\$0", 0.0)
                            } else {
                                spendingByStore.forEach { store ->
                                    StatRow(store.storeName, formatPrice(store.total.toInt()), store.percentage / 100.0)
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    }
                }

                item {
                    val maxCount = mostPurchasedProducts.maxOfOrNull { it.count } ?: 1
                    DetailedStatsCard("Productos más comprados") {
                        Column {
                            if (mostPurchasedProducts.isEmpty()) {
                                Text("Sin datos", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                StatRow("Sin productos", "x0", 0.0)
                            } else {
                                mostPurchasedProducts.take(5).forEach { prod ->
                                    StatRow("${prod.productName} (x${prod.count})", formatPrice(prod.totalSpent.toInt()), prod.count.toDouble() / maxCount)
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    }
                }

                item {
                    DetailedStatsCard("Gasto por Miembro") {
                        Column {
                            if (memberSpending.isEmpty()) {
                                Text("Sin datos", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                StatRow("Sin miembros", "\$0", 0.0)
                            } else {
                                memberSpending.forEach { member ->
                                    StatRow(member.userName, formatPrice(member.totalSpent.toInt()), member.percentage / 100.0)
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailedStatsCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(20.dp))
            content()
        }
    }
}

@Composable
fun StatRow(label: String, value: String, fraction: Double) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { fraction.toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            color = Emerald500,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

fun formatPrice(amount: Int): String {
    val formatter = java.text.DecimalFormat("$#,###.###", java.text.DecimalFormatSymbols(Locale("es", "AR")))
    return formatter.format(amount).replace(",", ".")
}

@Composable
fun Header(userName: String, onNotificationsClick: () -> Unit, unreadCount: Int = 0, isCritical: Boolean = false) {
    val headerBrush = if (isCritical) {
        Brush.horizontalGradient(listOf(Amber500.copy(alpha = 0.15f), Rose500.copy(alpha = 0.15f)))
    } else {
        null
    }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, if (isCritical) Rose500.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .then(if (headerBrush != null) Modifier.background(headerBrush) else Modifier)
                .height(80.dp)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.splash_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = "Hola, $userName",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    IconButton(
                        onClick = onNotificationsClick,
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                                .padding(horizontal = if (unreadCount > 9) 4.dp else 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                if (unreadCount > 99) "99+" else unreadCount.toString(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ModeSelector(isModoIndividual: Boolean, onModeChange: (Boolean) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(32.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(32.dp))
            .padding(6.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            SelectorItem(
                text = "INDIVIDUAL",
                isSelected = isModoIndividual,
                onClick = { onModeChange(true) },
                modifier = Modifier.weight(1f)
            )
            SelectorItem(
                text = "GRUPO",
                isSelected = !isModoIndividual,
                onClick = { onModeChange(false) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun GroupSelector(grupos: List<GroupDetailResponse>, selectedGroupId: String?, onGroupSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val grupoActual = grupos.find { it.id == selectedGroupId } ?: grupos.firstOrNull()

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            onClick = { expanded = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Groups, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        grupoActual?.name ?: "Sin grupo",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    if (grupoActual != null) {
                        Text(
                            grupoActual.categoria ?: "GRUPO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
                Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            grupos.forEach { grupo ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(grupo.name, fontWeight = FontWeight.Bold)
                                Text(
                                    grupo.categoria ?: "GRUPO",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    },
                    onClick = {
                        onGroupSelected(grupo.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SelectorItem(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(28.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                letterSpacing = 2.sp
            )
        )
    }
}

@Composable
fun BudgetHero(presupuesto: Presupuesto?, estimados: Map<Categoria, Int>, onEditBudget: (Int) -> Unit) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }

    if (presupuesto == null) {
        EmptyBudgetHero(onEditBudget = onEditBudget)
        return
    }

    val totalMonto = if (presupuesto.montoTotal <= 0) 1 else presupuesto.montoTotal
    val esenTotal = estimados[Categoria.ESENCIAL] ?: 0
    val prinTotal = estimados[Categoria.PRINCIPAL] ?: 0
    val secTotal = estimados[Categoria.SECUNDARIO] ?: 0

    val ratioEsen = (esenTotal.toFloat() / totalMonto).coerceIn(0f, 1f)
    val ratioPrin = (prinTotal.toFloat() / totalMonto).coerceIn(0f, 1f)
    val ratioSec = (secTotal.toFloat() / totalMonto).coerceIn(0f, 1f)

    val gastado = presupuesto.montoTotal - presupuesto.montoDisponible
    val porcentajeGastado = if (presupuesto.montoTotal > 0) {
        (gastado.toFloat() / presupuesto.montoTotal) * 100
    } else 0f

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            val colorScheme = MaterialTheme.colorScheme
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = colorScheme.surfaceVariant,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx())
                )
                if (ratioEsen > 0) {
                    drawArc(
                        color = Emerald500,
                        startAngle = -90f,
                        sweepAngle = 360f * ratioEsen,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx())
                    )
                }
                if (ratioPrin > 0) {
                    drawArc(
                        color = Blue500,
                        startAngle = -90f + (360f * ratioEsen),
                        sweepAngle = 360f * ratioPrin,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx())
                    )
                }
                if (ratioSec > 0) {
                    drawArc(
                        color = Amber500,
                        startAngle = -90f + (360f * (ratioEsen + ratioPrin)),
                        sweepAngle = 360f * ratioSec,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx())
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { showDetailsDialog = true }
            ) {
                Text(
                    "DISPONIBLE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Emerald500, fontSize = 9.sp,
                        fontWeight = FontWeight.Black, letterSpacing = 1.sp
                    )
                )
                Text(
                    formatPrice(presupuesto.montoDisponible),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = colorScheme.onBackground, fontWeight = FontWeight.Black, fontSize = 28.sp
                    )
                )
                Text(
                    "TOTAL: ${formatPrice(presupuesto.montoTotal)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold
                    )
                )
            }

            if (showDetailsDialog) {
                AlertDialog(
                    onDismissRequest = { showDetailsDialog = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    title = { Text(stringResource(R.string.budget_details_title), color = MaterialTheme.colorScheme.onSurface) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.total_label), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatPrice(presupuesto.montoTotal), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.spent_label), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatPrice(gastado), fontWeight = FontWeight.Bold, color = Emerald500)
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Disponible:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatPrice(presupuesto.montoDisponible), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Porcentaje gastado:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${String.format("%.1f", porcentajeGastado)}%", fontWeight = FontWeight.Bold, color = if (porcentajeGastado > 90) Rose500 else Emerald500)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showDetailsDialog = false; showEditDialog = true
                        }) { Text("EDITAR", color = MaterialTheme.colorScheme.primary) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDetailsDialog = false }) { Text("CERRAR", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(start = 8.dp)
        ) {
            if (ratioEsen + ratioPrin + ratioSec == 0f) {
                Text("Sin datos", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            } else {
                LegendItem(Emerald500, "ESENCIAL", formatPrice(esenTotal))
                LegendItem(Blue500, "PRINCIPAL", formatPrice(prinTotal))
                LegendItem(Amber500, "SECUNDARIO", formatPrice(secTotal))
            }
        }
    }

    if (showEditDialog) {
        EditBudgetDialog(
            currentTotal = presupuesto.montoTotal,
            onSave = onEditBudget,
            onDismiss = { showEditDialog = false }
        )
    }
}

@Composable
fun LegendItem(color: Color, label: String, value: String) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        Text(
            value,
            style = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        )
    }
}

@Composable
fun AccumulatedSavingsCard(total: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(40.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Simulated gradient glow in top right
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.TopEnd)
                    .background(
                        Brush.radialGradient(
                            listOf(Emerald500.copy(alpha = 0.15f), Color.Transparent)
                        )
                    )
            )

            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Emerald600, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Gasto Total",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    )
                    Text(
                        formatPrice(total),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 30.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun AITipCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(40.dp),
        colors = CardDefaults.cardColors(containerColor = Emerald600)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    "TIP DE AHORRO (IA)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = White.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                )
                Text(
                    "Estás gastando un 12% menos que el mes pasado en lácteos.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                )
            }
        }
    }
}

@Composable
fun PurchaseRow(supermarket: String, date: String, amount: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Store, contentDescription = null, tint = Emerald500)
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                supermarket,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            )
            Text(
                date,
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            )
        }

        Text(
            amount,
            style = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        )
    }
}

@Composable
fun EmptyBudgetHero(onEditBudget: (Int) -> Unit) {
    var showEditDialog by remember { mutableStateOf(false) }
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = surfaceVariant,
                        startAngle = 0f, sweepAngle = 360f,
                        useCenter = false, style = Stroke(width = 10.dp.toPx())
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SIN", style = MaterialTheme.typography.labelSmall.copy(
                        color = onSurfaceVariant, fontSize = 9.sp,
                        fontWeight = FontWeight.Black, letterSpacing = 1.sp
                    ))
                    Text("SALDO", style = MaterialTheme.typography.labelSmall.copy(
                        color = onSurfaceVariant, fontSize = 9.sp,
                        fontWeight = FontWeight.Black, letterSpacing = 1.sp
                    ))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { showEditDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp)
            ) { Text("EDITAR SALDO") }
        }
    }

    if (showEditDialog) {
        EditBudgetDialog(
            currentTotal = 0,
            onSave = onEditBudget,
            onDismiss = { showEditDialog = false }
        )
    }
}

@Composable
fun EditBudgetDialog(currentTotal: Int, onSave: (Int) -> Unit, onDismiss: () -> Unit) {
    var newAmount by remember { mutableStateOf(currentTotal.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Editar Presupuesto", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            TextField(
                value = newAmount,
                onValueChange = { newAmount = it },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val cleaned = newAmount.filter { it.isDigit() }.toIntOrNull() ?: 0
                onSave(cleaned)
            }) { Text("GUARDAR", color = MaterialTheme.colorScheme.primary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCELAR", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    )
}
