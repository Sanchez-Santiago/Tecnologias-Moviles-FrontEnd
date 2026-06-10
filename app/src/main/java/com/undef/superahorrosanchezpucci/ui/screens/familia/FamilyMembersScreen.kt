package com.undef.superahorrosanchezpucci.ui.screens.familia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.undef.superahorrosanchezpucci.R
import com.undef.superahorrosanchezpucci.data.model.RolUsuario
import com.undef.superahorrosanchezpucci.data.model.Usuario
import com.undef.superahorrosanchezpucci.ui.theme.*
import com.undef.superahorrosanchezpucci.viewmodel.FamilyViewModel

@Composable
fun FamilyMembersScreen(viewModel: FamilyViewModel, onBack: () -> Unit) {
    val usuarios by viewModel.usuarios.collectAsStateWithLifecycle()
    var showInviteDialog by remember { mutableStateOf(false) }
    var inviteEmail by remember { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showInviteDialog = true },
                containerColor = Emerald600,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("INVITAR") },
                shape = RoundedCornerShape(24.dp)
            )
        },
        topBar = {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
                }
                Text(
                    "MIEMBROS DE LA FAMILIA",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                )
            }
        }
    ) { padding ->
        if (showInviteDialog) {
            AlertDialog(
                onDismissRequest = { showInviteDialog = false },
                title = { Text("Invitar Familiar") },
                text = {
                    OutlinedTextField(
                        value = inviteEmail,
                        onValueChange = { inviteEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (inviteEmail.isNotBlank()) {
                            viewModel.invitarMiembro(inviteEmail)
                            inviteEmail = ""
                            showInviteDialog = false
                        }
                    }) { Text("ENVIAR") }
                },
                dismissButton = {
                    TextButton(onClick = { showInviteDialog = false }) { Text("CANCELAR") }
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            items(usuarios) { usuario ->
                val isAdmin = usuario.rol == RolUsuario.ADMIN
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (isAdmin) Emerald600 else Blue600,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                usuario.nombre.take(1).uppercase(),
                                color = White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(usuario.nombre, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            Text(usuario.email, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                        Surface(
                            color = if (isAdmin) Emerald500.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isAdmin) Emerald500.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Text(
                                if (isAdmin) "ADMIN" else "MIEMBRO",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isAdmin) Emerald500 else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 8.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
