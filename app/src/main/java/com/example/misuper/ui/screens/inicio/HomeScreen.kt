package com.example.misuper.ui.screens.inicio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.misuper.ui.theme.*

@Composable
fun HomeScreen() {
    val purchases = listOf(
        Triple("Carrefour", "12 Oct 2023", "$45.200"),
        Triple("Coto", "10 Oct 2023", "$12.800"),
        Triple("Jumbo", "08 Oct 2023", "$32.150")
    )

    Scaffold(
        topBar = { Header() },
        containerColor = Slate950
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            
            item { ModeSelector() }

            item { BudgetHero() }

            item { AccumulatedSavingsCard() }

            item { AITipCard() }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "ÚLTIMAS COMPRAS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Slate400,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Black
                        )
                    )
                    purchases.forEach { (supermarket, date, amount) ->
                        PurchaseRow(supermarket, date, amount)
                    }
                }
            }
        }
    }
}

@Composable
fun Header() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Slate950.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, Slate900)
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .height(88.dp)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "SUPER AHORRO",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Slate50,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                )
                Text(
                    text = "Hola, Santiago",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Emerald500,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Slate900, CircleShape)
                            .border(1.dp, Slate800, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Emerald500, CircleShape)
                            .align(Alignment.TopEnd)
                            .border(2.dp, Slate950, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .border(2.dp, Emerald500.copy(alpha = 0.2f), CircleShape)
                        .clip(CircleShape)
                        .background(Slate900),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Slate400)
                }
            }
        }
    }
}

@Composable
fun ModeSelector() {
    var isIndividual by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Slate900, RoundedCornerShape(32.dp))
            .border(1.dp, Slate800, RoundedCornerShape(32.dp))
            .padding(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            SelectorItem(
                text = "INDIVIDUAL",
                isSelected = isIndividual,
                onClick = { isIndividual = true },
                modifier = Modifier.weight(1f)
            )
            SelectorItem(
                text = "FAMILIAR",
                isSelected = !isIndividual,
                onClick = { isIndividual = false },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun SelectorItem(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(28.dp))
            .background(if (isSelected) White else Color.Transparent)
            .border(if (isSelected) 1.dp else 0.dp, if (isSelected) Slate800 else Color.Transparent, RoundedCornerShape(28.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (isSelected) Slate950 else Slate400,
                fontWeight = FontWeight.Black,
                fontSize = 10.sp,
                letterSpacing = 2.sp
            )
        )
    }
}

@Composable
fun BudgetHero() {
    val budget = 120000.0
    val spent = 84200.0
    val ratio = (spent / budget).toFloat()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier.size(176.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(176.dp)) {
                drawArc(
                    color = Slate800,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx())
                )
                drawArc(
                    color = Emerald500,
                    startAngle = -90f,
                    sweepAngle = 360f * ratio,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx())
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "PRESUPUESTO",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Slate400,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    "$120.000",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Slate50,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Ahorrado: $35.800",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Emerald500,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendItem(Emerald500, "ESENCIALES", "$42.000")
            LegendItem(Blue500, "SECUNDARIOS", "$25.000")
            LegendItem(Indigo600, "EXTRAS", "$17.200")
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String, value: String) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Slate400,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        Text(
            value,
            style = MaterialTheme.typography.titleMedium.copy(
                color = Slate50,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        )
    }
}

@Composable
fun AccumulatedSavingsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(40.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = BorderStroke(1.dp, Slate800)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Simulated gradient glow in top right
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.TopEnd)
                    .background(
                        Brush.radialGradient(
                            listOf(Indigo600.copy(alpha = 0.15f), Color.Transparent)
                        )
                    )
            )

            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Indigo600, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Ahorro acumulado",
                        style = MaterialTheme.typography.labelSmall.copy(color = Slate400, fontSize = 10.sp)
                    )
                    Text(
                        "$312.450",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            color = White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 30.sp
                        )
                    )
                }

                Text(
                    "+$12.5k",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Emerald500,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                )
            }
        }
    }
}

@Composable
fun AITipCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(40.dp),
        colors = CardDefaults.cardColors(containerColor = Emerald600)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    "TIP DE AHORRO",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = White.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                )
                Text(
                    "Estás gastando un 12% menos que el mes pasado en lácteos.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                )
            }
        }
    }
}

@Composable
fun PurchaseRow(supermarket: String, date: String, amount: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(Slate900.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Slate950, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Store, contentDescription = null, tint = Emerald500)
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                supermarket,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Slate50,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            )
            Text(
                date,
                style = MaterialTheme.typography.labelSmall.copy(color = Slate400, fontSize = 10.sp)
            )
        }

        Text(
            amount,
            style = MaterialTheme.typography.titleMedium.copy(
                color = Slate400,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        )
    }
}

// No custom extension needed if importing correct one

