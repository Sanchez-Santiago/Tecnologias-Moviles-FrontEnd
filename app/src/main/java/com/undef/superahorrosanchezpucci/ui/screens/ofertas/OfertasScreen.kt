package com.undef.superahorrosanchezpucci.ui.screens.ofertas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.undef.superahorrosanchezpucci.data.remote.dto.OfferResponse
import com.undef.superahorrosanchezpucci.ui.theme.*
import com.undef.superahorrosanchezpucci.viewmodel.OfertasViewModel

private data class MatchedOffer(
    val offer: OfferResponse,
    val matchedProductName: String,
    val reason: String,
    val discountedPrice: Int?,
    val originalPrice: Int?,
    val discountPercent: Int
)

@Composable
fun OfertasScreen(viewModel: OfertasViewModel) {
    val presupuestos by viewModel.presupuestos.collectAsStateWithLifecycle()
    val listas by viewModel.listas.collectAsStateWithLifecycle()
    val tickets by viewModel.tickets.collectAsStateWithLifecycle()
    val offers by viewModel.offers.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadActiveOffers()
    }

    val matchedOffers = remember(offers, listas, tickets) {
        if (offers.isEmpty()) emptyList()
        else matchOffersToProducts(offers, listas, tickets)
    }

    var selectedStore by remember { mutableStateOf("TODOS") }
    var minDiscount by remember { mutableStateOf(0) }
    var storeFilterExpanded by remember { mutableStateOf(false) }
    var discountFilterExpanded by remember { mutableStateOf(false) }

    val stores = remember(matchedOffers) {
        matchedOffers.map { it.offer.storeName ?: "General" }.distinct().sorted()
    }
    val discountOptions = listOf(0 to "Todos", 10 to "10%+", 25 to "25%+", 50 to "50%+")

    val filteredOffers = remember(matchedOffers, selectedStore, minDiscount) {
        matchedOffers.filter { m ->
            val storeMatch = selectedStore == "TODOS" || (m.offer.storeName ?: "General") == selectedStore
            val discountMatch = m.discountPercent >= minDiscount
            storeMatch && discountMatch
        }
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
            when {
                isLoading && offers.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                matchedOffers.isEmpty() -> {
                    EmptyOffersState()
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box {
                                    OutlinedButton(
                                        onClick = { storeFilterExpanded = true },
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Text(if (selectedStore == "TODOS") "Supermercado" else selectedStore, fontSize = 12.sp)
                                        Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp))
                                    }
                                    DropdownMenu(expanded = storeFilterExpanded, onDismissRequest = { storeFilterExpanded = false }) {
                                        DropdownMenuItem(text = { Text("Todas") }, onClick = { selectedStore = "TODOS"; storeFilterExpanded = false })
                                        stores.forEach { store ->
                                            DropdownMenuItem(text = { Text(store) }, onClick = { selectedStore = store; storeFilterExpanded = false })
                                        }
                                    }
                                }
                                Box {
                                    OutlinedButton(
                                        onClick = { discountFilterExpanded = true },
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Text("${minDiscount}%+ desc.", fontSize = 12.sp)
                                        Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp))
                                    }
                                    DropdownMenu(expanded = discountFilterExpanded, onDismissRequest = { discountFilterExpanded = false }) {
                                        discountOptions.forEach { (value, label) ->
                                            DropdownMenuItem(text = { Text(label) }, onClick = { minDiscount = value; discountFilterExpanded = false })
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        items(filteredOffers, key = { "${it.offer.id}-${it.matchedProductName}" }) { match ->
                            MatchedOfferCard(match)
                        }
                    }
                }
            }
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
                "No hay ofertas disponibles",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                "Agrega productos a tus listas o carga tickets con desglose para ver ofertas personalizadas.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun MatchedOfferCard(match: MatchedOffer) {
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

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Store, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        match.offer.storeName ?: "Varios supermercados",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Text(match.offer.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                match.offer.description?.let { desc ->
                    Text(desc, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 2)
                }
                Text("Coincide con: ${match.matchedProductName}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (match.discountedPrice != null && match.originalPrice != null) {
                        Text(
                            "$${match.originalPrice}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "$${match.discountedPrice}",
                            color = Emerald500,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            if (match.discountPercent > 0) {
                Box(
                    modifier = Modifier
                        .background(Emerald600, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "${match.discountPercent}% OFF",
                        color = White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

private fun matchOffersToProducts(
    offers: List<OfferResponse>,
    listas: List<com.undef.superahorrosanchezpucci.data.model.ListaCompra>,
    tickets: List<com.undef.superahorrosanchezpucci.data.model.Ticket>
): List<MatchedOffer> {
    val productNames = mutableSetOf<String>()

    listas.flatMap { it.productos }.forEach { p ->
        p.nombre.trim().takeIf { it.isNotBlank() }?.let { productNames.add(it.lowercase()) }
    }
    tickets.flatMap { it.productos }.forEach { p ->
        p.nombre.trim().takeIf { it.isNotBlank() }?.let { productNames.add(it.lowercase()) }
    }

    if (productNames.isEmpty()) return emptyList()

    val matched = mutableListOf<MatchedOffer>()

    offers.forEach { offer ->
        val offerText = "${offer.title} ${offer.description ?: ""} ${offer.termsConditions ?: ""}".lowercase()
        var bestMatch: String? = null

        for (name in productNames) {
            val keywords = name.split(" ").filter { it.length >= 3 }
            if (keywords.isEmpty()) continue
            if (keywords.any { keyword -> offerText.contains(keyword) }) {
                bestMatch = name
                break
            }
        }

        if (bestMatch != null) {
            val discountPercent = if (offer.discountType == "PERCENTAGE") {
                offer.discountValue.toInt()
            } else 0
            val originalPrice = null
            val discountedPrice = null

            matched.add(
                MatchedOffer(
                    offer = offer,
                    matchedProductName = bestMatch,
                    reason = "Producto en tu lista de compras",
                    discountedPrice = discountedPrice,
                    originalPrice = originalPrice,
                    discountPercent = discountPercent
                )
            )
        }
    }

    return matched.sortedByDescending { it.discountPercent }
}
