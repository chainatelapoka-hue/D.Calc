package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.data.local.AppThemeColor

val LocalAppPalette = staticCompositionLocalOf { MidnightNavyPalette }

@Composable
fun NexoraTheme(
    themeName: String = AppThemeColor.MIDNIGHT_NAVY.name,
    content: @Composable () -> Unit
) {
    val palette = getThemePalette(themeName)
    val colorScheme = darkColorScheme(
        primary = palette.accent,
        onPrimary = palette.mainBg,
        primaryContainer = palette.primaryOp,
        onPrimaryContainer = palette.mainText,
        secondary = palette.secondaryButton,
        onSecondary = palette.mainText,
        secondaryContainer = palette.surfaceVariant,
        onSecondaryContainer = palette.mainText,
        tertiary = palette.equalButton,
        onTertiary = palette.equalText,
        background = palette.mainBg,
        onBackground = palette.mainText,
        surface = palette.secondaryBg,
        onSurface = palette.mainText,
        surfaceVariant = palette.surfaceVariant,
        onSurfaceVariant = palette.secondaryText,
        outline = palette.border,
        outlineVariant = palette.border,
        error = palette.danger,
        onError = palette.mainBg
    )

    CompositionLocalProvider(LocalAppPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
