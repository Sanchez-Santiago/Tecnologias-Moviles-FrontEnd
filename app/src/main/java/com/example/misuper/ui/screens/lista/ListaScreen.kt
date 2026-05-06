package com.example.misuper.ui.screens.lista

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.misuper.data.model.Producto
import com.example.misuper.ui.components.ListaSkeleton
import com.example.misuper.ui.screens.inicio.ModeSelector
import com.example.misuper.ui.theme.*
import com.example.misuper.viewmodel.AppViewModel
import java.util.Locale
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext

@Composable
fun ListaScreen(viewModel: AppViewModel) {
    var selectedFilter by remember { mutableStateOf("TODOS") }
    var searchQuery by remember { mutableStateOf("") }
    var showAddItemModal by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<Producto?>(null) }
    
    val presupuestoActivo = viewModel.presupuestos.find { it.activo }
    val isFamiliar = presupuestoActivo?.id == "presupuesto-familiar"
    val listaActualId = if (isFamiliar) "lista-familiar" else "lista-individual"
    
    val listaActual = viewModel.listas.find { it.id == listaActualId }
    val items = listaActual?.productos ?: emptyList<Producto>()

    val filteredItems by remember(items, selectedFilter, searchQuery) {
        derivedStateOf {
            items.filter { item ->
                val matchesFilter = if (selectedFilter == "TODOS") true else item.categoria.name == selectedFilter
                val matchesQuery = item.nombre.contains(searchQuery, ignoreCase = true) ||
                        item.categoria.name.contains(searchQuery, ignoreCase = true) ||
                        item.precioEstimado.toString().contains(searchQuery)
                matchesFilter && matchesQuery
            }
        }
    }

    val totals by remember(items) {
        derivedStateOf {
            val totalEst = items.sumOf { it.precioEstimado * it.cantidad }
            val totalReal = items.filter { it.comprado }.sumOf { (it.precioReal ?: it.precioEstimado) * it.cantidad }
            Pair(totalEst, totalReal)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val context = LocalContext.current
        
        fun compartirLista() {
            val lista = viewModel.listas.find { it.id == listaActualId }
            if (lista != null) {
                val texto = buildString {
                    appendLine("🛒 Mi Lista de Compras: ${lista.nombre}")
                    appendLine("─".repeat(30))
                    lista.productos.forEach { producto ->
                        val check = if (producto.comprado) "✅" else "⬜"
                        appendLine("$check ${producto.nombre} x${producto.cantidad} - $${producto.precioEstimado}")
                    }
                    appendLine("─".repeat(30))
                    appendLine("Total estimado: $${lista.productos.sumOf { it.precioEstimado * it.cantidad }}")
                }
                
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, texto)
                    putExtra(Intent.EXTRA_SUBJECT, "Lista de Compras - MiSuper")
                }
                context.startActivity(Intent.createChooser(intent, "Compartir lista"))
            }
        }
        
        if (viewModel.isLoading) {
            ListaSkeleton()
        } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            ModeSelector(
                                activeId = presupuestoActivo?.id ?: "",
                                onModeChange = { id -> viewModel.cambiarPresupuestoActivo(id) }
                            )
                        }
                        IconButton(
                            onClick = { compartirLista() },
                            modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Compartir lista",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    SearchBar(query = searchQuery, onQueryChange = { searchQuery = it })
                }
            },
            floatingActionButton = { FabAddItem(onClick = { showAddItemModal = true }) },
            bottomBar = { 
                ShoppingSummary(totals.first, totals.second) 
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                PriorityFilters(
                    selectedFilter = selectedFilter,
                    onFilterSelected = { selectedFilter = it },
                    counts = mapOf(
                        "TODOS" to items.size,
                        "ESENCIAL" to items.count { it.categoria.name == "ESENCIAL" },
                        "PRINCIPAL" to items.count { it.categoria.name == "PRINCIPAL" },
                        "SECUNDARIO" to items.count { it.categoria.name == "SECUNDARIO" }
                    )
                )

                if (filteredItems.isEmpty()) {
                    EmptyState()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        items(filteredItems, key = { it.id }) { item ->
                            Box(modifier = Modifier.animateItem()) {
                                ItemCard(
                                    item = item,
                                    onCheckedChange = { _ ->
                                        viewModel.toggleProducto(listaActualId, item.id)
                                    },
                                    onEdit = { editingItem = item },
                                    onDelete = { viewModel.eliminarProducto(listaActualId, item.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
        }
        
        if (showAddItemModal) {
            NewProductScreen(viewModel = viewModel, onClose = { showAddItemModal = false })
        }
        
        if (editingItem != null) {
            NewProductScreen(viewModel = viewModel, itemToEdit = editingItem, onClose = { editingItem = null })
        }
    }
}

fun formatPrice(amount: Int): String {
    val formatter = java.text.DecimalFormat("$#,###.###", java.text.DecimalFormatSymbols(Locale("es", "AR")))
    return formatter.format(amount).replace(",", ".")
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    Box(modifier = Modifier.padding(horizontal = 24.dp)) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
            placeholder = { Text("Buscar producto o precio...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            singleLine = true
        )
    }
}

@Composable
fun PriorityFilters(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    counts: Map<String, Int>
) {
    val filters = listOf("TODOS", "ESENCIAL", "PRINCIPAL", "SECUNDARIO")
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterButton(filters[0], selectedFilter == filters[0], counts[filters[0]] ?: 0, Modifier.weight(1f)) { onFilterSelected(filters[0]) }
            FilterButton(filters[1], selectedFilter == filters[1], counts[filters[1]] ?: 0, Modifier.weight(1f)) { onFilterSelected(filters[1]) }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterButton(filters[2], selectedFilter == filters[2], counts[filters[2]] ?: 0, Modifier.weight(1f)) { onFilterSelected(filters[2]) }
            FilterButton(filters[3], selectedFilter == filters[3], counts[filters[3]] ?: 0, Modifier.weight(1f)) { onFilterSelected(filters[3]) }
        }
    }
}

@Composable
fun FilterButton(
    text: String,
    isSelected: Boolean,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        when (text) {
            "ESENCIAL" -> Emerald600
            "PRINCIPAL" -> Blue600
            "SECUNDARIO" -> Amber600
            else -> MaterialTheme.colorScheme.primary
        }
    } else MaterialTheme.colorScheme.surface

    val scale by animateFloatAsState(if (isSelected) 1.02f else 1f)

    Surface(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = backgroundColor,
        border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
        shadowElevation = if (isSelected) 8.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = if (isSelected) White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            
            Box(
                modifier = Modifier
                    .background(if (isSelected) White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
fun ItemCard(item: Producto, onCheckedChange: (Boolean) -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val categoryColor = when (item.categoria.name) {
        "ESENCIAL" -> Emerald500
        "PRINCIPAL" -> Blue500
        "SECUNDARIO" -> Amber500
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val borderColor by animateColorAsState(
        if (item.comprado) categoryColor.copy(alpha = 0.3f) else categoryColor
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .clickable { onCheckedChange(!item.comprado) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CustomCheckbox(isChecked = item.comprado)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.nombre,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = if (item.comprado) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                textDecoration = if (item.comprado) TextDecoration.LineThrough else null
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "x${item.cantidad}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Edit, "Editar", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Unit: ${formatPrice(item.precioEstimado)}",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                )
                
                val displayPrice = (item.precioReal ?: item.precioEstimado) * item.cantidad
                Text(
                    text = formatPrice(displayPrice),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Emerald500,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                )
            }
        }
    }
}

@Composable
fun CustomCheckbox(isChecked: Boolean) {
    val backgroundColor by animateColorAsState(if (isChecked) Emerald500 else Color.Transparent)
    val borderColor by animateColorAsState(if (isChecked) Emerald500 else MaterialTheme.colorScheme.onSurfaceVariant)
    
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(backgroundColor, CircleShape)
            .border(2.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (isChecked) {
            Icon(Icons.Default.Check, contentDescription = null, tint = White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun ShoppingSummary(totalEst: Int, totalReal: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        tonalElevation = 0.dp
    ) {
        Column {
            LinearProgressIndicator(
                progress = { if (totalEst > 0) totalReal.toFloat() / totalEst else 0f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = Emerald500,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total estimado", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                    Text(formatPrice(totalEst), style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface))
                }

                Text(
                    formatPrice(totalReal),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = Emerald500,
                        fontWeight = FontWeight.Black
                    )
                )
            }
        }
    }
}

@Composable
fun FabAddItem(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = CircleShape,
        modifier = Modifier
            .padding(bottom = 16.dp, end = 8.dp)
            .size(56.dp)
            .border(4.dp, MaterialTheme.colorScheme.background, CircleShape),
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = "Agregar", modifier = Modifier.size(28.dp))
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.ShoppingBag,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "TU LISTA ESTÁ VACÍA",
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.outline,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Black
            )
        )
    }
}
