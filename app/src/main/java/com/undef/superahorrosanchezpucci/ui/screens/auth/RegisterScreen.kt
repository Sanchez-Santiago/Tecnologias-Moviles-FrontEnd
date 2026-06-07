package com.undef.superahorrosanchezpucci.ui.screens.auth

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.undef.superahorrosanchezpucci.R
import com.undef.superahorrosanchezpucci.data.remote.AuthApi
import com.undef.superahorrosanchezpucci.viewmodel.AppStateStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun RegisterScreen(navController: NavController) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val authApi = remember { AuthApi(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = colorScheme.onSurface,
        unfocusedTextColor = colorScheme.onSurface,
        focusedLabelColor = colorScheme.primary,
        unfocusedLabelColor = colorScheme.onSurfaceVariant,
        cursorColor = colorScheme.primary,
        focusedBorderColor = colorScheme.primary,
        unfocusedBorderColor = colorScheme.outline,
        focusedContainerColor = colorScheme.surface,
        unfocusedContainerColor = colorScheme.surface
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.register_title),
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            color = colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.name_hint)) },
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.email_hint)) },
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.password_hint)) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            colors = fieldColors
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text(stringResource(R.string.confirm_password_hint)) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            colors = fieldColors
        )

        errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(message, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                errorMessage = null

                when {
                    name.isBlank() || email.isBlank() || password.isBlank() -> {
                        errorMessage = "Completá nombre, email y contraseña."
                        return@Button
                    }
                    !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> {
                        errorMessage = "Ingresá un email válido."
                        return@Button
                    }
                    password != confirmPassword -> {
                        errorMessage = "Las contraseñas no coinciden."
                        return@Button
                    }
                }

                scope.launch {
                    isLoading = true
                    val result = runCatching {
                        withContext(Dispatchers.IO) {
                            authApi.register(name.trim(), email.trim(), password)
                        }
                    }
                    isLoading = false

                    result
                        .onSuccess { session ->
                            if (session.token.isNullOrBlank()) {
                                navController.navigate("LOGIN") {
                                    popUpTo("REGISTER") { inclusive = true }
                                }
                                return@onSuccess
                            }

                            AppStateStore.get(context.applicationContext as android.app.Application).reload()
                            navController.navigate("INICIO") {
                                popUpTo("REGISTER") { inclusive = true }
                            }
                        }
                        .onFailure { error ->
                            errorMessage = error.message ?: "No se pudo crear la cuenta."
                        }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.primary,
                contentColor = colorScheme.onPrimary,
                disabledContainerColor = colorScheme.surfaceVariant,
                disabledContentColor = colorScheme.onSurfaceVariant
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    stringResource(R.string.register_button),
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = colorScheme.onPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = { navController.popBackStack() }
        ) {
            Text(stringResource(R.string.have_account), color = colorScheme.primary)
        }
    }
}
