package com.example.walkmansh.theme

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
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.app.Activity
import androidx.compose.runtime.SideEffect

private val DarkColorScheme = darkColorScheme(
    primary = WalkmanDarkPrimary,
    secondary = WalkmanDarkSecondary,
    tertiary = WalkmanGray,
    background = WalkmanDarkBg,
    surface = WalkmanDarkSurface,
    onPrimary = WalkmanDarkTextPrimary,
    onSecondary = WalkmanDarkTextPrimary,
    onBackground = WalkmanDarkTextPrimary,
    onSurface = WalkmanDarkTextPrimary,
    inverseSurface = WalkmanDarkTextPrimary,
    inverseOnSurface = WalkmanDarkBg
)

private val LightColorScheme = lightColorScheme(
    primary = WalkmanPrimary,
    secondary = WalkmanSecondary,
    tertiary = WalkmanGray,
    background = WalkmanLightBg,
    surface = WalkmanLightSurface,
    onPrimary = WalkmanTextPrimary,
    onSecondary = WalkmanTextPrimary,
    onBackground = WalkmanTextPrimary,
    onSurface = WalkmanTextPrimary,
    inverseSurface = WalkmanTextPrimary,
    inverseOnSurface = WalkmanLightBg
)


@Composable
fun WalkmanshTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    // Redesign: Xperia-inspired palette.
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}


