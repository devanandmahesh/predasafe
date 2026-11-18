package com.example.predasafe.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val PredaSafeDarkColorScheme = darkColorScheme(
    primary = BrightGreen,
    onPrimary = Color.White,
    primaryContainer = ForestGreen,
    onPrimaryContainer = MintGreen,

    secondary = AmberPrimary,
    onSecondary = Color.Black,
    secondaryContainer = AmberDark,
    onSecondaryContainer = AmberLight,

    tertiary = LightGreen,
    onTertiary = Color.Black,
    tertiaryContainer = GradientGreenStart,
    onTertiaryContainer = MintGreen,

    error = DangerRed,
    onError = Color.White,
    errorContainer = DangerRedDark,
    onErrorContainer = Color(0xFFFFCDD2),

    background = DarkBackground,
    onBackground = TextPrimary,

    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondary,

    outline = TextMuted,
    outlineVariant = DarkCardElevated
)

@Composable
fun PredasafeTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = PredaSafeDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                window.statusBarColor = DarkBackground.toArgb()
                window.navigationBarColor = DarkBackground.toArgb()
            }
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}