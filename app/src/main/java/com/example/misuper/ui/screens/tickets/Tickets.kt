package com.example.misuper.ui.screens.tickets

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.misuper.ui.theme.*
import java.util.Locale

data class TicketProduct(val name: String, val code: String, val price: Double)
data class Ticket(
    val supermarket: String,
    val total: Double,
    val date: String,
    val time: String,
    val products: List<TicketProduct> = emptyList()
)

@Composable
fun TicketsScreen() {
    var showModal by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val tickets = remember {
        mutableStateListOf(
            Ticket("Carrefour", 45200.0, "12 OCT 2023", "14:30", listOf(
                TicketProduct("Leche Larga Vida", "779123456", 1200.0),
                TicketProduct("Pan Lactal", "779876543", 2500.0),
                TicketProduct("Yogurt Griego", "779456123", 1800.0)
            )),
            Ticket("Coto", 12800.0, "10 OCT 2023", "18:15", listOf(
                TicketProduct("Arroz Gallo Oro", "779001122", 950.0),
                TicketProduct("Fideos Tallarin", "779334455", 800.0)
            )),
            Ticket("Jumbo", 32150.0, "08 OCT 2023", "11:00", listOf(
                TicketProduct("Bife de Chorizo", "1001", 15400.0),
                TicketProduct("Vino Malbec", "779998877", 6500.0)
            ))
        )
    }

    val filteredTickets = tickets.filter { ticket ->
        ticket.supermarket.contains(searchQuery, ignoreCase = true) ||
        ticket.date.contains(searchQuery, ignoreCase = true) ||
        ticket.total.toString().contains(searchQuery) ||
        ticket.products.any { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Box(modifier = Modifier.fillMaxSize().background(Slate950)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column {
                    TicketsHeader()
                    SearchBar(query = searchQuery, onQueryChange = { searchQuery = it })
                }
            },
            floatingActionButton = {
                FabAddTicket(onClick = { showModal = true })
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
            ) {
                items(filteredTickets) { ticket ->
                    TicketCard(ticket)
                }
            }
        }

        if (showModal) {
            RegisterPurchaseModal(onClose = { showModal = false })
        }
    }
}

@Composable
fun FabAddTicket(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = Emerald600,
        contentColor = White,
        shape = CircleShape,
        modifier = Modifier
            .padding(bottom = 16.dp, end = 8.dp)
            .size(56.dp)
            .border(4.dp, Slate950, CircleShape),
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = "Agregar Ticket", modifier = Modifier.size(28.dp))
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Slate800, RoundedCornerShape(16.dp)),
            placeholder = { Text("Buscar por súper, fecha o producto...", color = Slate500, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate500) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Slate500)
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Slate900,
                unfocusedContainerColor = Slate900,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Emerald500,
                focusedTextColor = White,
                unfocusedTextColor = White
            ),
            singleLine = true
        )
    }
}

@Composable
fun TicketsHeader() {
    Row(
        modifier = Modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "HISTORIAL DE COMPRAS",
            style = MaterialTheme.typography.labelSmall.copy(
                color = Slate100,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                letterSpacing = 2.4.sp
            )
        )
    }
}

@Composable
fun TicketCard(ticket: Ticket) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = BorderStroke(1.dp, Slate800)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Miniatura
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Slate800),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Receipt,
                        contentDescription = null,
                        tint = Slate600.copy(alpha = 0.6f),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        ticket.supermarket,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.CalendarToday, null, tint = Slate500, modifier = Modifier.size(10.dp))
                            Text(
                                ticket.date,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Slate500,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp
                                )
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.AccessTime, null, tint = Slate500, modifier = Modifier.size(10.dp))
                            Text(
                                ticket.time,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Slate500,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp
                                )
                            )
                        }
                    }
                }

                Text(
                    "$${String.format(Locale.getDefault(), "%,.0f", ticket.total)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Emerald500,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 20.dp),
                        thickness = 1.dp,
                        color = Slate800
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "DESGLOSE DE PRODUCTOS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Slate500,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        
                        TextButton(
                            onClick = { /* Delete logic */ },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Delete, null, tint = Rose500, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ELIMINAR", color = Rose500, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    ticket.products.forEach { product ->
                        ProductItemRow(product)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ProductItemRow(product: TicketProduct) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Slate950.copy(alpha = 0.4f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(product.name, color = White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(product.code, color = Slate500, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            "$${String.format(Locale.getDefault(), "%,.0f", product.price)}",
            color = White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun RegisterPurchaseModal(onClose: () -> Unit) {
    var supermarket by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        // Logic to handle captured image
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        capturedImageUri = uri
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp))
                .background(Slate900)
                .clickable(enabled = false) { }
                .padding(32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        "Registrar Compra",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        "Ingresa los detalles del ticket...",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate400, fontSize = 14.sp)
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Slate500)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ModalTextField(
                    value = supermarket,
                    onValueChange = { supermarket = it },
                    placeholder = "Súper",
                    leadingIcon = Icons.Default.Store
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        ModalTextField(
                            value = date,
                            onValueChange = { date = it },
                            placeholder = "Fecha",
                            leadingIcon = Icons.Default.CalendarToday
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ModalTextField(
                            value = time,
                            onValueChange = { time = it },
                            placeholder = "Hora",
                            leadingIcon = Icons.Default.AccessTime
                        )
                    }
                }

                ModalTextField(
                    value = total,
                    onValueChange = { total = it },
                    placeholder = "Precio Total",
                    leadingIcon = Icons.Default.LocalOffer,
                    isLarge = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Camera Button
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { cameraLauncher.launch(null) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (capturedImageUri != null) Emerald600.copy(alpha = 0.1f) else Slate800,
                        border = BorderStroke(1.dp, if (capturedImageUri != null) Emerald500 else Slate700.copy(alpha = 0.5f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (capturedImageUri != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, null, tint = Emerald500, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("CAPTURADO", color = Emerald500, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            } else {
                                Icon(Icons.Default.PhotoCamera, null, tint = Slate400, modifier = Modifier.size(24.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Gallery Button
                    Surface(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { galleryLauncher.launch("image/*") },
                        shape = RoundedCornerShape(16.dp),
                        color = Slate800,
                        border = BorderStroke(1.dp, Slate700.copy(alpha = 0.5f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Image, null, tint = Slate400, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { /* Save logic */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text(
                    "GUARDAR COMPRA",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = White,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        letterSpacing = 2.8.sp
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ModalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    isLarge: Boolean = false
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isLarge) 64.dp else 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Slate700.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        placeholder = { 
            Text(
                placeholder, 
                color = Slate500, 
                fontSize = if (isLarge) 18.sp else 16.sp,
                fontWeight = if (isLarge) FontWeight.Bold else FontWeight.Normal
            ) 
        },
        leadingIcon = { Icon(leadingIcon, null, tint = Slate500, modifier = Modifier.size(18.dp)) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Slate800,
            unfocusedContainerColor = Slate800,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = Emerald500,
            focusedTextColor = if (isLarge) White else Slate100,
            unfocusedTextColor = if (isLarge) White else Slate100
        ),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontSize = if (isLarge) 18.sp else 16.sp,
            fontWeight = if (isLarge) FontWeight.Bold else FontWeight.Normal
        )
    )
}
