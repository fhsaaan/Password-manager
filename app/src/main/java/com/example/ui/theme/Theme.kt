package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PurpleAccent,
    secondary = PurpleContainer,
    tertiary = PurplePrimary,
    background = Color(0xFF141218), // Deep dark plum/slate
    surface = Color(0xFF1D1B20),    // Darker card surface
    surfaceVariant = Color(0xFF49454F),
    onPrimary = Color(0xFF21005D),
    onSecondary = Color(0xFF21005D),
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5),
    onSurfaceVariant = Color(0xFFCAC4D0)
)

private val LightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    secondary = PurpleContainer,
    tertiary = PurpleAccent,
    background = GeometricBg,
    surface = GeometricCard,
    surfaceVariant = GeometricInputBg,
    onPrimary = Color.White,
    onSecondary = DarkPurpleText,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep specialized secure look by default, disabling dynamic system styling overhead
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
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
