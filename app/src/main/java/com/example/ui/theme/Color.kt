package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.example.data.local.AppThemeColor

// Dynamic Theme Color Tokens backed by LocalAppPalette
val NexoraMainBg: Color @Composable @ReadOnlyComposable get() = LocalAppPalette.current.mainBg
val NexoraSecondaryBg: Color @Composable @ReadOnlyComposable get() = LocalAppPalette.current.secondaryBg
val NexoraSurfaceVariant: Color @Composable @ReadOnlyComposable get() = LocalAppPalette.current.surfaceVariant
val NexoraBorder: Color @Composable @ReadOnlyComposable get() = LocalAppPalette.current.border

// Button Tones
val NexoraCalcButton: Color @Composable @ReadOnlyComposable get() = LocalAppPalette.current.calcButton
val NexoraSecondaryButton: Color @Composable @ReadOnlyComposable get() = LocalAppPalette.current.secondaryButton
val NexoraPrimaryOp: Color @Composable @ReadOnlyComposable get() = LocalAppPalette.current.primaryOp
val NexoraAccentBlue: Color @Composable @ReadOnlyComposable get() = LocalAppPalette.current.accent
val NexoraEqualButton: Color @Composable @ReadOnlyComposable get() = LocalAppPalette.current.equalButton
val NexoraEqualText: Color @Composable @ReadOnlyComposable get() = LocalAppPalette.current.equalText

// Text Tones
val NexoraMainText: Color @Composable @ReadOnlyComposable get() = LocalAppPalette.current.mainText
val NexoraSecondaryText: Color @Composable @ReadOnlyComposable get() = LocalAppPalette.current.secondaryText
val NexoraMutedText: Color @Composable @ReadOnlyComposable get() = LocalAppPalette.current.mutedText

// Functional
val NexoraSuccess: Color @Composable @ReadOnlyComposable get() = LocalAppPalette.current.success
val NexoraWarning: Color @Composable @ReadOnlyComposable get() = LocalAppPalette.current.warning
val NexoraDanger: Color @Composable @ReadOnlyComposable get() = LocalAppPalette.current.danger
val NexoraGold = Color(0xFFFCD34D)

data class CustomThemePalette(
    val mainBg: Color,
    val secondaryBg: Color,
    val surfaceVariant: Color,
    val border: Color,
    val calcButton: Color,
    val secondaryButton: Color,
    val primaryOp: Color,
    val accent: Color,
    val equalButton: Color,
    val equalText: Color,
    val mainText: Color,
    val secondaryText: Color,
    val mutedText: Color,
    val success: Color = Color(0xFF34D399),
    val warning: Color = Color(0xFFFBBF24),
    val danger: Color = Color(0xFFF87171)
)

val MidnightNavyPalette = CustomThemePalette(
    mainBg = Color(0xFF0B0F16),
    secondaryBg = Color(0xFF151B27),
    surfaceVariant = Color(0xFF1A2234),
    border = Color(0xFF263248),
    calcButton = Color(0xFF292F3B),
    secondaryButton = Color(0xFF3B4353),
    primaryOp = Color(0xFF075985),
    accent = Color(0xFF38BDF8),
    equalButton = Color(0xFF93C5FD),
    equalText = Color(0xFF0B0F16),
    mainText = Color(0xFFF8FAFC),
    secondaryText = Color(0xFF94A3B8),
    mutedText = Color(0xFF64748B)
)

val ObsidianAmoledPalette = CustomThemePalette(
    mainBg = Color(0xFF000000),
    secondaryBg = Color(0xFF121212),
    surfaceVariant = Color(0xFF1E1E1E),
    border = Color(0xFF2E2E2E),
    calcButton = Color(0xFF242424),
    secondaryButton = Color(0xFF363636),
    primaryOp = Color(0xFF1E3A8A),
    accent = Color(0xFF60A5FA),
    equalButton = Color(0xFF93C5FD),
    equalText = Color(0xFF000000),
    mainText = Color(0xFFF8FAFC),
    secondaryText = Color(0xFFA3A3A3),
    mutedText = Color(0xFF737373)
)

val DeepEmeraldPalette = CustomThemePalette(
    mainBg = Color(0xFF061412),
    secondaryBg = Color(0xFF0D231F),
    surfaceVariant = Color(0xFF13332D),
    border = Color(0xFF1C4840),
    calcButton = Color(0xFF183D36),
    secondaryButton = Color(0xFF245048),
    primaryOp = Color(0xFF065F46),
    accent = Color(0xFF10B981),
    equalButton = Color(0xFF6EE7B7),
    equalText = Color(0xFF061412),
    mainText = Color(0xFFF8FAFC),
    secondaryText = Color(0xFF86EFAC),
    mutedText = Color(0xFF52796F)
)

val RoyalAmethystPalette = CustomThemePalette(
    mainBg = Color(0xFF0F0A1C),
    secondaryBg = Color(0xFF1A122E),
    surfaceVariant = Color(0xFF241A3E),
    border = Color(0xFF37285C),
    calcButton = Color(0xFF291E44),
    secondaryButton = Color(0xFF3D2E63),
    primaryOp = Color(0xFF6B21A8),
    accent = Color(0xFFA855F7),
    equalButton = Color(0xFFD8B4FE),
    equalText = Color(0xFF0F0A1C),
    mainText = Color(0xFFF8FAFC),
    secondaryText = Color(0xFFC084FC),
    mutedText = Color(0xFF7E6B9E)
)

val GoldenAmberPalette = CustomThemePalette(
    mainBg = Color(0xFF120E08),
    secondaryBg = Color(0xFF1F190E),
    surfaceVariant = Color(0xFF2C2314),
    border = Color(0xFF40331C),
    calcButton = Color(0xFF302516),
    secondaryButton = Color(0xFF453620),
    primaryOp = Color(0xFF78350F),
    accent = Color(0xFFF59E0B),
    equalButton = Color(0xFFFDE68A),
    equalText = Color(0xFF120E08),
    mainText = Color(0xFFF8FAFC),
    secondaryText = Color(0xFFFCD34D),
    mutedText = Color(0xFF8C795E)
)

val CrimsonRubyPalette = CustomThemePalette(
    mainBg = Color(0xFF140A0F),
    secondaryBg = Color(0xFF22111A),
    surfaceVariant = Color(0xFF2F1724),
    border = Color(0xFF472337),
    calcButton = Color(0xFF381B2B),
    secondaryButton = Color(0xFF4F263C),
    primaryOp = Color(0xFF881337),
    accent = Color(0xFFF43F5E),
    equalButton = Color(0xFFFECDD3),
    equalText = Color(0xFF140A0F),
    mainText = Color(0xFFF8FAFC),
    secondaryText = Color(0xFFFDA4AF),
    mutedText = Color(0xFF946279)
)

fun getThemePalette(themeName: String): CustomThemePalette {
    return when (themeName) {
        AppThemeColor.OBSIDIAN_AMOLED.name -> ObsidianAmoledPalette
        AppThemeColor.DEEP_EMERALD.name -> DeepEmeraldPalette
        AppThemeColor.ROYAL_AMETHYST.name -> RoyalAmethystPalette
        AppThemeColor.GOLDEN_AMBER.name -> GoldenAmberPalette
        AppThemeColor.CRIMSON_RUBY.name -> CrimsonRubyPalette
        else -> MidnightNavyPalette
    }
}
