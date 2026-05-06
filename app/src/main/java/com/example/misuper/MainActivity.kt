package com.example.misuper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.example.misuper.ui.screens.auth.LoginScreen
import com.example.misuper.ui.screens.auth.RegisterScreen
import com.example.misuper.ui.screens.compra.DetalleCompraScreen
import com.example.misuper.ui.screens.compra.NuevaCompraScreen
import com.example.misuper.ui.screens.estadisticas.EstadisticasScreen
import com.example.misuper.ui.screens.familia.FamilyMembersScreen
import com.example.misuper.ui.screens.historial.HistorialScreen
import com.example.misuper.ui.screens.inicio.HomeScreen
import com.example.misuper.ui.screens.lista.ListaScreen
import com.example.misuper.ui.screens.notifications.NotificationsScreen
import com.example.misuper.ui.screens.ofertas.OfertasScreen
import com.example.misuper.ui.screens.profile.ProfileScreen
import com.example.misuper.ui.screens.settings.SettingsScreen
import com.example.misuper.ui.screens.splash.SplashScreen
import com.example.misuper.ui.screens.tickets.TicketsScreen
import com.example.misuper.ui.theme.*

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.misuper.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: AppViewModel = viewModel()
            MiSuperTheme(themeMode = viewModel.themeMode) {
                MainScreen(viewModel)
            }
        }
    }
}


@Composable
fun MainScreen(viewModel: AppViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf("INICIO", "LISTA", "TICKETS", "OFERTAS", "PERFIL")

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomBar(navController)
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "SPLASH",
            modifier = Modifier.padding(padding)
        ) {
            composable("SPLASH") { SplashScreen(navController) }
            composable("LOGIN") { LoginScreen(navController) }
            composable("REGISTER") { RegisterScreen(navController) }
            composable("INICIO") {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToNotifications = { navController.navigate("NOTIFICACIONES") },
                    onNavigateToFamily = { navController.navigate("FAMILIA") }
                )
            }
            composable("LISTA") { ListaScreen(viewModel) }
            composable("NUEVA_COMPRA") { NuevaCompraScreen(navController) }
            composable("DETALLE_COMPRA/{compraId}") { backStackEntry ->
                val compraId = backStackEntry.arguments?.getString("compraId") ?: ""
                DetalleCompraScreen(navController, compraId)
            }
            composable("HISTORIAL") { HistorialScreen(navController) }
            composable("ESTADISTICAS") { EstadisticasScreen(navController) }
            composable("TICKETS") { TicketsScreen(viewModel) }
            composable("OFERTAS") { OfertasScreen(viewModel) }
            composable("PERFIL") { ProfileScreen(viewModel, navController) }
            composable("SETTINGS") { SettingsScreen(navController, viewModel) }

            // Sub-pantallas
            composable("NOTIFICACIONES") { NotificationsScreen(viewModel) }
            composable("FAMILIA") { FamilyMembersScreen(viewModel, onBack = { navController.popBackStack() }) }
        }
    }
}

@Composable
fun BottomBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp
        ) {
            val items = listOf(
                Triple("INICIO", Icons.Filled.Home, "Inicio"),
                Triple("LISTA", Icons.Filled.ShoppingCart, "Lista"),
                Triple("TICKETS", Icons.Filled.RequestPage, "Tickets"),
                Triple("OFERTAS", Icons.Filled.LocalOffer, "Ofertas"),
                Triple("PERFIL", Icons.Filled.Person, "Perfil")
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
                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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


