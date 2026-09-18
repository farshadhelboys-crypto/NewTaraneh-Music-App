package com.newtaraneh.music.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NeonBlue = Color(0xFF19A7FF)
val NeonCyan = Color(0xFF35E6FF)
val DeepBlack = Color(0xFF071019)
val CardDark = Color(0xFF101C29)
val SurfaceDark = Color(0xFF0D1722)
val TextMuted = Color(0xFF9AAABD)

private val DarkColorScheme = darkColorScheme(
    primary = NeonBlue,
    secondary = NeonCyan,
    tertiary = NeonCyan,
    background = DeepBlack,
    surface = SurfaceDark,
    surfaceVariant = CardDark,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = TextMuted
)

@Composable
fun NewTaranehTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
