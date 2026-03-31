package me.lemonhall.worddragon.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WordDragonColorScheme =
    lightColorScheme(
        primary = DragonInk,
        onPrimary = Color.White,
        primaryContainer = DragonGold,
        onPrimaryContainer = DragonInk,
        secondary = DragonGreen,
        onSecondary = Color.White,
        tertiary = DragonOrange,
        onTertiary = Color.White,
        background = DragonCream,
        onBackground = DragonInk,
        surface = DragonCard,
        onSurface = DragonInk,
        surfaceVariant = DragonCard,
        onSurfaceVariant = DragonMutedText,
        outline = DragonOutline,
    )

@Composable
fun WordDragonTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WordDragonColorScheme,
        typography = WordDragonTypography,
        content = content,
    )
}
