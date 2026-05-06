package com.example.misuper.ui.screens.tickets

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.misuper.data.model.MetodoPago
import com.example.misuper.data.model.Ticket
import com.example.misuper.data.model.TicketProducto
import com.example.misuper.ui.screens.inicio.ModeSelector
import com.example.misuper.ui.theme.*
import com.example.misuper.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TicketsScreen(viewModel: AppViewModel) {
    var showModal by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var ticketToEdit by remember { mutableStateOf<Ticket?>(null) }
    
    val presupuestoActivo = viewModel.presupuestos.find { it.activo }
    val tickets = viewModel.tickets.filter { it.presupuestoId == presupuestoActivo?.id }

    val filteredTickets = tickets.filter { ticket ->
        ticket.supermercado.contains(searchQuery, ignoreCase = true) ||
        ticket.total.toString().contains(searchQuery) ||
        ticket.productos.any { it.nombre.contains(searchQuery, ignoreCase = true) }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                        ModeSelector(
                            activeId = presupuestoActivo?.id ?: "",
                            onModeChange = { id -> viewModel.cambiarPresupuestoActivo(id) }
                        )
                    }
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
                if (filteredTickets.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                            Text("No hay tickets en este modo", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        }
                    }
                }
                items(filteredTickets, key = { it.id }) { ticket ->
                    TicketCard(
                        ticket = ticket,
                        onDelete = { viewModel.eliminarTicket(ticket.id) },
                        onEdit = { ticketToEdit = ticket }
                    )
                }
            }
        }

        if (showModal) {
            RegisterPurchaseModal(
                viewModel = viewModel,
                onClose = { showModal = false }
            )
        }

        if (ticketToEdit != null) {
            RegisterPurchaseModal(
                viewModel = viewModel,
                ticketToEdit = ticketToEdit,
                onClose = { ticketToEdit = null }
            )
        }
    }
}

fun formatPrice(amount: Int): String {
    val formatter = java.text.DecimalFormat("$#,###.###", java.text.DecimalFormatSymbols(Locale("es", "AR")))
    return formatter.format(amount).replace(",", ".")
}

@Composable
fun FabAddTicket(onClick: () -> Unit) {
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
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
            placeholder = { Text("Buscar por súper, fecha o producto...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) },
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
fun TicketsHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "HISTORIAL DE COMPRAS",
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                letterSpacing = 2.4.sp
            )
        )
    }
}

