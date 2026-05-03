package com.example.misuper.ui.screens.lista

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.example.misuper.ui.theme.*

@Composable
fun NewProductScreen(itemToEdit: ShoppingItem? = null, onClose: () -> Unit) {
    var name by remember { mutableStateOf(itemToEdit?.name ?: "") }
    var brand by remember { mutableStateOf("") } // Brand not in ShoppingItem currently
    var price by remember { mutableStateOf(itemToEdit?.realPrice?.toString() ?: "") }
    var selectedCategory by remember { mutableStateOf(itemToEdit?.category ?: "ESENCIALES") }
    var quantity by remember { mutableIntStateOf(itemToEdit?.quantity ?: 1) }
    var selectedUnit by remember { mutableStateOf("Unidades") }
    var selectedPriority by remember { mutableStateOf("Media") }

    val isEditing = itemToEdit != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950.copy(alpha = 0.8f))
            .clickable(enabled = false) { } // Capture clicks to prevent dismissing if background is clicked (though usually you want that)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                .background(Slate900)
                .border(1.dp, Slate800, RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
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
                            color = White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    )
                    Text(
                        text = if (isEditing) "Actualiza los detalles de tu producto." else "Personaliza tu próximo artículo de compra.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Slate400,
                            fontSize = 14.sp
                        )
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

            // Form Fields
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Name
                FormField(label = "Nombre del Producto") {
                    CustomTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = "Ej: Leche deslactosada",
                        leadingIcon = Icons.Default.Inventory
                    )
                }

                // Brand and Price
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FormField(label = "Marca", modifier = Modifier.weight(1f)) {
                        CustomTextField(
                            value = brand,
                            onValueChange = { brand = it },
                            placeholder = "Marca",
                            height = 48.dp
                        )
                    }
                    FormField(label = "Precio Est.", modifier = Modifier.weight(1f)) {
                        CustomTextField(
                            value = price,
                            onValueChange = { price = it },
                            placeholder = "0.00",
                            leadingIcon = Icons.Default.LocalOffer,
                            height = 48.dp,
                            keyboardType = KeyboardType.Number
                        )
                    }
                }

                // Category
                FormField(label = "Categoría") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoryButton("ESENCIALES", selectedCategory == "ESENCIALES", Emerald500, Modifier.weight(1f)) { selectedCategory = "ESENCIALES" }
                        CategoryButton("SECUNDARIOS", selectedCategory == "SECUNDARIOS", Blue500, Modifier.weight(1f)) { selectedCategory = "SECUNDARIOS" }
                        CategoryButton("EXTRAS", selectedCategory == "EXTRAS", Amber500, Modifier.weight(1f)) { selectedCategory = "EXTRAS" }
                    }
                }

                // Quantity and Unit
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FormField(label = "Cantidad", modifier = Modifier.weight(1f)) {
                        QuantityCounter(
                            count = quantity,
                            onIncrement = { quantity++ },
                            onDecrement = { if (quantity > 1) quantity-- }
                        )
                    }
                    FormField(label = "Unidad", modifier = Modifier.weight(1f)) {
                        UnitSelector(selectedUnit) { selectedUnit = it }
                    }
                }

                // Priority
                FormField(label = "Prioridad") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PriorityCapsule("Baja", selectedPriority == "Baja", Slate500) { selectedPriority = "Baja" }
                        PriorityCapsule("Media", selectedPriority == "Media", Amber500) { selectedPriority = "Media" }
                        PriorityCapsule("Alta", selectedPriority == "Alta", Rose500) { selectedPriority = "Alta" }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Footer Button
            Button(
                onClick = { /* TODO: Save logic */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
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
                            color = White
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
                color = Slate500,
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
    keyboardType: KeyboardType = KeyboardType.Text
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Slate700, RoundedCornerShape(16.dp)),
        placeholder = { Text(placeholder, color = Slate600, fontSize = 16.sp) },
        leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null, modifier = Modifier.size(18.dp), tint = Slate600) } },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Slate800,
            unfocusedContainerColor = Slate800,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = Emerald500,
            focusedTextColor = White,
            unfocusedTextColor = White
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )
}

@Composable
fun CategoryButton(text: String, isSelected: Boolean, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) color else Slate800,
        border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, Slate700) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSelected) White else Slate400)
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
            .background(Slate800)
            .border(1.dp, Slate700, RoundedCornerShape(16.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onDecrement) { Icon(Icons.Default.Remove, null, tint = White) }
        Text(count.toString(), color = White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        IconButton(onClick = onIncrement) { Icon(Icons.Default.Add, null, tint = White) }
    }
}

@Composable
fun UnitSelector(selectedUnit: String, onUnitSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val units = listOf("Unidades", "Kilogramos", "Litros", "Gramos")

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Slate800)
                .border(1.dp, Slate700, RoundedCornerShape(16.dp))
                .clickable { expanded = true }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(selectedUnit, color = White, fontSize = 14.sp)
            Icon(Icons.Default.ArrowDropDown, null, tint = Slate400)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Slate800).border(1.dp, Slate700)
        ) {
            units.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit, color = White) },
                    onClick = {
                        onUnitSelected(unit)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun PriorityCapsule(text: String, isSelected: Boolean, color: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick),
        color = if (isSelected) color else Slate800,
        border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, Slate700) else null
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSelected) White else Slate400)
        )
    }
}
