package com.himma.app.core.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Himma Emerald Brand Tokens (aligned with assets/logo/)
val HimmaEmeraldLight = Color(0xFF16A34A)
val HimmaEmeraldBright = Color(0xFF22C55E)
val HimmaEmeraldDeep = Color(0xFF15803D)
val HimmaEmeraldSoft = Color(0xFF4ADE80)
val HimmaMintContainerDark = Color(0xFF0F3D22)
val HimmaMintContainerLight = Color(0xFFDCFCE7)

// AMOLED Dark Surface Palette
val AmoledBlack = Color(0xFF000000)
val AmoledSurface = Color(0xFF0A0F0D)
val AmoledSurfaceContainerLow = Color(0xFF0E1512)
val AmoledSurfaceContainer = Color(0xFF121B17)
val AmoledSurfaceContainerHigh = Color(0xFF19241F)
val AmoledSurfaceContainerHighest = Color(0xFF212E28)

// Standard Himma Dark Surface Palette (matches #111816 logo background)
val HimmaDarkBg = Color(0xFF111816)
val HimmaDarkSurfaceContainerLow = Color(0xFF16201D)
val HimmaDarkSurfaceContainer = Color(0xFF1B2723)
val HimmaDarkSurfaceContainerHigh = Color(0xFF23322D)
val HimmaDarkSurfaceContainerHighest = Color(0xFF2B3D37)

val HimmaAmoledDarkColorScheme = darkColorScheme(
    primary = HimmaEmeraldBright,
    onPrimary = Color(0xFF00210C),
    primaryContainer = HimmaMintContainerDark,
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = Color(0xFF86EFAC),
    onSecondary = Color(0xFF052E16),
    secondaryContainer = Color(0xFF183626),
    onSecondaryContainer = Color(0xFFBBF7D0),
    tertiary = Color(0xFF38BDF8),
    onTertiary = Color(0xFF002538),
    tertiaryContainer = Color(0xFF0C3449),
    onTertiaryContainer = Color(0xFFBAE6FD),
    background = AmoledBlack,
    onBackground = Color(0xFFF1F5F3),
    surface = AmoledBlack,
    onSurface = Color(0xFFF1F5F3),
    surfaceVariant = AmoledSurfaceContainerHigh,
    onSurfaceVariant = Color(0xFF9FB3A9),
    surfaceContainerLowest = AmoledBlack,
    surfaceContainerLow = AmoledSurfaceContainerLow,
    surfaceContainer = AmoledSurfaceContainer,
    surfaceContainerHigh = AmoledSurfaceContainerHigh,
    surfaceContainerHighest = AmoledSurfaceContainerHighest,
    outline = Color(0xFF374A41),
    outlineVariant = Color(0xFF223029),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
)

val HimmaDarkColorScheme = darkColorScheme(
    primary = HimmaEmeraldBright,
    onPrimary = Color(0xFF00210C),
    primaryContainer = HimmaMintContainerDark,
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = Color(0xFF86EFAC),
    onSecondary = Color(0xFF052E16),
    secondaryContainer = Color(0xFF1D3B2B),
    onSecondaryContainer = Color(0xFFBBF7D0),
    tertiary = Color(0xFF38BDF8),
    onTertiary = Color(0xFF002538),
    tertiaryContainer = Color(0xFF0C3449),
    onTertiaryContainer = Color(0xFFBAE6FD),
    background = HimmaDarkBg,
    onBackground = Color(0xFFF1F5F3),
    surface = HimmaDarkBg,
    onSurface = Color(0xFFF1F5F3),
    surfaceVariant = HimmaDarkSurfaceContainerHigh,
    onSurfaceVariant = Color(0xFFA7BAB1),
    surfaceContainerLowest = Color(0xFF0C1210),
    surfaceContainerLow = HimmaDarkSurfaceContainerLow,
    surfaceContainer = HimmaDarkSurfaceContainer,
    surfaceContainerHigh = HimmaDarkSurfaceContainerHigh,
    surfaceContainerHighest = HimmaDarkSurfaceContainerHighest,
    outline = Color(0xFF3D5248),
    outlineVariant = Color(0xFF283730),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
)

val HimmaLightColorScheme = lightColorScheme(
    primary = HimmaEmeraldLight,
    onPrimary = Color.White,
    primaryContainer = HimmaMintContainerLight,
    onPrimaryContainer = Color(0xFF052E16),
    secondary = HimmaEmeraldDeep,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2F5EA),
    onSecondaryContainer = Color(0xFF0B3B1E),
    tertiary = Color(0xFF0284C7),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0F2FE),
    onTertiaryContainer = Color(0xFF082F49),
    background = Color(0xFFF7FAF8),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFF7FAF8),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE4ECE7),
    onSurfaceVariant = Color(0xFF475569),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF0F5F2),
    surfaceContainer = Color(0xFFEAF1ED),
    surfaceContainerHigh = Color(0xFFE2EBE6),
    surfaceContainerHighest = Color(0xFFD9E4DE),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
)
