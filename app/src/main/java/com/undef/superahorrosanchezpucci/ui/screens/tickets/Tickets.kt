package com.undef.superahorrosanchezpucci.ui.screens.tickets

import android.Manifest
import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.undef.superahorrosanchezpucci.data.model.MetodoPago
import com.undef.superahorrosanchezpucci.data.model.Ticket
import com.undef.superahorrosanchezpucci.data.model.TicketProducto
import com.undef.superahorrosanchezpucci.ui.components.TicketsSkeleton
import com.undef.superahorrosanchezpucci.ui.screens.inicio.ModeSelector
import com.undef.superahorrosanchezpucci.ui.theme.*
import com.undef.superahorrosanchezpucci.viewmodel.TicketsViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import java.util.*

@Composable
fun TicketsScreen(viewModel: TicketsViewModel) {
    var showModal by rememberSaveable { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var ticketToEdit by remember { mutableStateOf<Ticket?>(null) }
    val presupuestos by viewModel.presupuestos.collectAsStateWithLifecycle()
    val allTickets by viewModel.tickets.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    
    val presupuestoActivo = presupuestos.find { it.activo } ?: presupuestos.firstOrNull()
    val tickets = allTickets.filter { 
        it.presupuestoId == presupuestoActivo?.id || it.presupuestoId.isBlank()
    }

    val filteredTickets = tickets.filter { ticket ->
        ticket.supermercado.contains(searchQuery, ignoreCase = true) ||
        ticket.total.toString().contains(searchQuery) ||
        ticket.productos.any { it.nombre.contains(searchQuery, ignoreCase = true) }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (isLoading) {
            TicketsSkeleton()
        } else {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                        val isModoIndividual = presupuestoActivo?.let { active ->
                            presupuestos.find { it.tipo == com.undef.superahorrosanchezpucci.data.model.TipoPresupuesto.INDIVIDUAL }?.id == active.id
                        } ?: false
                        ModeSelector(
                            isModoIndividual = isModoIndividual,
                            onModeChange = { individual -> viewModel.cambiarModo(individual) }
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

private fun parsePriceInput(input: String): Int {
    val value = input.trim()
        .replace("$", "")
        .replace(" ", "")
    if (value.isBlank()) return 0

    val normalized = when {
        value.contains(",") -> value.replace(".", "").replace(",", ".")
        value.count { it == '.' } == 1 && value.substringAfter(".").length in 1..2 -> value
        else -> value.replace(".", "")
    }

    return normalized.toDoubleOrNull()?.roundToInt()
        ?: value.filter { it.isDigit() }.toIntOrNull()
        ?: 0
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
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "DESGLOSE DE PRODUCTOS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (ticket.imagenPath.isNotEmpty()) {
                                TicketActionButton(
                                    text = "VER",
                                    icon = Icons.Default.Visibility,
                                    color = Emerald500,
                                    modifier = Modifier.weight(1f),
                                    onClick = { showAttachment = true }
                                )
                            }
                            TicketActionButton(
                                text = "EDITAR",
                                icon = Icons.Default.Edit,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f),
                                onClick = onEdit
                            )
                            TicketActionButton(
                                text = "ELIMINAR",
                                icon = Icons.Default.Delete,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f),
                                onClick = onDelete
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (ticket.productos.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.4f))
                                .padding(16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                "Sin productos cargados",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        ticket.productos.forEach { product ->
                            ProductItemRow(product)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
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
                        if (ticket.imagenPath.startsWith("content://") || ticket.imagenPath.startsWith("file://")) {
                            // Es una URI (galería o archivo seleccionado)
                            val context = LocalContext.current
                            val bitmap = remember(ticket.imagenPath) {
                                try {
                                    val uri = Uri.parse(ticket.imagenPath)
                                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                        android.graphics.BitmapFactory.decodeStream(inputStream)
                                    }
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Comprobante",
                                    modifier = Modifier.fillMaxSize().padding(16.dp)
                                )
                            } else {
                                Icon(Icons.Default.BrokenImage, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(64.dp))
                            }
                        } else if (ticket.imagenPath.isNotEmpty()) {
                            // Es un archivo guardado internamente
                            val context = LocalContext.current
                            val bitmap = remember(ticket.imagenPath) {
                                try {
                                    context.openFileInput(ticket.imagenPath).use { inputStream ->
                                        android.graphics.BitmapFactory.decodeStream(inputStream)
                                    }
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Comprobante",
                                    modifier = Modifier.fillMaxSize().padding(16.dp)
                                )
                            } else {
                                Icon(Icons.Default.BrokenImage, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(64.dp))
                            }
                        } else if (ticket.imagenPath.isNotEmpty()) {
                            // Es un archivo guardado internamente
                            val context = LocalContext.current
                            val bitmap = remember(ticket.imagenPath) {
                                try {
                                    context.openFileInput(ticket.imagenPath).use { inputStream ->
                                        android.graphics.BitmapFactory.decodeStream(inputStream)
                                    }
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Comprobante",
                                    modifier = Modifier.fillMaxSize().padding(16.dp)
                                )
                            } else {
                                Icon(
                                    if (ticket.imagenPath.contains(".pdf")) Icons.Default.PictureAsPdf else Icons.Default.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        } else {
                            Text("No hay comprobante adjunto", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(
                        "La imagen queda adjunta como comprobante. El total y los productos se cargan desde el formulario.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 12.dp)
                    )
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
fun TicketActionButton(
    text: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
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
fun RegisterPurchaseModal(viewModel: TicketsViewModel, ticketToEdit: Ticket? = null, onClose: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val calendar = Calendar.getInstance()
    if (ticketToEdit != null) calendar.timeInMillis = ticketToEdit.fechaHora
    val presupuestos by viewModel.presupuestos.collectAsStateWithLifecycle()
    
    var supermarket by rememberSaveable { mutableStateOf(ticketToEdit?.supermercado ?: "") }
    var dateMillis by rememberSaveable { mutableLongStateOf(ticketToEdit?.fechaHora ?: System.currentTimeMillis()) }
    var total by rememberSaveable { mutableStateOf(ticketToEdit?.total?.toString() ?: "") }
    var savedImagePath by rememberSaveable { mutableStateOf(ticketToEdit?.imagenPath) }
    var selectedUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var productName by rememberSaveable { mutableStateOf("") }
    var productPrice by rememberSaveable { mutableStateOf("") }
    var productQuantity by rememberSaveable { mutableStateOf("1") }
    var isAnalyzingTicket by rememberSaveable { mutableStateOf(false) }
    var analysisError by rememberSaveable { mutableStateOf<String?>(null) }
    val ticketProducts = remember(ticketToEdit?.id) {
        mutableStateListOf<TicketProducto>().apply {
            addAll(ticketToEdit?.productos.orEmpty())
        }
    }

    var productSuggestions by remember { mutableStateOf<List<com.undef.superahorrosanchezpucci.data.local.CatalogoProductoEntity>>(emptyList()) }
    var showProductSuggestions by remember { mutableStateOf(false) }

    LaunchedEffect(productName) {
        if (productName.length >= 2 && showProductSuggestions) {
            productSuggestions = viewModel.buscarEnCatalogo(productName)
        } else {
            productSuggestions = emptyList()
        }
    }

    fun addProductToTicket() {
        val cleanProductPrice = parsePriceInput(productPrice)
        val cleanQuantity = productQuantity.filter { it.isDigit() }.toIntOrNull() ?: 1
        if (productName.isBlank() || cleanProductPrice <= 0) return

        ticketProducts.add(
            TicketProducto(
                nombre = productName.trim(),
                precio = cleanProductPrice,
                cantidad = cleanQuantity.coerceAtLeast(1)
            )
        )
        productName = ""
        productPrice = ""
        productQuantity = "1"
        showProductSuggestions = false
    }
    
    // Helper function to save bitmap to gallery (must be called from non-composable context)
    fun saveBitmapToGallery(bitmap: Bitmap, context: Context): String {
        // Save to internal storage
        val filename = "ticket_${UUID.randomUUID()}.jpg"
        context.openFileOutput(filename, Context.MODE_PRIVATE).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
        }
        
        // Also save to public gallery (Pictures/MiSuper)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MiSuper")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { os ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, os as java.io.OutputStream)
                }
            }
        } else {
            val picturesDir = File("/storage/emulated/0/Pictures/MiSuper")
            if (!picturesDir.exists()) picturesDir.mkdirs()
            val file = File(picturesDir, filename)
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            }
            // Notify gallery
            val mediaScanIntent = android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            mediaScanIntent.data = Uri.fromFile(file)
            context.sendBroadcast(mediaScanIntent)
        }
        
        return filename
    }
    
    // Helper function to copy image from gallery
    fun copyImageFromGallery(uri: Uri): String {
        val filename = "ticket_${UUID.randomUUID()}.jpg"
        context.contentResolver.openInputStream(uri)?.use { input ->
            context.openFileOutput(filename, Context.MODE_PRIVATE).use { output ->
                input.copyTo(output)
            }
        }
        return filename
    }

    fun parseAnalyzedDate(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        val formats = listOf("yyyy-MM-dd", "dd/MM/yyyy", "dd-MM-yyyy", "dd MMM yyyy")
        return formats.firstNotNullOfOrNull { pattern ->
            runCatching {
                SimpleDateFormat(pattern, Locale.getDefault()).apply { isLenient = false }
                    .parse(value)
                    ?.time
            }.getOrNull()
        }
    }

    fun analyzeSavedTicketImage() {
        val imagePath = savedImagePath ?: return
        coroutineScope.launch {
            isAnalyzingTicket = true
            analysisError = null
            runCatching {
                val imageBytes = withContext(Dispatchers.IO) {
                    context.openFileInput(imagePath).use { input -> input.readBytes() }
                }
                val mimeType = selectedUri
                    ?.let { context.contentResolver.getType(it) }
                    ?.takeIf { it.startsWith("image/") }
                    ?: "image/jpeg"

                viewModel.analizarTicketImagen(imageBytes, mimeType)
            }.onSuccess { analysis ->
                analysis.storeName?.takeIf { it.isNotBlank() }?.let { supermarket = it }
                analysis.total?.takeIf { it > 0 }?.let { total = it.toString() }
                parseAnalyzedDate(analysis.purchaseDate)?.let { dateMillis = it }
                if (analysis.products.isNotEmpty()) {
                    ticketProducts.clear()
                    ticketProducts.addAll(analysis.products)
                }
            }.onFailure { error ->
                analysisError = error.message ?: "No se pudo analizar el ticket."
            }
            isAnalyzingTicket = false
        }
    }

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
        if (bitmap != null) {
            val path = saveBitmapToGallery(bitmap, context)
            savedImagePath = path
            selectedUri = null
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = copyImageFromGallery(uri)
            savedImagePath = path
            selectedUri = uri // Keep URI for preview
        }
    }

    val isValid = supermarket.isNotBlank() && parsePriceInput(total) > 0

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
                .fillMaxHeight(0.92f)
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

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "PRODUCTOS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp
                        )
                    )
                    Box {
                        ModalTextField(
                            value = productName,
                            onValueChange = { 
                                productName = it
                                showProductSuggestions = true
                            },
                            placeholder = "Producto",
                            leadingIcon = Icons.Default.Inventory
                        )
                        
                        if (productSuggestions.isNotEmpty() && showProductSuggestions) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 60.dp)
                                    .heightIn(max = 200.dp)
                                    .zIndex(1f),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    productSuggestions.forEach { suggestion ->
                                        ListItem(
                                            headlineContent = { Text(suggestion.name) },
                                            supportingContent = { Text(suggestion.categoryName) },
                                            modifier = Modifier.clickable {
                                                productName = suggestion.name
                                                productPrice = suggestion.price.toInt().toString()
                                                showProductSuggestions = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ModalTextField(
                            value = productPrice,
                            onValueChange = { productPrice = it },
                            placeholder = "Precio",
                            leadingIcon = Icons.Default.LocalOffer,
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                        ModalTextField(
                            value = productQuantity,
                            onValueChange = { productQuantity = it.filter { char -> char.isDigit() }.ifBlank { "1" } },
                            placeholder = "Cant.",
                            leadingIcon = Icons.Default.Numbers,
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(0.72f)
                        )
                    }
                    OutlinedButton(
                        onClick = ::addProductToTicket,
                        enabled = productName.isNotBlank() && parsePriceInput(productPrice) > 0,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AGREGAR PRODUCTO", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    ticketProducts.forEachIndexed { index, product ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(product.nombre, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("${formatPrice(product.precio)} x${product.cantidad}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                            IconButton(onClick = { ticketProducts.removeAt(index) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

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
                            .clickable { 
                                val permissionCheckResult = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                                if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                                    cameraLauncher.launch(null)
                                } else {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = if (savedImagePath != null) Emerald600.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (savedImagePath != null) Emerald500 else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (savedImagePath != null) {
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
                            .clickable { galleryLauncher.launch("image/*") },
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

                if (savedImagePath != null) {
                    Button(
                        onClick = ::analyzeSavedTicketImage,
                        enabled = !isAnalyzingTicket,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Blue600,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isAnalyzingTicket) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isAnalyzingTicket) "ANALIZANDO..." else "ANALIZAR CON IA",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                analysisError?.let { error ->
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Preview
                if (savedImagePath != null || selectedUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            savedImagePath != null -> {
                                val context = LocalContext.current
                                val bitmap = remember(savedImagePath) {
                                    try {
                                        context.openFileInput(savedImagePath).use { inputStream ->
                                            android.graphics.BitmapFactory.decodeStream(inputStream)
                                        }
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Vista previa",
                                        modifier = Modifier.fillMaxSize().padding(16.dp)
                                    )
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Image, null, tint = Blue500)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Imagen guardada", color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                            savedImagePath != null -> {
                                val context = LocalContext.current
                                val bitmap = remember(savedImagePath) {
                                    try {
                                        context.openFileInput(savedImagePath).use { inputStream ->
                                            android.graphics.BitmapFactory.decodeStream(inputStream)
                                        }
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Vista previa",
                                        modifier = Modifier.fillMaxSize().padding(16.dp)
                                    )
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Image, null, tint = Blue500)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Imagen guardada", color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                            selectedUri != null -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AttachFile, null, tint = Blue500)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Archivo seleccionado", color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { 
                    if (isValid) {
                        val cleanPrice = parsePriceInput(total)
                        
                        // Use the saved image path
                        val path = savedImagePath ?: selectedUri?.toString() ?: ticketToEdit?.imagenPath ?: ""
                        
                        val presupuestoActivo = presupuestos.find { it.activo }
                        val ticket = Ticket(
                            id = ticketToEdit?.id ?: UUID.randomUUID().toString(),
                            supermercado = supermarket,
                            direccion = ticketToEdit?.direccion ?: "Dirección",
                            fechaHora = dateMillis,
                            total = cleanPrice,
                            metodoPago = ticketToEdit?.metodoPago ?: MetodoPago.EFECTIVO,
                            imagenPath = path,
                            presupuestoId = ticketToEdit?.presupuestoId ?: presupuestoActivo?.id.orEmpty(),
                            productos = ticketProducts.toList()
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
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
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
