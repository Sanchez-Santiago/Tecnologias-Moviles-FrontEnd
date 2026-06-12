package com.undef.superahorrosanchezpucci.ui.screens.familia

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.undef.superahorrosanchezpucci.data.model.RolUsuario
import com.undef.superahorrosanchezpucci.data.model.Usuario
import com.undef.superahorrosanchezpucci.data.remote.dto.BudgetProgress
import com.undef.superahorrosanchezpucci.data.remote.dto.GroupDetailResponse
import com.undef.superahorrosanchezpucci.data.remote.dto.GroupMemberResponse
import com.undef.superahorrosanchezpucci.data.remote.dto.InvitationResponse
import com.undef.superahorrosanchezpucci.ui.components.FamilySkeleton
import com.undef.superahorrosanchezpucci.ui.theme.*
import com.undef.superahorrosanchezpucci.viewmodel.FamilyViewModel

@Composable
fun FamilyMembersScreen(viewModel: FamilyViewModel, onBack: () -> Unit) {
    val grupos by viewModel.grupos.collectAsStateWithLifecycle()
    val invitaciones by viewModel.invitaciones.collectAsStateWithLifecycle()
    val usuarioActual by viewModel.usuarioActual.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val successMsg by viewModel.successMsg.collectAsStateWithLifecycle()
    val budgetProgress by viewModel.budgetProgress.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    var showInviteDialog by remember { mutableStateOf(false) }
    var inviteEmail by remember { mutableStateOf("") }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var selectedCategoria by remember { mutableStateOf("FAMILIA") }
    val categorias = listOf("FAMILIA", "AMIGOS", "TRABAJO")
    var categoriaExpanded by remember { mutableStateOf(false) }
    var grupoExpanded by remember { mutableStateOf(false) }
    var selectedGrupoId by remember { mutableStateOf<String?>(null) }
    var selectedGrupoNombre by remember { mutableStateOf("") }

    LaunchedEffect(grupos) {
        if (selectedGrupoId == null && grupos.isNotEmpty()) {
            selectedGrupoId = grupos.first().id
            selectedGrupoNombre = grupos.first().name
            viewModel.loadBudgetProgress(grupos.first().id)
        }
    }

    LaunchedEffect(selectedGrupoId) {
        selectedGrupoId?.let { viewModel.loadBudgetProgress(it) }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(successMsg) {
        successMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccess()
        }
    }

    val selectedGroup = grupos.find { it.id == selectedGrupoId }
    val esIndividual = selectedGroup?.categoria == "INDIVIDUAL"
    val isAdmin = selectedGroup?.let { group ->
        usuarioActual?.let { user ->
            group.members.any { it.id == user.id && it.role == "ADMIN" }
        } ?: false
    } ?: false

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                if (grupos.isNotEmpty() && isAdmin && !esIndividual) {
                    ExtendedFloatingActionButton(
                        onClick = { showInviteDialog = true },
                        containerColor = Emerald600,
                        contentColor = Color.White,
                        icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                        text = { Text("AGREGAR MIEMBRO") },
                        shape = RoundedCornerShape(24.dp)
                    )
                }
                ExtendedFloatingActionButton(
                    onClick = { showCreateGroupDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("CREAR GRUPO") },
                    shape = RoundedCornerShape(24.dp)
                )
            }
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
                    "GRUPOS",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                )
            }
        }
    ) { padding ->
        if (isLoading && grupos.isEmpty()) {
            FamilySkeleton()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                val pendientes = invitaciones.filter { it.status == "PENDING" }
                if (pendientes.isNotEmpty()) {
                    item {
                        Text(
                            "INVITACIONES PENDIENTES",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Black
                            )
                        )
                    }
                    items(pendientes) { inv ->
                        InvitationCard(inv,
                            onAccept = { viewModel.aceptarInvitacion(inv.token) },
                            onReject = { viewModel.rechazarInvitacion(inv.token) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }

                if (grupos.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.GroupAdd, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Aún no tienes grupos", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("Crea un grupo para empezar a invitar miembros", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { showCreateGroupDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                                    shape = RoundedCornerShape(16.dp)
                                ) { Text("CREAR GRUPO") }
                            }
                        }
                    }
                }

                if (grupos.size > 1) {
                    item {
                        GroupSelectorCard(
                            grupos = grupos,
                            selectedId = selectedGrupoId,
                            onGroupSelected = { id, name ->
                                selectedGrupoId = id
                                selectedGrupoNombre = name
                            }
                        )
                    }
                } else if (grupos.size == 1) {
                    item {
                        Text(
                            "Grupo: ${grupos.first().name}",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                if (selectedGroup != null && budgetProgress.isNotEmpty()) {
                    item {
                        BudgetProgressCard(budgetProgress.first())
                    }
                }

                item {
                    val miembros = selectedGroup?.members ?: emptyList()
                    Text(
                        "MIEMBROS (${miembros.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Black
                        )
                    )
                }
                if (grupos.isNotEmpty()) {
                    val miembros = selectedGroup?.members ?: emptyList<GroupMemberResponse>()
                    items(miembros.size) { idx ->
                        val member = miembros[idx]
                        val isMemberAdmin = member.role == "ADMIN"
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(if (isMemberAdmin) Emerald600 else Blue600, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(member.fullName.take(1).uppercase(), color = White, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(member.fullName, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                    Text(member.email, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                }
                                Surface(
                                    color = if (isMemberAdmin) Emerald500.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (isMemberAdmin) Emerald500.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline)
                                ) {
                                    Text(
                                        if (isMemberAdmin) "ADMIN" else "MIEMBRO",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isMemberAdmin) Emerald500 else MaterialTheme.colorScheme.onSurfaceVariant,
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
    }

    if (showInviteDialog && selectedGrupoId != null) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("Agregar miembro a $selectedGrupoNombre") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Ingresa el correo electrónico del usuario:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inviteEmail,
                        onValueChange = { inviteEmail = it },
                        label = { Text("Correo electrónico") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (inviteEmail.isNotBlank() && selectedGrupoId != null) {
                        viewModel.invitarMiembro(selectedGrupoId!!, inviteEmail)
                        inviteEmail = ""
                        showInviteDialog = false
                    }
                }) { Text("ENVIAR INVITACIÓN") }
            },
            dismissButton = {
                TextButton(onClick = { showInviteDialog = false }) { Text("CANCELAR") }
            }
        )
    }

    if (showCreateGroupDialog) {
        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            title = { Text("Crear Grupo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        label = { Text("Nombre del grupo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(modifier = Modifier.fillMaxWidth().clickable { categoriaExpanded = true }) {
                        OutlinedTextField(
                            value = selectedCategoria,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Categoría") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        DropdownMenu(
                            expanded = categoriaExpanded,
                            onDismissRequest = { categoriaExpanded = false }
                        ) {
                            categorias.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        selectedCategoria = cat
                                        categoriaExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = newGroupName.isNotBlank(),
                    onClick = {
                        viewModel.crearGrupo(newGroupName, selectedCategoria)
                        newGroupName = ""
                        showCreateGroupDialog = false
                    }
                ) { Text("CREAR") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateGroupDialog = false }) { Text("CANCELAR") }
            }
        )
    }
}

@Composable
fun BudgetProgressCard(progress: BudgetProgress) {
    val spent = progress.spent
    val budgetAmount = progress.budgetAmount
    val remaining = maxOf(0.0, budgetAmount - spent)
    val percentage = if (budgetAmount > 0) (spent / budgetAmount * 100).toFloat() else 0f

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "PRESUPUESTO DEL GRUPO",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Black
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                progress.budgetName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { percentage / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = if (percentage > 90f) Rose500 else Emerald500,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                BudgetStat("Total", "$${formatBudgetNum(budgetAmount)}", Modifier.weight(1f))
                BudgetStat("Gastado", "$${formatBudgetNum(spent)}", Modifier.weight(1f))
                BudgetStat("Restante", "$${formatBudgetNum(remaining)}", Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun BudgetStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        )
    }
}

fun formatBudgetNum(amount: Double): String {
    return if (amount >= 1000000) {
        String.format("%.1fM", amount / 1000000)
    } else if (amount >= 1000) {
        String.format("%.0f", amount)
    } else {
        String.format("%.0f", amount)
    }
}

@Composable
fun GroupSelectorCard(
    grupos: List<GroupDetailResponse>,
    selectedId: String?,
    onGroupSelected: (String, String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val grupoActual = grupos.find { it.id == selectedId } ?: grupos.first()

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            onClick = { expanded = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Groups, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(grupoActual.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        grupoActual.categoria ?: "GRUPO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary, fontSize = 10.sp
                        )
                    )
                }
                Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            grupos.forEach { grupo ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(grupo.name, fontWeight = FontWeight.Bold)
                                Text(
                                    grupo.categoria ?: "GRUPO",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.primary, fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    },
                    onClick = {
                        onGroupSelected(grupo.id, grupo.name)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun InvitationCard(inv: InvitationResponse, onAccept: () -> Unit, onReject: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(Blue500.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Email, null, tint = Blue500, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Invitación a ${inv.groupName}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("De: ${inv.invitedByEmail}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onReject, colors = ButtonDefaults.filledTonalButtonColors(containerColor = Rose500.copy(alpha = 0.1f))) {
                    Icon(Icons.Default.Close, null, tint = Rose500, modifier = Modifier.size(16.dp))
                }
                FilledTonalButton(onClick = onAccept, colors = ButtonDefaults.filledTonalButtonColors(containerColor = Emerald500.copy(alpha = 0.1f))) {
                    Icon(Icons.Default.Check, null, tint = Emerald500, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
