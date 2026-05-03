package com.example.misuper.ui.screens.mapa

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.misuper.ui.theme.*

data class Store(
    val id: Int,
    val name: String,
    val address: String,
    val distance: String,
    val offers: Int,
    val isOpen: Boolean,
    val color: Color,
    val xOffset: Float, // Normalized 0..1
    val yOffset: Float  // Normalized 0..1
)

@Composable
fun MapScreen() {
    val stores = remember {
        listOf(
            Store(1, "Mercadona Centro", "Calle Mayor, 14", "800m", 3, true, Emerald600, 0.4f, 0.45f),
            Store(2, "Carrefour Express", "Av. Libertad, 22", "1.2km", 5, true, Color(0xFF1E40AF), 0.7f, 0.3f),
            Store(3, "Lidl Market", "Plaza Norte, 5", "2.5km", 2, false, Color(0xFFB91C1C), 0.2f, 0.6f)
        )
    }

    var selectedStore by remember { mutableStateOf<Store?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        // 1. Contenedor Base (Lienzo del Mapa)
        MapLayer()

        // 2. Marcadores de Tiendas
        stores.forEach { store ->
            StoreMarker(
                store = store,
                isSelected = selectedStore?.id == store.id,
                onClick = { selectedStore = if (selectedStore?.id == store.id) null else store }
            )
        }

        // 3. Superposición Superior (Barra de Búsqueda)
        TopOverlay(
            query = searchQuery,
            onQueryChange = { searchQuery = it }
        )

        // 4. Botones de Acción Rápidos
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = if (selectedStore != null) 240.dp else 24.dp, end = 24.dp)
                .padding(bottom = 16.dp) // Extra padding for system nav
        ) {
            QuickActions()
        }

        // 5. Tarjeta de Tienda Seleccionada (Bottom Sheet)
        AnimatedVisibility(
            visible = selectedStore != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selectedStore?.let { store ->
                StoreDetailCard(
                    store = store,
                    onClose = { selectedStore = null }
                )
            }
        }
    }
}

@Composable
fun MapLayer() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridSize = 40.dp.toPx()
        val gridColor = Slate600.copy(alpha = 0.1f)

        // Vertical lines
        for (x in 0..(size.width / gridSize).toInt()) {
            drawLine(
                color = gridColor,
                start = Offset(x * gridSize, 0f),
                end = Offset(x * gridSize, size.height),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Horizontal lines
        for (y in 0..(size.height / gridSize).toInt()) {
            drawLine(
                color = gridColor,
                start = Offset(0f, y * gridSize),
                end = Offset(size.width, y * gridSize),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

@Composable
fun BoxScope.StoreMarker(
    store: Store,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(if (isSelected) 1.25f else 1f, label = "marker_scale")
    
    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .offset(
                x = (store.xOffset * 300).dp, // Simplified positioning for demo
                y = (store.yOffset * 500).dp
            )
            .scale(scale)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Glow effect
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .blur(10.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(Emerald500.copy(alpha = 0.4f), Color.Transparent)
                        ),
                        CircleShape
                    )
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Floating label
            if (isSelected) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = store.name.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }

            // Marker Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(store.color)
                    .border(
                        if (isSelected) 2.dp else 4.dp,
                        if (isSelected) Emerald500 else Slate950,
                        RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Store,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )

                // Offer indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(14.dp)
                        .background(Color.Black, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = store.offers.toString(),
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun TopOverlay(query: String, onQueryChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search Bar
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(24.dp),
            color = Slate900.copy(alpha = 0.9f),
            border = BorderStroke(1.dp, Slate800)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null, tint = Slate400, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(12.dp))
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { 
                        Text("Buscar tiendas cerca...", color = Slate400.copy(alpha = 0.6f), fontWeight = FontWeight.Bold) 
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Emerald500,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Layer Button
        Surface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(24.dp),
            color = Slate900.copy(alpha = 0.9f),
            border = BorderStroke(1.dp, Slate800)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Layers, null, tint = Slate400)
            }
        }
    }
}

@Composable
fun QuickActions() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        FloatingActionButton(
            onClick = {},
            containerColor = Slate900,
            contentColor = Emerald500,
            shape = RoundedCornerShape(16.dp),
            elevation = FloatingActionButtonDefaults.elevation(8.dp)
        ) {
            Icon(Icons.Default.MyLocation, null)
        }

        FloatingActionButton(
            onClick = {},
            containerColor = Slate900,
            contentColor = Slate400,
            shape = RoundedCornerShape(16.dp),
            elevation = FloatingActionButtonDefaults.elevation(8.dp)
        ) {
            Icon(Icons.Default.Fullscreen, null)
        }
    }
}

@Composable
fun StoreDetailCard(store: Store, onClose: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
        color = Slate900,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(32.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Logo Placeholder
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(store.color.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .border(1.dp, store.color.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Store, null, tint = store.color, modifier = Modifier.size(32.dp))
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = store.name,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            StatusBadge(isOpen = store.isOpen)
                        }
                        Text(
                            text = "${store.address} • ${store.distance}",
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate400, fontSize = 12.sp)
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Slate800, CircleShape)
                ) {
                    Icon(Icons.Default.Close, null, tint = Slate400, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = {},
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Navigation, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("RUTA", fontWeight = FontWeight.Bold)
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clickable { },
                    shape = RoundedCornerShape(16.dp),
                    color = Slate800,
                    border = BorderStroke(1.dp, Slate700)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "OFERTAS (${store.offers})",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(isOpen: Boolean) {
    Surface(
        color = if (isOpen) Emerald500.copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, if (isOpen) Emerald500.copy(alpha = 0.5f) else Color.Red.copy(alpha = 0.5f))
    ) {
        Text(
            text = if (isOpen) "ABIERTO" else "CERRADO",
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (isOpen) Emerald500 else Color.Red,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )
        )
    }
}
