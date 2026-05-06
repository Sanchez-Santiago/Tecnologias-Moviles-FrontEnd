package com.example.misuper.ui.screens.lista

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.misuper.data.model.Categoria
import com.example.misuper.data.model.Producto
import com.example.misuper.ui.theme.*
import com.example.misuper.viewmodel.AppViewModel
import java.util.*

@Composable
fun NewProductScreen(viewModel: AppViewModel, itemToEdit: Producto? = null, onClose: () -> Unit) {
    var name by remember { mutableStateOf(itemToEdit?.nombre ?: "") }
    var brand by remember { mutableStateOf(itemToEdit?.marca ?: "") }
    var price by remember { mutableStateOf(itemToEdit?.precioEstimado?.toString() ?: "") }
    var selectedCategory by remember { mutableStateOf(itemToEdit?.categoria ?: Categoria.ESENCIAL) }
    var quantity by remember { mutableIntStateOf(itemToEdit?.cantidad ?: 1) }
    
    var nameError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }

    val isEditing = itemToEdit != null
    
    fun validateForm(): Boolean {
        var isValid = true
        
        if (name.isBlank()) {
            nameError = "El nombre es obligatorio"
            isValid = false
        } else {
            nameError = null
        }
        
        val priceValue = price.filter { it.isDigit() }.toIntOrNull()
        if (price.isBlank()) {
            priceError = "El precio es obligatorio"
            isValid = false
        } else if (priceValue == null || priceValue == 0) {
            priceError = "El precio no puede ser 0"
            isValid = false
        } else {
            priceError = null
        }
        
        return isValid
    }
    
    val isValid = name.isNotBlank() && price.isNotBlank() && price.filter { it.isDigit() }.toIntOrNull() ?: 0 > 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(enabled = false) { }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                .padding(32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = if (isEditing) "Editar Producto" else "Añadir Producto",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    )
                    Text(
                        text = if (isEditing) "Actualiza los detalles de tu producto." else "Personaliza tu próximo artículo de compra.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
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

            // Form Fields
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                FormField(label = "Nombre del Producto *") {
                    CustomTextField(
                        value = name,
                        onValueChange = { 
                            name = it
                            if (it.isNotBlank()) nameError = null
                        },
                        placeholder = "Ej: Leche deslactosada",
                        leadingIcon = Icons.Default.Inventory
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FormField(label = "Marca", modifier = Modifier.weight(1f)) {
                        CustomTextField(
                            value = brand,
                            onValueChange = { brand = it },
                            placeholder = "Marca",
                            height = 48.dp
                        )
                    }
                    FormField(label = "Precio Est. *", modifier = Modifier.weight(1f)) {
                        CustomTextField(
                            value = price,
                            onValueChange = { 
                                price = it
                                if (it.isNotBlank()) priceError = null
                            },
                            placeholder = "0.00",
                            leadingIcon = Icons.Default.LocalOffer,
                            height = 48.dp,
                            keyboardType = KeyboardType.Number
                        )
                    }
                }

                FormField(label = "Categoría") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoryButton("ESENCIAL", selectedCategory == Categoria.ESENCIAL, Emerald500, Modifier.weight(1f)) { selectedCategory = Categoria.ESENCIAL }
                        CategoryButton("PRINCIPAL", selectedCategory == Categoria.PRINCIPAL, Blue500, Modifier.weight(1f)) { selectedCategory = Categoria.PRINCIPAL }
                        CategoryButton("SECUNDARIO", selectedCategory == Categoria.SECUNDARIO, Amber500, Modifier.weight(1f)) { selectedCategory = Categoria.SECUNDARIO }
                    }
                }

                FormField(label = "Cantidad") {
                    QuantityCounter(
                        count = quantity,
                        onIncrement = { quantity++ },
                        onDecrement = { if (quantity > 1) quantity-- }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { 
                    if (validateForm()) {
                        val cleanPrice = price.filter { it.isDigit() }.toIntOrNull() ?: 0
                        
                        val newProduct = Producto(
                            id = itemToEdit?.id ?: UUID.randomUUID().toString(),
                            nombre = name,
                            marca = brand,
                            precioEstimado = cleanPrice,
                            precioReal = itemToEdit?.precioReal,
                            cantidad = quantity,
                            comprado = itemToEdit?.comprado ?: false,
                            categoria = selectedCategory
                        )
                        
                        // Determinar en qué lista guardarlo basándose en el presupuesto activo
                        val presupuestoActivo = viewModel.presupuestos.find { it.activo }
                        val listaId = if (presupuestoActivo?.id == "presupuesto-familiar") "lista-familiar" else "lista-individual"
                        
                        println("Intentando guardar producto: $newProduct en lista: $listaId")
                        viewModel.agregarProducto(listaId, newProduct)
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
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (isEditing) Icons.Default.Save else Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEditing) "GUARDAR CAMBIOS" else "CREAR PRODUCTO",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            color = if (isValid) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun FormField(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.5.sp
            )
        )
        content()
    }
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector? = null,
    height: Dp = 56.dp,
    keyboardType: KeyboardType = KeyboardType.Text,
    error: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    1.dp, 
                    if (error != null) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) 
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), 
                    RoundedCornerShape(16.dp)
                ),
            placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp) },
            leadingIcon = if (leadingIcon != null) {
                { Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else null,
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
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
        )
        
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

@Composable
fun CategoryButton(text: String, isSelected: Boolean, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) color else MaterialTheme.colorScheme.surfaceVariant,
        border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSelected) White else MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}

@Composable
fun QuantityCounter(count: Int, onIncrement: () -> Unit, onDecrement: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onDecrement) { Icon(Icons.Default.Remove, null, tint = MaterialTheme.colorScheme.onSurface) }
        Text(count.toString(), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        IconButton(onClick = onIncrement) { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onSurface) }
    }
}
