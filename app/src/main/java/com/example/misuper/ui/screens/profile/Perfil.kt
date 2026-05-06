package com.example.misuper.ui.screens.profile

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.misuper.data.model.RolUsuario
import com.example.misuper.data.model.Usuario
import com.example.misuper.ui.theme.*
import com.example.misuper.viewmodel.AppViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun ProfileScreen(viewModel: AppViewModel, navController: NavController? = null) {
    var visible by remember { mutableStateOf(false) }
    var showEditProfile by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    val isDark = when (viewModel.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    LaunchedEffect(Unit) { visible = true }

    val members = viewModel.usuarios
    val presupuestos = viewModel.presupuestos

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Background Orbs
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset(y = (-200).dp)
                .align(Alignment.TopCenter)
                .blur(100.dp)
                .drawBehind { drawCircle(Emerald500.copy(alpha = 0.05f)) }
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = { ProfileHeader(isDark) },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp),
                contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
            ) {
                // User Info Card
                item {
                    ProfileInfoCard(
                        name = "Santiago",
                        email = "santiago@misuper.com",
                        isDark = isDark,
                        onEdit = { showEditProfile = true }
                    )
                }

                // Theme Selector
                item {
                    SectionHeader("TEMA DE LA APLICACIÓN")
                    ThemeSelector(
                        currentMode = viewModel.themeMode,
                        isDark = isDark,
                        onModeChange = { viewModel.updateThemeMode(it) }
                    )
                }

                // Family Budget Stats
                item {
                    SectionHeader("ESTADÍSTICAS DE PRESUPUESTO")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        viewModel.presupuestos.forEach { p ->
                            BudgetStatCard(
                                title = p.nombre,
                                total = p.montoTotal,
                                available = p.montoDisponible,
                                isDark = isDark,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Family Members
                item {
                    SectionHeader("MIEMBROS DE LA FAMILIA (${members.size})")
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        members.forEach { member ->
                            MemberRow(member, isDark)
                        }
                    }
                }

                // Settings
                item {
                    SectionHeader("AJUSTES")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (isDark) Slate900 else Slate50)
                            .border(1.dp, if (isDark) Slate800 else Slate200, RoundedCornerShape(24.dp))
                    ) {
                        SettingsItem(
                            Icons.Default.Notifications, 
                            "Notificaciones", 
                            "Configura tus alertas",
                            isDark = isDark,
                            onClick = { navController?.navigate("NOTIFICACIONES") }
                        )
                        SettingsItem(
                            Icons.Default.History, 
                            "Historial de Compras", 
                            "Ver todas tus compras",
                            isDark = isDark,
                            onClick = { navController?.navigate("HISTORIAL") }
                        )
                        SettingsItem(
                            Icons.Default.BarChart, 
                            "Estadísticas", 
                            "Visualiza tus gastos",
                            isDark = isDark,
                            onClick = { navController?.navigate("ESTADISTICAS") }
                        )
                        SettingsItem(
                            Icons.Default.Settings, 
                            "Configuración", 
                            "Ajustes de la aplicación",
                            isDark = isDark,
                            onClick = { navController?.navigate("SETTINGS") }
                        )
                        SettingsItem(
                            Icons.Default.Security, 
                            "Privacidad", 
                            "Maneja tus datos",
                            isDark = isDark,
                            onClick = { 
                                scope.launch {
                                    snackbarHostState.showSnackbar("Próximamente")
                                }
                            }
                        )
                        SettingsItem(
                            Icons.Default.Help, 
                            "Ayuda y Soporte", 
                            "Centro de asistencia",
                            isDark = isDark,
                            onClick = { 
                                scope.launch {
                                    snackbarHostState.showSnackbar("Próximamente")
                                }
                            }
                        )
                        SettingsItem(
                            Icons.Default.Logout, 
                            "Cerrar Sesión", 
                            "Salir de la cuenta", 
                            isDestructive = true,
                            isDark = isDark,
                            onClick = { }
                        )
                    }
                }
            }
        }
        
        if (showEditProfile) {
            EditProfileModal(onClose = { showEditProfile = false })
        }
    }
}

