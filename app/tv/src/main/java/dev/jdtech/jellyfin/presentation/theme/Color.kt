package dev.jdtech.jellyfin.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.darkColorScheme as darkColorSchemeTv

private val NetflixBlack = Color(0xFF141414)
private val NetflixSurface = Color(0xFF1A1A1A)
private val NetflixSurfaceVariant = Color(0xFF262626)
private val NetflixRed = Color(0xFFE50914)
private val NetflixWhite = Color(0xFFF5F5F1)
private val NetflixMuted = Color(0xFFB3B3B3)

val darkScheme =
    darkColorScheme(
        primary = NetflixRed,
        onPrimary = NetflixWhite,
        primaryContainer = Color(0xFF5C0A0F),
        onPrimaryContainer = NetflixWhite,
        secondary = NetflixMuted,
        onSecondary = NetflixBlack,
        secondaryContainer = NetflixSurfaceVariant,
        onSecondaryContainer = NetflixWhite,
        tertiary = NetflixWhite,
        onTertiary = NetflixBlack,
        tertiaryContainer = NetflixSurfaceVariant,
        onTertiaryContainer = NetflixWhite,
        error = Color(0xFFCF6679),
        onError = NetflixBlack,
        errorContainer = Color(0xFF8C1D2E),
        onErrorContainer = NetflixWhite,
        background = NetflixBlack,
        onBackground = NetflixWhite,
        surface = NetflixSurface,
        onSurface = NetflixWhite,
        surfaceVariant = NetflixSurfaceVariant,
        onSurfaceVariant = NetflixMuted,
        outline = Color(0xFF3D3D3D),
        outlineVariant = Color(0xFF303030),
        scrim = Color.Black,
        inverseSurface = NetflixWhite,
        inverseOnSurface = NetflixBlack,
        inversePrimary = Color(0xFFB20710),
        surfaceDim = Color(0xFF101010),
        surfaceBright = Color(0xFF2A2A2A),
        surfaceContainerLowest = Color(0xFF0F0F0F),
        surfaceContainerLow = Color(0xFF171717),
        surfaceContainer = Color(0xFF1E1E1E),
        surfaceContainerHigh = Color(0xFF242424),
        surfaceContainerHighest = Color(0xFF2B2B2B),
    )

val darkSchemeTv =
    darkColorSchemeTv(
        primary = NetflixRed,
        onPrimary = NetflixWhite,
        primaryContainer = Color(0xFF5C0A0F),
        onPrimaryContainer = NetflixWhite,
        secondary = NetflixMuted,
        onSecondary = NetflixBlack,
        secondaryContainer = NetflixSurfaceVariant,
        onSecondaryContainer = NetflixWhite,
        tertiary = NetflixWhite,
        onTertiary = NetflixBlack,
        tertiaryContainer = NetflixSurfaceVariant,
        onTertiaryContainer = NetflixWhite,
        error = Color(0xFFCF6679),
        onError = NetflixBlack,
        errorContainer = Color(0xFF8C1D2E),
        onErrorContainer = NetflixWhite,
        background = NetflixBlack,
        onBackground = NetflixWhite,
        surface = NetflixSurface,
        onSurface = NetflixWhite,
        surfaceVariant = NetflixSurfaceVariant,
        onSurfaceVariant = NetflixMuted,
        scrim = Color.Black,
        inverseSurface = NetflixWhite,
        inverseOnSurface = NetflixBlack,
        inversePrimary = Color(0xFFB20710),
    )
