package com.example.misuper.ui.screens.inicio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.misuper.data.model.Usuario
import com.example.misuper.ui.theme.*

@Composable
fun NewMembersSection(
    members: List<Usuario>,
    onAddClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MIEMBROS DE LA FAMILIA",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Slate400,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Black
                )
            )
            Text(
                text = "GESTIONAR",
                modifier = Modifier.clickable { onAddClick() },
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Emerald500,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(members) { member ->
                MemberAvatar(member)
            }
            
            item {
                AddMemberCircle(onClick = onAddClick)
            }
        }
    }
}

@Composable
fun MemberAvatar(member: Usuario) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Slate900)
                .border(2.dp, Emerald500.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = member.nombre.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = White,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        Text(
            text = member.nombre.split(" ").first(),
            style = MaterialTheme.typography.labelSmall.copy(
                color = Slate400,
                fontSize = 10.sp
            )
        )
    }
}

@Composable
fun AddMemberCircle(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Slate900)
                .border(2.dp, Slate800, CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Agregar miembro",
                tint = Slate400,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = "Agregar",
            style = MaterialTheme.typography.labelSmall.copy(
                color = Slate500,
                fontSize = 10.sp
            )
        )
    }
}
