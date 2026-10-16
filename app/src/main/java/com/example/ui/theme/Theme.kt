package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AegisDarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = CyberVoid,
    primaryContainer = BubbleMine,
    onPrimaryContainer = CyberCyan,
    secondary = CyberEmerald,
    onSecondary = CyberVoid,
    secondaryContainer = CyberSurfaceVariant,
    onSecondaryContainer = CyberEmerald,
    tertiary = CyberAmber,
    onTertiary = CyberVoid,
    background = CyberVoid,
    onBackground = TextPrimary,
    surface = CyberDarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = CyberBorder,
    error = CyberRed,
    onError = TextPrimary,
    errorContainer = CyberRedDark,
    onErrorContainer = TextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = AegisDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = CyberVoid.toArgb()
                window.navigationBarColor = CyberVoid.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
