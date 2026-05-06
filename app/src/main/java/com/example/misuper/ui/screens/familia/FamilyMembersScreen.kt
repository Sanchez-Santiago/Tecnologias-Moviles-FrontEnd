package com.example.misuper.ui.screens.familia

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.misuper.data.model.RolUsuario
import com.example.misuper.data.model.Usuario
import com.example.misuper.ui.theme.*
import com.example.misuper.viewmodel.AppViewModel
import java.util.*

@Composable
fun FamilyMembersScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Slate950,
        topBar = {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = White)
                }
                Text(
                    "MIEMBROS DE LA FAMILIA",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = White,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Emerald600,
                contentColor = White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            Text(
                "Administra quiénes pueden ver y editar las listas de compras y el presupuesto familiar.",
                style = MaterialTheme.typography.bodySmall.copy(color = Slate400),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(viewModel.usuarios) { usuario ->
                    MemberCard(usuario)
                }
            }
        }
    }

    if (showAddDialog) {
        AddMemberDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, email ->
                viewModel.agregarUsuario(
                    Usuario(
                        id = UUID.randomUUID().toString(),
                        nombre = name,
                        email = email,
                        rol = RolUsuario.MIEMBRO
                    )
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
fun MemberCard(usuario: Usuario) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Slate900,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (usuario.rol == RolUsuario.ADMIN) Emerald600 else Blue600,
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
                Text(usuario.nombre, color = White, fontWeight = FontWeight.Bold)
                Text(usuario.email, color = Slate500, fontSize = 12.sp)
            }
            Surface(
                color = if (usuario.rol == RolUsuario.ADMIN) Emerald500.copy(alpha = 0.1f) else Slate800,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (usuario.rol == RolUsuario.ADMIN) Emerald500.copy(alpha = 0.5f) else Slate700
                )
            ) {
                Text(
                    usuario.rol.name,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (usuario.rol == RolUsuario.ADMIN) Emerald500 else Slate400,
                        fontSize = 8.sp
                    )
                )
            }
        }
    }
}

@Composable
fun AddMemberDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        title = { Text("Invitar Miembro", color = White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Nombre") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Slate800,
                        unfocusedContainerColor = Slate800,
                        focusedTextColor = White,
                        unfocusedTextColor = White
                    )
                )
                TextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("Email") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Slate800,
                        unfocusedContainerColor = Slate800,
                        focusedTextColor = White,
                        unfocusedTextColor = White
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, email) }) {
                Text("INVITAR", color = Emerald500)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR", color = Slate400)
            }
        }
    )
}
