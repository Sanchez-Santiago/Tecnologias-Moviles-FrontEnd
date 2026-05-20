package com.undef.superahorrosanchezpucci.ui.screens.compra

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.undef.superahorrosanchezpucci.R
import com.undef.superahorrosanchezpucci.ui.theme.Emerald700
import kotlinx.coroutines.launch

private data class ProductoCompraDraft(
    val nombre: String = "",
    val codigo: String = "",
    val descripcion: String = "",
    val precio: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevaCompraScreen(navController: NavController) {
    var fecha by remember { mutableStateOf("") }
    var hora by remember { mutableStateOf("") }
    var supermercado by remember { mutableStateOf("") }
    val productos = remember { mutableStateListOf(ProductoCompraDraft()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val total by remember {
        derivedStateOf {
            productos.sumOf { producto ->
                producto.precio.filter { it.isDigit() }.toIntOrNull() ?: 0
            }
        }
    }

    fun validarFormulario(): Boolean {
        return fecha.isNotBlank() &&
            hora.isNotBlank() &&
            supermercado.isNotBlank() &&
            productos.isNotEmpty() &&
            productos.all { it.nombre.isNotBlank() && it.precio.filter { char -> char.isDigit() }.toIntOrNull() != null }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.nueva_compra_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back_button))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                OutlinedTextField(
                    value = fecha,
                    onValueChange = { fecha = it },
                    label = { Text(stringResource(R.string.fecha_hint)) },
                    placeholder = { Text("20/05/2026") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = hora,
                    onValueChange = { hora = it },
                    label = { Text(stringResource(R.string.hora_hint)) },
                    placeholder = { Text("14:30") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = supermercado,
                    onValueChange = { supermercado = it },
                    label = { Text(stringResource(R.string.supermercado_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.products_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = { productos.add(ProductoCompraDraft()) }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("Agregar")
                    }
                }
            }

            itemsIndexed(productos) { index, producto ->
                ProductoCompraCard(
                    producto = producto,
                    puedeEliminar = productos.size > 1,
                    onProductoChange = { actualizado -> productos[index] = actualizado },
                    onEliminar = { productos.removeAt(index) }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.total_label),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${'$'}$total",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Emerald700
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        if (validarFormulario()) {
                            navController.popBackStack()
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Completá fecha, hora, supermercado y productos con precio.")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald700)
                ) {
                    Text(
                        text = stringResource(R.string.save_purchase_button),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductoCompraCard(
    producto: ProductoCompraDraft,
    puedeEliminar: Boolean,
    onProductoChange: (ProductoCompraDraft) -> Unit,
    onEliminar: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Producto",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onEliminar,
                    enabled = puedeEliminar
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar producto")
                }
            }

            OutlinedTextField(
                value = producto.nombre,
                onValueChange = { onProductoChange(producto.copy(nombre = it)) },
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = producto.codigo,
                onValueChange = { onProductoChange(producto.copy(codigo = it)) },
                label = { Text("Código") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = producto.descripcion,
                onValueChange = { onProductoChange(producto.copy(descripcion = it)) },
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            OutlinedTextField(
                value = producto.precio,
                onValueChange = { value ->
                    onProductoChange(producto.copy(precio = value.filter { it.isDigit() }))
                },
                label = { Text("Precio") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }
}
