package com.rishavmishra010.railcam.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary        = Amber,
    onPrimary      = Color.Black,
    secondary      = AmberDark,
    onSecondary    = Color.Black,
    background     = Background,
    onBackground   = TextPrimary,
    surface        = Surface,
    onSurface      = TextPrimary,
    surfaceVariant = SurfaceHigh,
    onSurfaceVariant = TextSecondary,
    outline        = BorderColor,
    error          = AlertRed,
    onError        = Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary        = Amber,
    onPrimary      = Color.Black,
    secondary      = AmberDark,
    onSecondary    = Color.Black,
    background     = LightBackground,
    onBackground   = LightTextPrimary,
    surface        = LightSurface,
    onSurface      = LightTextPrimary,
    surfaceVariant = LightSurfaceHigh,
    onSurfaceVariant = LightTextSecondary,
    outline        = LightBorderColor,
    error          = AlertRed,
    onError        = Color.White,
)

@Composable
fun RailCamTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
