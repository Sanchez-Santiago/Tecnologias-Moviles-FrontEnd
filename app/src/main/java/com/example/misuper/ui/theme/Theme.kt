package com.example.misuper.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// ----------------------
// ENUM DEL MODO DE TEMA
// ----------------------

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

// ----------------------
// DARK THEME
// ----------------------

private val DarkColorScheme = darkColorScheme(
    primary = Emerald500,
    secondary = Blue500,
    tertiary = Indigo600,
    background = Slate950,
    surface = Slate900,
    onPrimary = Slate950,
    onSecondary = Slate950,
    onTertiary = Slate950,
    onBackground = Slate50,
    onSurface = Slate50,
    outline = Slate800,
    surfaceVariant = Slate900,
    onSurfaceVariant = Slate400,
    error = Rose500,
    onError = White
)

// ----------------------
// LIGHT THEME
// ----------------------

private val LightColorScheme = lightColorScheme(
    primary = Emerald600,
    secondary = Blue600,
    tertiary = Indigo600,
    background = White,
    surface = Slate50,
    onPrimary = White,
    onSecondary = White,
    onTertiary = White,
    onBackground = Slate950,
    onSurface = Slate950,
    outline = Slate200,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    error = Rose600,
    onError = White
)

// ----------------------
// THEME PRINCIPAL
// ----------------------

@Composable
fun MiSuperTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {

    // 🔥 Convertimos ThemeMode → Boolean (dark o light)
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
