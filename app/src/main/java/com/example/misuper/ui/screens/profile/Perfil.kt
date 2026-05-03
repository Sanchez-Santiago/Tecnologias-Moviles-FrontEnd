package com.example.misuper.ui.screens.profile

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.misuper.ui.theme.*

@Composable
fun ProfileScreen() {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        // Background Orbs for depth
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset(y = (-200).dp)
                .align(Alignment.TopCenter)
                .blur(100.dp)
                .drawBehind {
                    drawCircle(Emerald500.copy(alpha = 0.05f))
                }
        )
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset(y = 200.dp)
                .align(Alignment.BottomCenter)
                .blur(100.dp)
                .drawBehind {
                    drawCircle(Blue500.copy(alpha = 0.05f))
                }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(top = 64.dp, bottom = 100.dp)
        ) {
            item {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn() + expandVertically()
                ) {
                    UserInfoSection()
                }
            }

            item {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(animationSpec = tween(delayMillis = 100)) + slideInVertically(initialOffsetY = { 50 })
                ) {
                    StatsBentoBox()
                }
            }

            item {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(animationSpec = tween(delayMillis = 200)) + slideInVertically(initialOffsetY = { 50 })
                ) {
                    SettingsList()
                }
            }

            item {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(animationSpec = tween(delayMillis = 300)) + slideInVertically(initialOffsetY = { 50 })
                ) {
                    LogoutButton()
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                FooterSection()
            }
        }
    }
}

@Composable
fun UserInfoSection() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            // Avatar Circle
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Emerald600, Color(0xFF064E3B))
                        )
                    )
                    .border(2.dp, Slate800, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color.White
                )
            }

            // Online Indicator with pulse
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 0.4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )

            Box(
                modifier = Modifier
                    .size(18.dp)
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
                    .background(Slate950, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(Emerald500.copy(alpha = alpha), CircleShape)
                        .border(1.dp, Emerald500, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Santiago Sanches",
            style = MaterialTheme.typography.headlineSmall.copy(
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
        )
        Text(
            text = "PLAN PREMIUM FAMILIAR",
            style = MaterialTheme.typography.labelSmall.copy(
                color = Slate500,
                fontSize = 10.sp,
                letterSpacing = 2.4.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
fun StatsBentoBox() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = Slate900,
        border = BorderStroke(1.dp, Slate800)
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TOTAL AHORRADO", style = MaterialTheme.typography.labelSmall.copy(color = Slate500, fontSize = 10.sp))
                Text(
                    "€1,240.50",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Emerald500,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp),
                thickness = 1.dp,
                color = Slate800.copy(alpha = 0.3f)
            )

            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SUPER PUNTOS", style = MaterialTheme.typography.labelSmall.copy(color = Slate500, fontSize = 10.sp))
                Text(
                    "2,840 pts",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Blue500,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
fun SettingsList() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = Slate900,
        border = BorderStroke(1.dp, Slate800)
    ) {
        Column {
            SettingsItem(
                icon = Icons.Default.Notifications,
                iconColor = Blue500,
                title = "Notificaciones",
                subtitle = "Control de alertas",
                isLast = false
            )
            SettingsItem(
                icon = Icons.Default.Group,
                iconColor = Emerald500,
                title = "Miembros del Grupo",
                subtitle = "Gestión familiar",
                isLast = false
            )
            SettingsItem(
                icon = Icons.Default.Security,
                iconColor = Amber500,
                title = "Seguridad y Privacidad",
                subtitle = "Contraseña",
                isLast = false
            )
            SettingsItem(
                icon = Icons.Default.CreditCard,
                iconColor = Indigo600,
                title = "Métodos de Pago",
                subtitle = "Tarjetas vinculadas",
                isLast = false
            )
            SettingsItem(
                icon = Icons.AutoMirrored.Filled.Help,
                iconColor = Slate400,
                title = "Ayuda y Soporte",
                isLast = true
            )
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String? = null,
    isLast: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "scale")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { /* Logic */ }
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Slate950, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Slate100,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Slate500,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Slate700,
                modifier = Modifier.size(16.dp)
            )
        }

        if (!isLast) {
            HorizontalDivider(
                modifier = Modifier.padding(top = 16.dp),
                thickness = 1.dp,
                color = Slate800.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun LogoutButton() {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "scale")
    val bgColor = if (isPressed) Rose500.copy(alpha = 0.1f) else Slate900

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { /* Logic */ },
        shape = RoundedCornerShape(40.dp),
        color = bgColor,
        border = BorderStroke(1.dp, Slate800)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                "CERRAR SESIÓN ACTUAL",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Rose500,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.1.sp
                )
            )
        }
    }
}

@Composable
fun FooterSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            "SUPER AHORRO V1.0.4",
            style = MaterialTheme.typography.labelSmall.copy(
                color = Slate700,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.2.sp
            )
        )
        Text(
            "© 2024 - MATERIA: PROGRAMACIÓN MÓVIL",
            style = MaterialTheme.typography.labelSmall.copy(
                color = Slate700,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.2.sp
            )
        )
    }
}