@Composable
fun ThemeSelector(currentMode: ThemeMode, isDark: Boolean, onModeChange: (ThemeMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(if (isDark) Slate900 else Slate50)
            .border(1.dp, if (isDark) Slate800 else Slate200, RoundedCornerShape(24.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ThemeOption(
            icon = Icons.Default.SettingsBrightness,
            label = "Sistema",
            isSelected = currentMode == ThemeMode.SYSTEM,
            modifier = Modifier.weight(1f),
            onClick = { onModeChange(ThemeMode.SYSTEM) }
        )
        ThemeOption(
            icon = Icons.Default.LightMode,
            label = "Claro",
            isSelected = currentMode == ThemeMode.LIGHT,
            modifier = Modifier.weight(1f),
            onClick = { onModeChange(ThemeMode.LIGHT) }
        )
        ThemeOption(
            icon = Icons.Default.DarkMode,
            label = "Oscuro",
            isSelected = currentMode == ThemeMode.DARK,
            modifier = Modifier.weight(1f),
            onClick = { onModeChange(ThemeMode.DARK) }
        )
    }
}

@Composable
fun ThemeOption(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val contentColor = if (isSelected) White else Slate500
    val containerColor = if (isSelected) Emerald600 else Color.Transparent

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = contentColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

fun formatPrice(amount: Int): String {
    val formatter = java.text.DecimalFormat("$#,###.###", java.text.DecimalFormatSymbols(Locale("es", "AR")))
    return formatter.format(amount).replace(",", ".")
}

@Composable
fun ProfileHeader(isDark: Boolean) {
    Row(
        modifier = Modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            "MI PERFIL",
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (isDark) Slate100 else Slate900,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                letterSpacing = 2.4.sp
            )
        )
    }
}

@Composable
fun ProfileInfoCard(name: String, email: String, isDark: Boolean, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) Slate900 else Slate50),
        border = BorderStroke(1.dp, if (isDark) Slate800 else Slate200)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Emerald600, CircleShape)
                    .border(4.dp, if (isDark) Slate950 else White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    name.take(1).uppercase(),
                    color = White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(20.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = if (isDark) White else Slate950, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(email, color = Slate500, fontSize = 14.sp)
            }
            
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, null, tint = Emerald500)
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        title,
        modifier = Modifier.padding(bottom = 16.dp),
        style = MaterialTheme.typography.labelSmall.copy(
            color = Slate500,
            fontWeight = FontWeight.Black,
            fontSize = 10.sp,
            letterSpacing = 1.5.sp
        )
    )
}

@Composable
fun BudgetStatCard(title: String, total: Int, available: Int, isDark: Boolean, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) Slate900 else Slate50),
        border = BorderStroke(1.dp, if (isDark) Slate800 else Slate200)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title.uppercase(), color = Slate500, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(formatPrice(total), color = if (isDark) White else Slate950, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Disponible", color = Emerald500, fontSize = 11.sp)
            Text(formatPrice(available), color = Emerald500, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun MemberRow(member: Usuario, isDark: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (isDark) Slate900 else Slate50)
            .border(1.dp, if (isDark) Slate800 else Slate200, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(if (isDark) Slate800 else Slate200, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(member.nombre.take(1).uppercase(), color = if (isDark) White else Slate950, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(member.nombre, color = if (isDark) White else Slate950, fontWeight = FontWeight.Bold)
            Text(member.rol.name, color = Slate500, fontSize = 12.sp)
        }
        if (member.rol == RolUsuario.ADMIN) {
            Icon(Icons.Default.Stars, null, tint = Amber500, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector, 
    title: String, 
    subtitle: String, 
    isDark: Boolean,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (isDestructive) Rose500 else Slate400, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(20.dp))
        Column {
            Text(title, color = if (isDestructive) Rose500 else if (isDark) White else Slate950, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Slate500, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = if (isDark) Slate700 else Slate300)
    }
}

@Composable
fun EditProfileModal(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clickable(enabled = false) { },
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900)
        ) {
            Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Editar Perfil", color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = "Santiago",
                    onValueChange = {},
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = White,
                        unfocusedTextColor = White,
                        focusedBorderColor = Emerald500
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                ) {
                    Text("GUARDAR CAMBIOS")
                }
            }
        }
    }
}
