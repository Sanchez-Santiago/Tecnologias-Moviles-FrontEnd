package com.undef.superahorrosanchezpucci.ui.screens.estadisticas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.undef.superahorrosanchezpucci.data.remote.dto.MonthlySummary
import com.undef.superahorrosanchezpucci.data.remote.dto.SpendingByCategory
import com.undef.superahorrosanchezpucci.data.remote.dto.SpendingByImportance
import com.undef.superahorrosanchezpucci.data.remote.dto.SpendingByStore
import com.undef.superahorrosanchezpucci.data.remote.dto.StoreFrequency
import com.undef.superahorrosanchezpucci.data.remote.dto.MostPurchasedProduct
import com.undef.superahorrosanchezpucci.data.remote.dto.MemberSpending
import com.undef.superahorrosanchezpucci.ui.theme.Emerald500
import com.undef.superahorrosanchezpucci.ui.theme.Emerald600
import com.undef.superahorrosanchezpucci.ui.theme.Emerald700
import com.undef.superahorrosanchezpucci.viewmodel.StatisticsViewModel

private val monthNames = listOf(
    "Ene", "Feb", "Mar", "Abr", "May", "Jun",
    "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
)

@Composable
fun EstadisticasScreen(viewModel: StatisticsViewModel) {
    val spendingByCategory by viewModel.spendingByCategory.collectAsStateWithLifecycle()
    val spendingByStore by viewModel.spendingByStore.collectAsStateWithLifecycle()
    val spendingByImportance by viewModel.spendingByImportance.collectAsStateWithLifecycle()
    val monthlySummary by viewModel.monthlySummary.collectAsStateWithLifecycle()
    val storeFrequency by viewModel.storeFrequency.collectAsStateWithLifecycle()
    val mostPurchasedProducts by viewModel.mostPurchasedProducts.collectAsStateWithLifecycle()
    val memberSpending by viewModel.memberSpending.collectAsStateWithLifecycle()
    val grupos by viewModel.grupos.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val grupoId = grupos.firstOrNull()?.id
    LaunchedEffect(grupoId) {
        grupoId?.let { viewModel.loadStats(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "ESTADÍSTICAS",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
        )

        if (grupoId != null && grupos.isNotEmpty()) {
            Text(
                grupos.first().name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading && spendingByCategory.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val latest = monthlySummary.maxByOrNull { it.year * 12 + it.month }
            val total = latest?.total ?: 0.0
            val previous = monthlySummary
                .sortedBy { it.year * 12 + it.month }
                .dropLast(1)
                .lastOrNull()
            val diff = if (previous != null && previous.total > 0) {
                ((total - previous.total) / previous.total * 100).toInt()
            } else null

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val monthName = if (latest != null) "${monthNames.getOrElse(latest.month - 1) { "?" }} ${latest.year}" else ""
                    Text("Gasto Total${if (monthName.isNotBlank()) " - $monthName" else ""}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("$${String.format("%,.0f", total)}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Emerald700)
                    if (diff != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "${if (diff >= 0) "↑" else "↓"} ${kotlin.math.abs(diff)}% vs mes anterior",
                            color = if (diff >= 0) Color.Red.copy(alpha = 0.7f) else Emerald600
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (spendingByStore.isNotEmpty()) {
                val maxStore = spendingByStore.maxOfOrNull { it.total } ?: 1.0
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Gasto por Supermercado", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        spendingByStore.forEach { store ->
                            SpendingBar(store.storeName, store.total, store.percentage / 100.0)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (spendingByCategory.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Gasto por Categoría", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        spendingByCategory.forEach { cat ->
                            SpendingBar(cat.categoryName, cat.total, cat.percentage / 100.0)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (spendingByImportance.isNotEmpty()) {
                val importanceColors = mapOf(
                    "ESENCIAL" to Emerald700,
                    "PRIMARIO" to Emerald600,
                    "SECUNDARIO" to Emerald500
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Gasto por Prioridad", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        spendingByImportance.forEach { imp ->
                            val color = importanceColors[imp.importance] ?: Emerald500
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(imp.importance, color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("$${String.format("%,.0f", imp.total)} (${String.format("%.1f", imp.percentage)}%)",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            Text("${imp.purchaseCount} compras · ${imp.itemCount} productos",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { (imp.percentage / 100.0).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = color,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (storeFrequency.isNotEmpty()) {
                val maxCount = storeFrequency.maxOfOrNull { it.purchaseCount } ?: 1
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Tiendas más frecuentadas", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        storeFrequency.forEach { store ->
                            FrequencyBar(store.storeName, store.purchaseCount, store.purchaseCount.toDouble() / maxCount)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "$${String.format("%,.0f", store.totalSpent)} · ${String.format("%.1f", store.percentage)}% de compras",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (mostPurchasedProducts.isNotEmpty()) {
                val maxItems = mostPurchasedProducts.maxOfOrNull { it.count } ?: 1
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Productos más comprados", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        mostPurchasedProducts.forEach { prod ->
                            SpendingBar("${prod.productName} (x${prod.count})", prod.totalSpent, prod.count.toDouble() / maxItems)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (memberSpending.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Gasto por Miembro", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        memberSpending.forEach { member ->
                            SpendingBar(member.userName, member.totalSpent, member.percentage / 100.0)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (monthlySummary.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Evolución Mensual", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        val maxMonthly = monthlySummary.maxOfOrNull { it.total } ?: 1.0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            monthlySummary.sortedBy { it.year * 12 + it.month }.forEach { m ->
                                MonthColumn(
                                    monthNames.getOrElse(m.month - 1) { "?" },
                                    m.total,
                                    m.total / maxMonthly
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (spendingByCategory.isEmpty() && spendingByStore.isEmpty() && monthlySummary.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    Text("No hay datos de gastos aún", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SpendingBar(label: String, amount: Double, fraction: Double) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            Text("$${String.format("%,.0f", amount)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { fraction.toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = Emerald600,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun FrequencyBar(label: String, count: Int, fraction: Double) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            Text("$count compras", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { fraction.toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = Emerald600,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun MonthColumn(label: String, amount: Double, fraction: Double) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(IntrinsicSize.Min)
    ) {
        Text("$${String.format("%,.0f", amount)}", style = MaterialTheme.typography.bodySmall, color = Emerald700, fontSize = 9.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(24.dp)
                .height((fraction * 80).dp.coerceAtLeast(4.dp)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Emerald600,
                shape = MaterialTheme.shapes.extraSmall
            ) {}
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
