package com.undef.superahorrosanchezpucci.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.undef.superahorrosanchezpucci.R
import com.undef.superahorrosanchezpucci.ui.theme.Emerald700
import com.undef.superahorrosanchezpucci.viewmodel.AuthViewModel

@Composable
fun LoginScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val authViewModel: AuthViewModel = viewModel()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(Emerald700, shape = RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "S",
                fontSize = 60.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.app_name),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Emerald700
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.email_hint)) },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.password_hint)) },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                errorMessage = null
                if (email.isBlank() || password.isBlank()) {
                    errorMessage = "Completá email y contraseña."
                    return@Button
                }

                isLoading = true
                authViewModel.login(email.trim(), password) { result ->
                    isLoading = false
                    result.onSuccess {
                        navController.navigate("INICIO") {
                            popUpTo("LOGIN") { inclusive = true }
                        }
                    }.onFailure { error ->
                        errorMessage = error.message ?: "No se pudo iniciar sesión."
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Emerald700)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 3.dp
                )
            } else {
                Text(
                    text = stringResource(R.string.login_button).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.no_account),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick = { navController.navigate("REGISTER") }
            ) {
                Text(
                    text = "Registrate",
                    color = Emerald700,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
