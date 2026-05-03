package com.example.misuper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.*
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.misuper.ui.theme.MiSuperTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.RequestPage
import com.example.misuper.ui.screens.inicio.HomeScreen

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
            composable("Perfil") { ProfileScreen() }
        }
    }
}

@Composable
fun BottomBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        NavigationBarItem(
            selected = currentDestination?.route == "INICIO",
            onClick = { navController.navigate("INICIO") },
            icon = { Icon(Icons.Filled.Home, null) },
            label = { Text("Inicio") }
        )

        NavigationBarItem(
            selected = currentDestination?.route == "LISTA",
            onClick = { navController.navigate("LISTA") },
            icon = { Icon(Icons.Filled.ShoppingCart, null) },
            label = { Text("Lista") }
        )

        NavigationBarItem(
            selected = currentDestination?.route == "TICKETS",
            onClick = { navController.navigate("TICKETS") },
            icon = { Icon(Icons.Filled.RequestPage, null) },
            label = { Text("Tickets") }
        )

        NavigationBarItem(
            selected = currentDestination?.route == "MAPA",
            onClick = { navController.navigate("MAPA") },
            icon = { Icon(Icons.Filled.LocationOn, null) },
            label = { Text("Mapa") }
        )

        NavigationBarItem(
            selected = currentDestination?.route == "PERFIL",
            onClick = { navController.navigate("PERFIL") },
            icon = { Icon(Icons.Filled.Person, null) },
            label = { Text("Perfil") }
        )
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