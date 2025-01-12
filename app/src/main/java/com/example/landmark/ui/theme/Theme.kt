package com.example.landmark.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable

// Define your color schemes for Dark and Light themes using Material3
private val DarkColorPalette = darkColors(
    primary = Purple40,
    secondary = Pink40
)

private val LightColorPalette = lightColors(
    primary = Purple80,
    secondary = Pink80,
    // You can further customize other colors (background, surface, etc.) as needed
)

@Composable
fun LandmarkTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    // Use Material3's MaterialTheme with the new color schemes
    MaterialTheme(
        colors = colors,   // Use colorScheme instead of colors
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}