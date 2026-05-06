package com.example.misuper.ui.screens.estadisticas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.misuper.ui.theme.Emerald700

@Composable
fun EstadisticasScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Estadísticas",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Gasto Total por Período (Mock)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Gasto Total - Mayo 2026", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("$67.500", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Emerald700)
                Spacer(modifier = Modifier.height(8.dp))
                Text("↑ 12% vs mes anterior", color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Gasto por Supermercado (Mock)
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Gasto por Supermercado", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                GastoBarMock("Supermercado Ejemplo", 15000, 30000)
                Spacer(modifier = Modifier.height(8.dp))
                GastoBarMock("Mercado Central", 22000, 30000)
                Spacer(modifier = Modifier.height(8.dp))
                GastoBarMock("HiperMax", 18500, 30000)
                Spacer(modifier = Modifier.height(8.dp))
                GastoBarMock("Otros", 12000, 30000)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Evolución Mensual (Mock)
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Evolución Mensual", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MesColumnMock("Ene", 45000)
                    MesColumnMock("Feb", 52000)
                    MesColumnMock("Mar", 48000)
                    MesColumnMock("Abr", 60000)
                    MesColumnMock("May", 67500)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Productos más comprados (Mock)
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Productos más comprados", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                repeat(3) { index ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Producto ${index + 1}")
                        Text("${5 - index}x", fontWeight = FontWeight.Bold, color = Emerald700)
                    }
                    if (index < 2) Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun GastoBarMock(supermercado: String, monto: Int, maxMonto: Int) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(supermercado, style = MaterialTheme.typography.bodySmall)
            Text("$$monto", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { monto.toFloat() / maxMonto.toFloat() },
            modifier = Modifier.fillMaxWidth(),
            color = Emerald700
        )
    }
}

@Composable
fun MesColumnMock(mes: String, monto: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("$$monto", style = MaterialTheme.typography.bodySmall, color = Emerald700)
        Spacer(modifier = Modifier.height(4.dp))
        Text(mes, style = MaterialTheme.typography.bodySmall)
    }
}
