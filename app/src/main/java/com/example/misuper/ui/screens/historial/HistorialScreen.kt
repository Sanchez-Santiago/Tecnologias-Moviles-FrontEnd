package com.example.misuper.ui.screens.historial

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.misuper.ui.theme.Emerald700

data class CompraMock(
    val id: String,
    val fecha: String,
    val supermercado: String,
    val total: Int
)

@Composable
fun HistorialScreen(navController: NavController) {
    val comprasMock = listOf(
        CompraMock("1", "15/05/2026", "Supermercado Ejemplo", 15000),
        CompraMock("2", "10/05/2026", "Mercado Central", 22000),
        CompraMock("3", "05/05/2026", "HiperMax", 18500),
        CompraMock("4", "01/05/2026", "Supermercado Ejemplo", 12000)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Historial de Compras",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(comprasMock) { compra ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { navController.navigate("DETALLE_COMPRA/${compra.id}") }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(compra.supermercado, fontWeight = FontWeight.Medium)
                            Text(compra.fecha, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Text("$${compra.total}", fontWeight = FontWeight.Bold, color = Emerald700)
                    }
                }
            }
        }
    }
}
