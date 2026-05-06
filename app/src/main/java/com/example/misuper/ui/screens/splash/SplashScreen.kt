package com.example.misuper.ui.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.misuper.ui.theme.Emerald500
import com.example.misuper.ui.theme.Emerald700
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    LaunchedEffect(Unit) {
        delay(2500)
        navController.navigate("LOGIN") {
            popUpTo("SPLASH") { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Emerald700, Emerald500, Color.White)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "SUPER",
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 4.sp
            )
            Text(
                text = "AHORRO",
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 4.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Tu aliado en el supermercado",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
