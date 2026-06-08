package com.undef.superahorrosanchezpucci.ui.screens.ofertas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.undef.superahorrosanchezpucci.data.model.ListaCompra
import com.undef.superahorrosanchezpucci.data.model.Presupuesto
import com.undef.superahorrosanchezpucci.data.model.Ticket
import com.undef.superahorrosanchezpucci.ui.screens.tickets.formatPrice
import com.undef.superahorrosanchezpucci.ui.theme.*
import com.undef.superahorrosanchezpucci.viewmodel.OfertasViewModel
import kotlin.math.abs

data class Oferta(
    val supermercado: String,
    val producto: String,
    val precioAnterior: Int,
    val precioOferta: Int,
    val descuento: Int,
    val motivo: String,
    val score: Int
)

private data class ProductoFrecuente(
    val nombre: String,
    val precioBase: Int,
    val score: Int
)

@Composable
fun OfertasScreen(viewModel: OfertasViewModel) {
    val presupuestos by viewModel.presupuestos.collectAsStateWithLifecycle()
    val listas by viewModel.listas.collectAsStateWithLifecycle()
    val tickets by viewModel.tickets.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val presupuestoActivo = presupuestos.find { it.activo }
    val ofertas = remember(listas, tickets, presupuestoActivo) {
        generarOfertasPersonalizadas(listas, tickets, presupuestoActivo)
    }

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
                    "Basadas en tu lista y tus tickets guardados",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OfferSyncBanner()

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                ofertas.isEmpty() -> {
                    EmptyOffersState()
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
                    ) {
                        items(ofertas, key = { "${it.supermercado}-${it.producto}-${it.descuento}" }) { oferta ->
                            OfertaCard(oferta)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfferSyncBanner() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "PERSONALIZACION LOCAL ACTIVA",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun EmptyOffersState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.LocalOffer, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(42.dp))
            Text(
                "Todavia no hay productos suficientes",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                "Agrega productos a tus listas o carga tickets con desglose para ver ofertas mas precisas.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun OfertaCard(oferta: Oferta) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
                Text(oferta.motivo, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatPrice(oferta.precioAnterior),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        formatPrice(oferta.precioOferta),
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
                    "${oferta.descuento}% OFF",
                    color = White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

private fun generarOfertasPersonalizadas(
    listas: List<ListaCompra>,
    tickets: List<Ticket>,
    presupuestoActivo: Presupuesto?
): List<Oferta> {
    val listasDelModo = if (presupuestoActivo == null) {
        listas
    } else {
        listas.filter { it.presupuestoId == presupuestoActivo.id }
    }

    val frecuentes = buildMap<String, ProductoFrecuente> {
        listasDelModo.flatMap { it.productos }.forEach { producto ->
            val nombre = producto.nombre.trim()
            if (nombre.isBlank()) return@forEach
            val key = normalizarProducto(nombre)
            val precio = listOf(producto.precioReal, producto.precioEstimado, producto.precio)
                .filterNotNull()
                .firstOrNull { it > 0 } ?: precioReferencia(nombre)
            val score = producto.cantidad + if (producto.comprado) 2 else 1
            val actual = this[key]
            put(
                key,
                ProductoFrecuente(
                    nombre = actual?.nombre ?: nombre,
                    precioBase = maxOf(actual?.precioBase ?: 0, precio),
                    score = (actual?.score ?: 0) + score
                )
            )
        }

        tickets.flatMap { it.productos }.forEach { producto ->
            val nombre = producto.nombre.trim()
            if (nombre.isBlank()) return@forEach
            val key = normalizarProducto(nombre)
            val precio = producto.precio.takeIf { it > 0 } ?: precioReferencia(nombre)
            val actual = this[key]
            put(
                key,
                ProductoFrecuente(
                    nombre = actual?.nombre ?: nombre,
                    precioBase = maxOf(actual?.precioBase ?: 0, precio),
                    score = (actual?.score ?: 0) + (producto.cantidad * 3)
                )
            )
        }
    }.values

    val base = if (frecuentes.isNotEmpty()) {
        frecuentes.sortedByDescending { it.score }.take(12)
    } else {
        productosBase()
    }

    return base
        .mapIndexed { index, producto -> producto.toOferta(index) }
        .sortedWith(compareByDescending<Oferta> { it.score }.thenByDescending { it.descuento })
        .take(10)
}

private fun ProductoFrecuente.toOferta(index: Int): Oferta {
    val descuento = 12 + abs(nombre.hashCode() + index).mod(24)
    val precioAnterior = precioBase.takeIf { it > 0 } ?: precioReferencia(nombre)
    val precioOferta = (precioAnterior * (100 - descuento) / 100.0).toInt().coerceAtLeast(1)
    val supermercados = listOf("Carrefour", "Coto", "Jumbo", "Disco", "ChangoMas", "Dia")
    val supermercado = supermercados[abs(nombre.hashCode() + index).mod(supermercados.size)]

    return Oferta(
        supermercado = supermercado,
        producto = nombre,
        precioAnterior = precioAnterior,
        precioOferta = precioOferta,
        descuento = descuento,
        motivo = if (score > 3) "Producto frecuente" else "Similar a tu lista",
        score = score
    )
}

private fun productosBase(): List<ProductoFrecuente> {
    return listOf(
        ProductoFrecuente("Leche Entera 1L", 1200, 1),
        ProductoFrecuente("Pan Tajado", 1800, 1),
        ProductoFrecuente("Arroz 1kg", 2500, 1),
        ProductoFrecuente("Aceite Girasol 1.5L", 4200, 1)
    )
}

private fun precioReferencia(nombre: String): Int {
    val normalized = normalizarProducto(nombre)
    return when {
        "leche" in normalized -> 1200
        "pan" in normalized -> 1800
        "arroz" in normalized -> 2500
        "aceite" in normalized -> 4200
        "yerba" in normalized -> 3500
        "azucar" in normalized -> 1600
        "fideo" in normalized || "pasta" in normalized -> 1700
        "huevo" in normalized -> 3000
        "pollo" in normalized -> 5500
        "carne" in normalized -> 8000
        else -> 2200
    }
}

private fun normalizarProducto(value: String): String {
    return value
        .lowercase()
        .replace(Regex("[^a-z0-9áéíóúñ ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}
