package com.example.misuper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.example.misuper.ui.screens.inicio.HomeScreen
import com.example.misuper.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiSuperTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomBar(navController)
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "INICIO",
            modifier = Modifier.padding(padding)
        ) {
            composable("INICIO") { HomeScreen() }
            composable("LISTA") { ListaScreen() }
            composable("TICKETS") { TicketsScreen() }
            composable("MAPA") { MapScreen() }
            composable("PERFIL") { ProfileScreen() }
        }
    }
}

@Composable
fun BottomBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Slate900.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, Slate800)
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp
        ) {
            val items = listOf(
                Triple("INICIO", Icons.Filled.Home, "INICIO"),
                Triple("LISTA", Icons.Filled.ShoppingCart, "LISTA"),
                Triple("TICKETS", Icons.Filled.RequestPage, "TICKETS"),
                Triple("MAPA", Icons.Filled.LocationOn, "MAPA"),
                Triple("PERFIL", Icons.Filled.Person, "PERFIL")
            )

            items.forEach { (route, icon, label) ->
                val selected = currentDestination?.route == route
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = if (selected) White else Slate400
                        )
                    },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = if (selected) White else Slate400
                            )
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    }
}

@Composable
fun ListaScreen() {
    Text("LISTA")
}

@Composable
fun TicketsScreen() {
    Text("TICKETS")
}

@Composable
fun MapScreen() {
    Text("MAPA")
}

@Composable
fun ProfileScreen() {
    Text("PERFIL")
}
