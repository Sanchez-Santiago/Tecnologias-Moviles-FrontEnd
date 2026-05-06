package com.example.misuper.ui.screens.compra

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.misuper.ui.theme.Emerald700

@Composable
fun DetalleCompraScreen(navController: NavController, compraId: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Detalle de Compra",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Fecha:", fontWeight = FontWeight.Medium)
                    Text("15/05/2026")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Hora:", fontWeight = FontWeight.Medium)
                    Text("14:30")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Supermercado:", fontWeight = FontWeight.Medium)
                    Text("Supermercado Ejemplo")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Productos",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Productos mock
        repeat(3) { index ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Producto ${index + 1}", fontWeight = FontWeight.Medium)
                        Text("Código: 000${index + 1}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text("Descripción del producto", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Text("$${(index + 1) * 5000}", fontWeight = FontWeight.Bold, color = Emerald700)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Total:", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("$15000", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Emerald700)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Emerald700)
        ) {
            Text("Volver", modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}
