package com.undef.superahorrosanchezpucci

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.undef.superahorrosanchezpucci.ui.screens.auth.LoginScreen
import com.undef.superahorrosanchezpucci.ui.screens.auth.RegisterScreen
import com.undef.superahorrosanchezpucci.ui.screens.compra.DetalleCompraScreen
import com.undef.superahorrosanchezpucci.ui.screens.compra.NuevaCompraScreen
import com.undef.superahorrosanchezpucci.ui.screens.estadisticas.EstadisticasScreen
import com.undef.superahorrosanchezpucci.ui.screens.familia.FamilyMembersScreen
import com.undef.superahorrosanchezpucci.ui.screens.historial.HistorialScreen
import com.undef.superahorrosanchezpucci.ui.screens.inicio.HomeScreen
import com.undef.superahorrosanchezpucci.ui.screens.lista.ListaScreen
import com.undef.superahorrosanchezpucci.ui.screens.notifications.NotificationsScreen
import com.undef.superahorrosanchezpucci.ui.screens.ofertas.OfertasScreen
import com.undef.superahorrosanchezpucci.ui.screens.profile.ProfileScreen
import com.undef.superahorrosanchezpucci.ui.screens.settings.SettingsScreen
import com.undef.superahorrosanchezpucci.ui.screens.splash.SplashScreen
import com.undef.superahorrosanchezpucci.ui.screens.tickets.TicketsScreen
import com.undef.superahorrosanchezpucci.ui.theme.*
import com.undef.superahorrosanchezpucci.data.remote.AuthSessionStore
import com.undef.superahorrosanchezpucci.viewmodel.FamilyViewModel
import com.undef.superahorrosanchezpucci.viewmodel.HomeViewModel
import com.undef.superahorrosanchezpucci.viewmodel.ListaViewModel
import com.undef.superahorrosanchezpucci.viewmodel.OfertasViewModel
import com.undef.superahorrosanchezpucci.viewmodel.NotificationsViewModel
import com.undef.superahorrosanchezpucci.viewmodel.ProfileViewModel
import com.undef.superahorrosanchezpucci.viewmodel.StatisticsViewModel
import com.undef.superahorrosanchezpucci.viewmodel.ThemeViewModel
import com.undef.superahorrosanchezpucci.viewmodel.TicketsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AuthSessionStore.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = viewModel()
            val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
            MiSuperTheme(themeMode = themeMode) {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf("INICIO", "LISTA", "TICKETS", "OFERTAS", "PERFIL")

    val ticketsViewModel: TicketsViewModel = viewModel()
    val ticketsState by ticketsViewModel.tickets.collectAsStateWithLifecycle()

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
                val homeViewModel: HomeViewModel = viewModel()
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToNotifications = { navController.navigate("NOTIFICACIONES") },
                    onNavigateToFamily = { navController.navigate("FAMILIA") }
                )
            }
            composable("LISTA") {
                val listaViewModel: ListaViewModel = viewModel()
                ListaScreen(listaViewModel)
            }
            composable("NUEVA_COMPRA") { NuevaCompraScreen(navController, ticketsViewModel) }
            composable("DETALLE_COMPRA/{compraId}") { backStackEntry ->
                val compraId = backStackEntry.arguments?.getString("compraId") ?: ""
                val compra = ticketsState.find { it.id == compraId }
                DetalleCompraScreen(navController, compra)
            }
            composable("HISTORIAL") {
                HistorialScreen(navController, ticketsState)
            }
            composable("ESTADISTICAS") {
                val statisticsViewModel: StatisticsViewModel = viewModel()
                EstadisticasScreen(statisticsViewModel)
            }
            composable("TICKETS") { TicketsScreen(ticketsViewModel) }
            composable("OFERTAS") {
                val ofertasViewModel: OfertasViewModel = viewModel()
                OfertasScreen(ofertasViewModel)
            }
            composable("PERFIL") {
                val profileViewModel: ProfileViewModel = viewModel()
                ProfileScreen(profileViewModel, navController)
            }
            composable("SETTINGS") {
                SettingsScreen(navController)
            }
            composable("NOTIFICACIONES") {
                val notificationsViewModel: NotificationsViewModel = viewModel()
                NotificationsScreen(notificationsViewModel)
            }
            composable("FAMILIA") {
                val familyViewModel: FamilyViewModel = viewModel()
                FamilyMembersScreen(familyViewModel, onBack = { navController.popBackStack() })
            }
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
                Triple("INICIO", Icons.Filled.Home, stringResource(R.string.home_title)),
                Triple("LISTA", Icons.Filled.ShoppingCart, stringResource(R.string.lista_title)),
                Triple("TICKETS", Icons.Filled.RequestPage, stringResource(R.string.tickets_title)),
                Triple("OFERTAS", Icons.Filled.LocalOffer, stringResource(R.string.ofertas_title)),
                Triple("PERFIL", Icons.Filled.Person, stringResource(R.string.perfil_title))
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
