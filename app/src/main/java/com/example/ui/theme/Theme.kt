package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemePreference {
    LIGHT,
    DARK,
    SYSTEM
}

data class SaviorExtendedColors(
    val positive: Color,
    val positiveContainer: Color,
    val borderSubtle: Color,
    val warmAccent: Color,
    val warmContainer: Color,
    val deepBlue: Color
)

val LocalSaviorColors = staticCompositionLocalOf {
    SaviorExtendedColors(
        positive = CalmPositive,
        positiveContainer = CalmPositiveContainer,
        borderSubtle = CalmSurfaceBorder,
        warmAccent = CalmWarmMuted,
        warmContainer = CalmWarmContainer,
        deepBlue = CalmDeepBlue
    )
}

private val SaviorLightColorScheme = lightColorScheme(
    primary = CalmPrimary,
    onPrimary = Color.White,
    primaryContainer = CalmLightBlue,
    onPrimaryContainer = CalmDeepBlue,
    secondary = CalmPositive,
    onSecondary = Color.White,
    secondaryContainer = CalmPositiveContainer,
    onSecondaryContainer = Color(0xFF284537),
    tertiary = CalmWarmMuted,
    onTertiary = Color.White,
    tertiaryContainer = CalmWarmContainer,
    onTertiaryContainer = Color(0xFF5A3927),
    background = CalmBackground,
    onBackground = CalmTextPrimary,
    surface = CalmSurface,
    onSurface = CalmTextPrimary,
    surfaceVariant = CalmLightBlue,
    onSurfaceVariant = CalmTextSecondary,
    outline = CalmSurfaceBorder,
    outlineVariant = Color(0xFFE6EEF5),
    error = CalmSubtleError,
    onError = Color.White,
    errorContainer = CalmSubtleErrorContainer,
    onErrorContainer = Color(0xFF5C2626)
)

private val SaviorDarkColorScheme = darkColorScheme(
    primary = CalmDarkPrimary,
    onPrimary = Color(0xFF0F2233),
    primaryContainer = CalmDarkDeepBlue,
    onPrimaryContainer = Color(0xFFD7E8F7),
    secondary = CalmDarkPositive,
    onSecondary = Color(0xFF102A1F),
    secondaryContainer = CalmDarkPositiveContainer,
    onSecondaryContainer = Color(0xFFCDE7DA),
    tertiary = Color(0xFFD2A388),
    onTertiary = Color(0xFF331D11),
    tertiaryContainer = Color(0xFF3A2A22),
    onTertiaryContainer = Color(0xFFF0D5C5),
    background = CalmDarkBackground,
    onBackground = CalmDarkTextPrimary,
    surface = CalmDarkSurface,
    onSurface = CalmDarkTextPrimary,
    surfaceVariant = CalmDarkSurfaceVariant,
    onSurfaceVariant = CalmDarkTextSecondary,
    outline = CalmDarkBorder,
    outlineVariant = Color(0xFF233244),
    error = CalmDarkSubtleError,
    onError = Color(0xFF3B1517),
    errorContainer = CalmDarkSubtleErrorContainer,
    onErrorContainer = Color(0xFFF3D4D4)
)

@Composable
fun SaviorTheme(
    themePreference: ThemePreference = ThemePreference.LIGHT,
    content: @Composable () -> Unit
) {
    val isDark = when (themePreference) {
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) SaviorDarkColorScheme else SaviorLightColorScheme
    val extendedColors = if (isDark) {
        SaviorExtendedColors(
            positive = CalmDarkPositive,
            positiveContainer = CalmDarkPositiveContainer,
            borderSubtle = CalmDarkBorder,
            warmAccent = Color(0xFFD2A388),
            warmContainer = Color(0xFF3A2A22),
            deepBlue = Color(0xFF9AC0E3)
        )
    } else {
        SaviorExtendedColors(
            positive = CalmPositive,
            positiveContainer = CalmPositiveContainer,
            borderSubtle = CalmSurfaceBorder,
            warmAccent = CalmWarmMuted,
            warmContainer = CalmWarmContainer,
            deepBlue = CalmDeepBlue
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(LocalSaviorColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SaviorTypography,
            shapes = SaviorShapes,
            content = content
        )
    }
}
