package com.example.misuper.ui.screens.profile

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.example.misuper.data.model.RolUsuario
import com.example.misuper.data.model.Usuario
import com.example.misuper.ui.theme.*
import com.example.misuper.viewmodel.AppViewModel
import java.util.Locale

@Composable
fun ProfileScreen(viewModel: AppViewModel) {
    var visible by remember { mutableStateOf(false) }
    var showEditProfile by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) { visible = true }

    val members = viewModel.usuarios
    val presupuestos = viewModel.presupuestos

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
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
            topBar = { ProfileHeader() }
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
                        onEdit = { showEditProfile = true }
                    )
                }

                // Family Budget Stats
                item {
                    SectionHeader("ESTADÍSTICAS DE PRESUPUESTO")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        presupuestos.forEach { p ->
                            BudgetStatCard(
                                title = p.nombre,
                                total = p.montoTotal,
                                available = p.montoDisponible,
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
                            MemberRow(member)
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
                            .background(Slate900)
                            .border(1.dp, Slate800, RoundedCornerShape(24.dp))
                    ) {
                        SettingsItem(Icons.Default.Notifications, "Notificaciones", "Configura tus alertas")
                        SettingsItem(Icons.Default.Security, "Privacidad", "Maneja tus datos")
                        SettingsItem(Icons.Default.Help, "Ayuda y Soporte", "Centro de asistencia")
                        SettingsItem(Icons.Default.Logout, "Cerrar Sesión", "Salir de la cuenta", isDestructive = true)
                    }
                }
            }
        }
        
        if (showEditProfile) {
            EditProfileModal(onClose = { showEditProfile = false })
        }
    }
}

fun formatPrice(amount: Int): String {
    val formatter = java.text.DecimalFormat("$#,###.###", java.text.DecimalFormatSymbols(Locale("es", "AR")))
    return formatter.format(amount).replace(",", ".")
}

@Composable
fun ProfileHeader() {
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
                color = Slate100,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                letterSpacing = 2.4.sp
            )
        )
    }
}

@Composable
fun ProfileInfoCard(name: String, email: String, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = BorderStroke(1.dp, Slate800)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Emerald600, CircleShape)
                    .border(4.dp, Slate950, CircleShape),
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
                Text(name, color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
fun BudgetStatCard(title: String, total: Int, available: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = BorderStroke(1.dp, Slate800)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title.uppercase(), color = Slate500, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(formatPrice(total), color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Disponible", color = Emerald500, fontSize = 11.sp)
            Text(formatPrice(available), color = Emerald500, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun MemberRow(member: Usuario) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Slate900)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Slate800, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(member.nombre.take(1).uppercase(), color = White, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(member.nombre, color = White, fontWeight = FontWeight.Bold)
            Text(member.rol.name, color = Slate500, fontSize = 12.sp)
        }
        if (member.rol == RolUsuario.ADMIN) {
            Icon(Icons.Default.Stars, null, tint = Amber500, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String, isDestructive: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (isDestructive) Rose500 else Slate400, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(20.dp))
        Column {
            Text(title, color = if (isDestructive) Rose500 else White, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Slate500, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = Slate700)
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
