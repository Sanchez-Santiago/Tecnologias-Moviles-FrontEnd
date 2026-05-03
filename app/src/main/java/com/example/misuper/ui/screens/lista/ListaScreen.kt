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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.misuper.ui.theme.*

data class ShoppingItem(
    val id: Int,
    val name: String,
    val quantity: Int,
    val category: String,
    val estPrice: Double,
    val realPrice: Double,
    val isChecked: Boolean = false
)

@Composable
fun ListaScreen() {
    var selectedFilter by remember { mutableStateOf("TODOS") }
    
    val items = remember {
        mutableStateListOf(
            ShoppingItem(1, "Leche Entera", 2, "ESENCIALES", 1200.0, 1150.0),
            ShoppingItem(2, "Pan Tajado", 1, "ESENCIALES", 800.0, 850.0, true),
            ShoppingItem(3, "Vino Tinto", 1, "EXTRAS", 4500.0, 4200.0),
            ShoppingItem(4, "Detergente", 1, "SECUNDARIOS", 2500.0, 2600.0),
            ShoppingItem(5, "Yogurt Griego", 3, "SECUNDARIOS", 900.0, 950.0)
        )
    }

    val filteredItems = if (selectedFilter == "TODOS") items else items.filter { it.category == selectedFilter }

    Box(modifier = Modifier.fillMaxSize().background(Slate950)) {
        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = { FabAddItem() }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                PriorityFilters(
                    selectedFilter = selectedFilter,
                    onFilterSelected = { selectedFilter = it },
                    counts = mapOf(
                        "TODOS" to items.size,
                        "ESENCIALES" to items.count { it.category == "ESENCIALES" },
                        "SECUNDARIOS" to items.count { it.category == "SECUNDARIOS" },
                        "EXTRAS" to items.count { it.category == "EXTRAS" }
                    )
                )

                if (filteredItems.isEmpty()) {
                    EmptyState()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        items(filteredItems, key = { it.id }) { item ->
                            ItemCard(
                                item = item,
                                onCheckedChange = { checked ->
                                    val index = items.indexOfFirst { it.id == item.id }
                                    if (index != -1) items[index] = item.copy(isChecked = checked)
                                }
                            )
                        }
                    }
                }
            }
        }
        
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            ShoppingSummary()
        }
    }
}

@Composable
fun PriorityFilters(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    counts: Map<String, Int>
) {
    val filters = listOf("TODOS", "ESENCIALES", "SECUNDARIOS", "EXTRAS")
    
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
            "ESENCIALES" -> Emerald600
            "SECUNDARIOS" -> Blue600
            "EXTRAS" -> Amber600
            else -> Slate700
        }
    } else Slate900

    val scale by animateFloatAsState(if (isSelected) 1.02f else 1f)

    Surface(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = backgroundColor,
        border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, Slate800) else null,
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
                    color = if (isSelected) White else Slate500
                )
            )
            
            Box(
                modifier = Modifier
                    .background(White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) White else Slate500
                    )
                )
            }
        }
    }
}

@Composable
fun ItemCard(item: ShoppingItem, onCheckedChange: (Boolean) -> Unit) {
    var isPressing by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressing) 0.98f else 1f)
    
    val borderColor by animateColorAsState(
        if (item.isChecked) Emerald500.copy(alpha = 0.3f) else Slate800
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .clickable { 
                onCheckedChange(!item.isChecked)
            },
        colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CustomCheckbox(isChecked = item.isChecked)
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = if (item.isChecked) Slate100.copy(alpha = 0.4f) else Slate100,
                                fontWeight = FontWeight.Bold,
                                textDecoration = if (item.isChecked) TextDecoration.LineThrough else null
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "x${item.quantity}",
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                        )
                    }
                }
                
                val categoryColor = when (item.category) {
                    "ESENCIALES" -> Emerald500
                    "SECUNDARIOS" -> Blue500
                    "EXTRAS" -> Amber500
                    else -> Slate400
                }
                Box(modifier = Modifier.size(8.dp).background(categoryColor, CircleShape))
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Est: $${item.estPrice}",
                    style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 12.sp)
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$${item.realPrice}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Emerald500,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = Slate500,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CustomCheckbox(isChecked: Boolean) {
    val backgroundColor by animateColorAsState(if (isChecked) Emerald500 else Color.Transparent)
    val borderColor by animateColorAsState(if (isChecked) Emerald500 else Slate500)
    
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
fun ShoppingSummary() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 80.dp), // Adjust for BottomBar
        color = Slate900.copy(alpha = 0.8f),
        tonalElevation = 0.dp
    ) {
        Box(modifier = Modifier.blur(12.dp)) // Backdrop blur simulation
        
        Column {
            LinearProgressIndicator(
                progress = { 0.65f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = Emerald500,
                trackColor = Slate800,
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total estimado", style = MaterialTheme.typography.labelSmall.copy(color = Slate500))
                    Text("$8.450", style = MaterialTheme.typography.titleMedium.copy(color = White))
                }
                
                Text(
                    "$11.250",
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
fun FabAddItem() {
    FloatingActionButton(
        onClick = { },
        containerColor = Emerald600,
        contentColor = White,
        shape = CircleShape,
        modifier = Modifier
            .padding(bottom = 100.dp, end = 8.dp) // Lifted to not cover nav
            .size(56.dp)
            .border(4.dp, Slate950, CircleShape),
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
            tint = Slate800
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "TU LISTA ESTÁ VACÍA",
            style = MaterialTheme.typography.labelSmall.copy(
                color = Slate800,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Black
            )
        )
    }
}
