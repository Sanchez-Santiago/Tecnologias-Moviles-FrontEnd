package com.example.misuper.ui.screens.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// ----------------------
// HOME SCREEN
// ----------------------

@Composable
fun HomeScreen() {

    val productos = listOf(
        "Leche" to 1200,
        "Pan" to 800,
        "Huevos" to 2500
    )

    val ofertas = listOf(
        "Leche en descuento" to 900,
        "Arroz oferta" to 700
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {
            BudgetCard()
        }

        item {
            SectionTitle("Esenciales")
        }

        items(productos) { (nombre, precio) ->
            ProductCard(nombre, precio)
        }

        item {
            SectionTitle("Ofertas")
        }

        items(ofertas) { (nombre, precio) ->
            OfferCard(nombre, precio)
        }
    }
}

// ----------------------
// PRESUPUESTO
// ----------------------

@Composable
fun BudgetCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            Text(
                "Presupuesto",
                style = MaterialTheme.typography.labelMedium
            )

            Text(
                "$120.000",
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = { }) {
                Text("Editar")
            }
        }
    }
}

// ----------------------
// PRODUCTO
// ----------------------

@Composable
fun ProductCard(nombre: String, precio: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(nombre)
                Text(
                    "Marca genérica",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text("$$precio")
        }
    }
}

// ----------------------
// OFERTA
// ----------------------

@Composable
fun OfferCard(nombre: String, precio: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(nombre)
            Text(
                "Oferta especial",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "$$precio",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

// ----------------------
// TITULO SECCION
// ----------------------

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium
    )
}