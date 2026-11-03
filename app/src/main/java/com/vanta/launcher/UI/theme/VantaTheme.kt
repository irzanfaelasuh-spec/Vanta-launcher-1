package com.vanta.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val VantaBackground = Color(0xFF101114)
val VantaSurface = Color(0xFF191B20)
val VantaPanel = Color(0xFF25282E)
val VantaPanelSoft = Color(0xFF30343B)
val VantaText = Color(0xFFF2F3F5)
val VantaMuted = Color(0xFFA2A7B0)
val VantaAccent = Color(0xFF83D99A)
val VantaAccentDark = Color(0xFF153C27)
val VantaBorder = Color(0xFF3A3E46)
val VantaDanger = Color(0xFFE17D7D)

private val DarkScheme = darkColorScheme(
    primary = VantaAccent,
    onPrimary = Color(0xFF0C2415),
    secondary = Color(0xFFB6C7BB),
    background = VantaBackground,
    onBackground = VantaText,
    surface = VantaSurface,
    onSurface = VantaText,
    surfaceVariant = VantaPanel,
    onSurfaceVariant = VantaMuted,
    outline = VantaBorder,
    error = VantaDanger
)

@Composable
fun VantaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, content = content)
}