@Composable
fun TicketCard(ticket: Ticket, onDelete: () -> Unit, onEdit: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var showAttachment by remember { mutableStateOf(false) }
    val sdfDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Receipt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        ticket.supermercado,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.CalendarToday, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(10.dp))
                            Text(
                                sdfDate.format(Date(ticket.fechaHora)),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp
                                )
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.AccessTime, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(10.dp))
                            Text(
                                sdfTime.format(Date(ticket.fechaHora)),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp
                                )
                            )
                        }
                    }
                }

                Text(
                    formatPrice(ticket.total),
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
                        color = MaterialTheme.colorScheme.outline
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "DESGLOSE DE PRODUCTOS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (ticket.imagenPath.isNotEmpty()) {
                                IconButton(onClick = { showAttachment = true }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Visibility, "Ver Ticket", tint = Emerald500, modifier = Modifier.size(18.dp))
                                }
                            }
                            TextButton(
                                onClick = onEdit,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("EDITAR", color = MaterialTheme.colorScheme.secondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            TextButton(
                                onClick = onDelete,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ELIMINAR", color = MaterialTheme.colorScheme.error, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    ticket.productos.forEach { product ->
                        ProductItemRow(product)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    if (showAttachment) {
        AlertDialog(
            onDismissRequest = { showAttachment = false },
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Comprobante adjunto", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (ticket.imagenPath.contains(".pdf")) Icons.Default.PictureAsPdf else Icons.Default.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAttachment = false }) {
                    Text("CERRAR", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}

@Composable
fun ProductItemRow(product: TicketProducto) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.4f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(product.nombre, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text("x${product.cantidad}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            formatPrice(product.precio),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun RegisterPurchaseModal(viewModel: AppViewModel, ticketToEdit: Ticket? = null, onClose: () -> Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    if (ticketToEdit != null) calendar.timeInMillis = ticketToEdit.fechaHora
    
    var supermarket by remember { mutableStateOf(ticketToEdit?.supermercado ?: "") }
    var dateMillis by remember { mutableLongStateOf(ticketToEdit?.fechaHora ?: System.currentTimeMillis()) }
    var total by remember { mutableStateOf(ticketToEdit?.total?.toString() ?: "") }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    val datePicker = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selected = Calendar.getInstance()
            selected.set(year, month, dayOfMonth)
            dateMillis = selected.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        capturedBitmap = bitmap
        selectedUri = null
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedUri = uri
        capturedBitmap = null
    }

    val isValid = supermarket.isNotBlank() && total.isNotBlank()

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
                .background(MaterialTheme.colorScheme.surface)
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
                        if (ticketToEdit == null) "Registrar Compra" else "Editar Compra",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        "Ingresa los detalles del ticket...",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ModalTextField(
                    value = supermarket,
                    onValueChange = { supermarket = it },
                    placeholder = "Comercio / Súper *",
                    leadingIcon = Icons.Default.Store
                )
                
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .clickable { datePicker.show() },
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.CalendarToday, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Text(
                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(dateMillis)),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                ModalTextField(
                    value = total,
                    onValueChange = { total = it },
                    placeholder = "Precio Total *",
                    leadingIcon = Icons.Default.LocalOffer,
                    isLarge = true,
                    keyboardType = KeyboardType.Number
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
                        color = if (capturedBitmap != null) Emerald600.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (capturedBitmap != null) Emerald500 else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (capturedBitmap != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, null, tint = Emerald500, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("CAPTURADO", color = Emerald500, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            } else {
                                Icon(Icons.Default.PhotoCamera, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Gallery Button
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { galleryLauncher.launch("*/*") }, 
                        shape = RoundedCornerShape(16.dp),
                        color = if (selectedUri != null) Blue600.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (selectedUri != null) Blue500 else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (selectedUri != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AttachFile, null, tint = Blue500, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("ADJUNTO", color = Blue500, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            } else {
                                Icon(Icons.Default.FileUpload, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
                
                // Preview
                if (capturedBitmap != null || selectedUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (capturedBitmap != null) {
                            Image(
                                bitmap = capturedBitmap!!.asImageBitmap(),
                                contentDescription = "Vista previa",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Description, null, tint = Blue500)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Archivo seleccionado", color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (isValid) {
                        val cleanPrice = total.filter { it.isDigit() }.toIntOrNull() ?: 0
                        
                        val path = capturedBitmap?.let { "camera_img_${UUID.randomUUID()}" } 
                                 ?: selectedUri?.toString() 
                                 ?: ticketToEdit?.imagenPath 
                                 ?: ""

                        val ticket = Ticket(
                            id = ticketToEdit?.id ?: UUID.randomUUID().toString(),
                            supermercado = supermarket,
                            direccion = ticketToEdit?.direccion ?: "Dirección",
                            fechaHora = dateMillis,
                            total = cleanPrice,
                            metodoPago = ticketToEdit?.metodoPago ?: MetodoPago.EFECTIVO,
                            imagenPath = path,
                            presupuestoId = ticketToEdit?.presupuestoId ?: "", // El repo asignará el activo si es nuevo
                            productos = ticketToEdit?.productos ?: emptyList()
                        )
                        if (ticketToEdit == null) {
                            viewModel.agregarTicket(ticket)
                        } else {
                            viewModel.actualizarTicket(ticket)
                        }
                        onClose()
                    }
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text(
                    "GUARDAR COMPRA",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isValid) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
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
    isLarge: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isLarge) 64.dp else 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        placeholder = { 
            Text(
                placeholder, 
                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                fontSize = if (isLarge) 18.sp else 16.sp,
                fontWeight = if (isLarge) FontWeight.Bold else FontWeight.Normal
            ) 
        },
        leadingIcon = { Icon(leadingIcon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontSize = if (isLarge) 18.sp else 16.sp,
            fontWeight = if (isLarge) FontWeight.Bold else FontWeight.Normal
        ),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType)
    )
}
