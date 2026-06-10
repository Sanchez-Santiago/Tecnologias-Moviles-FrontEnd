package com.undef.superahorrosanchezpucci.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.undef.superahorrosanchezpucci.R
import com.undef.superahorrosanchezpucci.ui.theme.Emerald700
import com.undef.superahorrosanchezpucci.ui.theme.ThemeMode
import com.undef.superahorrosanchezpucci.viewmodel.ThemeViewModel

@Composable
fun SettingsScreen(navController: NavController, viewModel: ThemeViewModel) {
    var notificaciones by remember { mutableStateOf(true) }
    var sincronizacion by remember { mutableStateOf(false) }
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.appearance_title), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.dark_theme_option))
                    Switch(
                        checked = themeMode == ThemeMode.DARK,
                        onCheckedChange = { enabled ->
                            viewModel.updateThemeMode(
                                if (enabled) ThemeMode.DARK
                                else ThemeMode.LIGHT
                            )
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.notifications_title), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.enable_notifications_option))
                    Switch(
                        checked = notificaciones,
                        onCheckedChange = { notificaciones = it }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.auto_sync_option))
                    Switch(
                        checked = sincronizacion,
                        onCheckedChange = { sincronizacion = it }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Super Ahorro v2.0",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
