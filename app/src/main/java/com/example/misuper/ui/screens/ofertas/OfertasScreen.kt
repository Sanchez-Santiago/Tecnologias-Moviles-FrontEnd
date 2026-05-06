package com.example.misuper.ui.screens.ofertas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.misuper.ui.theme.*
import com.example.misuper.viewmodel.AppViewModel

data class Oferta(
    val supermercado: String,
    val producto: String,
    val precioAnterior: Double,
    val precioOferta: Double,
    val descuento: String
)

@Composable
fun OfertasScreen(viewModel: AppViewModel) {
    val ofertasEjemplo = listOf(
        Oferta("Carrefour", "Leche Entera 1L", 1200.0, 950.0, "20% OFF"),
        Oferta("Coto", "Pan Tajado", 1800.0, 1400.0, "22% OFF"),
        Oferta("Jumbo", "Arroz Gallo Oro 1kg", 2500.0, 1900.0, "24% OFF"),
        Oferta("Disco", "Aceite Girasol 1.5L", 4200.0, 3500.0, "16% OFF")
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.padding(24.dp).statusBarsPadding()) {
                Text(
                    "OFERTAS PERSONALIZADAS",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                )
                Text(
                    "Basado en tus productos frecuentes",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Próximamente banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                color = Amber500.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Amber500.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = Amber500)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "CONEXIÓN CON SUPERMERCADOS (PRÓXIMAMENTE)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Amber500,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
            ) {
                items(ofertasEjemplo) { oferta ->
                    OfertaCard(oferta)
                }
            }
        }
    }
}

@Composable
fun OfertaCard(oferta: Oferta) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = Emerald500)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(oferta.supermercado, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(oferta.producto, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$${oferta.precioAnterior.toInt()}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        style = MaterialTheme.typography.bodySmall.copy(
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "$${oferta.precioOferta.toInt()}",
                        color = Emerald500,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .background(Emerald600, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    oferta.descuento,
                    color = White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}
