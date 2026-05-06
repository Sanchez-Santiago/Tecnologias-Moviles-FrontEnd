package com.example.misuper.ui.screens.inicio

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.misuper.data.model.Categoria
import com.example.misuper.data.model.Presupuesto
import com.example.misuper.data.model.Usuario
import com.example.misuper.ui.theme.*
import com.example.misuper.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onNavigateToNotifications: () -> Unit,
    onNavigateToFamily: () -> Unit
) {
    val presupuestoActivo = viewModel.presupuestos.find { it.activo }
    val tickets = viewModel.tickets
        .filter { it.presupuestoId == presupuestoActivo?.id }
        .sortedByDescending { it.fechaHora }
        .take(3)

    Scaffold(
        topBar = { Header(onNavigateToNotifications) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
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
                ModeSelector(
                    activeId = presupuestoActivo?.id ?: "",
                    onModeChange = { id -> viewModel.cambiarPresupuestoActivo(id) }
                ) 
            }

            item { 
                presupuestoActivo?.let { presupuesto ->
                    val listaId = if (presupuesto.id == "presupuesto-familiar") "lista-familiar" else "lista-individual"
                    val estimados = viewModel.getEstimadosPorCategoria(listaId)
                    
                    BudgetHero(
                        presupuesto = presupuesto,
                        estimados = estimados,
                        onEditBudget = { nuevoMonto ->
                            viewModel.actualizarPresupuesto(presupuesto.id, nuevoMonto)
                        }
                    )
                }
            }

            item { 
                NewMembersSection(
                    members = viewModel.usuarios,
                    onAddClick = onNavigateToFamily
                )
            }

            item { 
                val totalGastado = viewModel.tickets.sumOf { it.total }
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
        }
    }
}

fun formatPrice(amount: Int): String {
    val formatter = java.text.DecimalFormat("$#,###.###", java.text.DecimalFormatSymbols(Locale("es", "AR")))
    return formatter.format(amount).replace(",", ".")
}

@Composable
fun Header(onNotificationsClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .height(88.dp)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "SUPER AHORRO",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                )
                Text(
                    text = "Hola, Santiago",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
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
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .align(Alignment.TopEnd)
                            .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun ModeSelector(activeId: String, onModeChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(32.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(32.dp))
            .padding(6.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            val isIndividual = activeId == "presupuesto-individual"
            SelectorItem(
                text = "INDIVIDUAL",
                isSelected = isIndividual,
                onClick = { onModeChange("presupuesto-individual") },
                modifier = Modifier.weight(1f)
            )
            SelectorItem(
                text = "FAMILIAR",
                isSelected = !isIndividual,
                onClick = { onModeChange("presupuesto-familiar") },
                modifier = Modifier.weight(1f)
            )
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
fun BudgetHero(presupuesto: Presupuesto, estimados: Map<Categoria, Int>, onEditBudget: (Int) -> Unit) {
    var showEditDialog by remember { mutableStateOf(false) }
    
    val totalMonto = if (presupuesto.montoTotal <= 0) 1 else presupuesto.montoTotal
    
    val esenTotal = estimados[Categoria.ESENCIAL] ?: 0
    val prinTotal = estimados[Categoria.PRINCIPAL] ?: 0
    val secTotal = estimados[Categoria.SECUNDARIO] ?: 0
    
    val ratioEsen = (esenTotal.toFloat() / totalMonto).coerceIn(0f, 1f)
    val ratioPrin = (prinTotal.toFloat() / totalMonto).coerceIn(0f, 1f)
    val ratioSec = (secTotal.toFloat() / totalMonto).coerceIn(0f, 1f)

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
                    color = colorScheme.outline,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx())
                )
                drawArc(
                    color = Emerald500,
                    startAngle = -90f,
                    sweepAngle = 360f * ratioEsen,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx())
                )
                drawArc(
                    color = Blue500,
                    startAngle = -90f + (360f * ratioEsen),
                    sweepAngle = 360f * ratioPrin,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx())
                )
                drawArc(
                    color = Amber500,
                    startAngle = -90f + (360f * (ratioEsen + ratioPrin)),
                    sweepAngle = 360f * ratioSec,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx())
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { showEditDialog = true }
            ) {
                Text(
                    "DISPONIBLE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Emerald500,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    formatPrice(presupuesto.montoDisponible),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = colorScheme.onBackground,
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp
                    )
                )
                Text(
                    "TOTAL: ${formatPrice(presupuesto.montoTotal)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(start = 8.dp)
        ) {
            LegendItem(Emerald500, "ESENCIAL", formatPrice(esenTotal))
            LegendItem(Blue500, "PRINCIPAL", formatPrice(prinTotal))
            LegendItem(Amber500, "SECUNDARIO", formatPrice(secTotal))
        }
    }

    if (showEditDialog) {
        var newAmount by remember { mutableStateOf(presupuesto.montoTotal.toString()) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
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
                    onEditBudget(cleaned)
                    showEditDialog = false 
                }) {
                    Text("GUARDAR", color = MaterialTheme.colorScheme.primary)
                }
            }
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
                            listOf(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f), Color.Transparent)
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
                        .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiary,
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "TIP DE AHORRO (IA)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = White.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "PRÓXIMAMENTE",
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(color = White, fontSize = 7.sp)
                        )
                    }
                }
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
