package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val CodePulseDarkColorScheme = darkColorScheme(
    primary = ElectricIndigoLight,
    onPrimary = Color(0xFF090D16),
    primaryContainer = ElectricIndigoContainer,
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = EmeraldSuccess,
    onSecondary = Color(0xFF052E16),
    secondaryContainer = EmeraldContainer,
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = CyanSpark,
    onTertiary = Color(0xFF082F49),
    tertiaryContainer = CyanContainer,
    onTertiaryContainer = Color(0xFFE0F2FE),
    background = ObsidianCanvas,
    onBackground = TextPrimary,
    surface = CarbonSurface,
    onSurface = TextPrimary,
    surfaceVariant = CodeEditorSurface,
    onSurfaceVariant = TextSecondary,
    outline = SubtleBorder,
    outlineVariant = Color(0xFF1E293B),
    error = CoralError,
    onError = Color.White,
    errorContainer = CoralContainer,
    onErrorContainer = Color(0xFFFECACA)
)

val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = CodePulseDarkColorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
