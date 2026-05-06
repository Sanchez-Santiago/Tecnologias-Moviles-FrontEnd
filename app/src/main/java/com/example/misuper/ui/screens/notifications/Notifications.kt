package com.example.misuper.ui.screens.notifications

import androidx.compose.foundation.background
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
import com.example.misuper.ui.theme.*
import com.example.misuper.viewmodel.AppViewModel

data class AppNotification(
    val title: String,
    val message: String,
    val time: String,
    val type: NotificationType
)

enum class NotificationType {
    OFFER, BUDGET, MEMBER
}

@Composable
fun NotificationsScreen(viewModel: AppViewModel) {
    val dummyNotifications = listOf(
        AppNotification("¡Nueva Oferta!", "La leche que compraste está en oferta en Carrefour.", "2h ago", NotificationType.OFFER),
        AppNotification("Presupuesto Alcanzado", "Has gastado el 80% de tu presupuesto semanal.", "5h ago", NotificationType.BUDGET),
        AppNotification("Nuevo Miembro", "Lucas se ha unido a tu grupo familiar.", "1d ago", NotificationType.MEMBER)
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.padding(24.dp).statusBarsPadding()) {
                Text(
                    "NOTIFICACIONES",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Próximamente banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                color = Blue600.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Blue600.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, tint = Blue500)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "SINCRONIZACIÓN EN TIEMPO REAL (PRÓXIMAMENTE)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Blue500,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
            ) {
                items(dummyNotifications) { notification ->
                    NotificationItem(notification)
                }
            }
        }
    }
}

@Composable
fun NotificationItem(notification: AppNotification) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when (notification.type) {
                NotificationType.OFFER -> Icons.Default.LocalOffer
                NotificationType.BUDGET -> Icons.Default.PieChart
                NotificationType.MEMBER -> Icons.Default.GroupAdd
            }
            val color = when (notification.type) {
                NotificationType.OFFER -> Emerald500
                NotificationType.BUDGET -> Amber500
                NotificationType.MEMBER -> Blue500
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(notification.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(notification.message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Text(notification.time, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 10.sp)
            }
        }
    }
}
