// This file is the table of colour values from DESIGN.md; naming each literal would add nothing.
@file:Suppress("MagicNumber")

package net.drehtuer.nextgallery.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.drehtuer.nextgallery.core.model.AccentColor

private data class AccentRoles(val primary: Color, val onPrimary: Color, val container: Color, val onContainer: Color)

private fun lightRoles(accent: AccentColor) = when (accent) {
    AccentColor.BLUE -> AccentRoles(Color(0xFF00639B), Color.White, Color(0xFFCFE5FF), Color(0xFF001D33))
    AccentColor.TEAL -> AccentRoles(Color(0xFF00696E), Color.White, Color(0xFF9CF0F7), Color(0xFF002022))
    AccentColor.PLUM -> AccentRoles(Color(0xFF7B4E7F), Color.White, Color(0xFFFFD6FD), Color(0xFF30063A))
    AccentColor.MOSS -> AccentRoles(Color(0xFF4C6600), Color.White, Color(0xFFCCF087), Color(0xFF141F00))
    AccentColor.CORAL -> AccentRoles(Color(0xFFA23F2E), Color.White, Color(0xFFFFDAD3), Color(0xFF3D0600))
}

private fun darkRoles(accent: AccentColor) = when (accent) {
    AccentColor.BLUE -> AccentRoles(Color(0xFF99CBFF), Color(0xFF003354), Color(0xFF004A76), Color(0xFFCFE5FF))
    AccentColor.TEAL -> AccentRoles(Color(0xFF80D4DA), Color(0xFF00363A), Color(0xFF004F53), Color(0xFF9CF0F7))
    AccentColor.PLUM -> AccentRoles(Color(0xFFEBB5ED), Color(0xFF4A2050), Color(0xFF613766), Color(0xFFFFD6FD))
    AccentColor.MOSS -> AccentRoles(Color(0xFFB1D46E), Color(0xFF253500), Color(0xFF384D00), Color(0xFFCCF087))
    AccentColor.CORAL -> AccentRoles(Color(0xFFFFB4A6), Color(0xFF5F150A), Color(0xFF822819), Color(0xFFFFDAD3))
}

/**
 * The colour scheme for an accent (DESIGN.md). Roles the design does not name reuse the accent or a neutral, so that
 * no Material default hue can show up. `secondaryContainer` is the design's warning colour, not a second accent.
 */
fun nextGalleryColorScheme(accent: AccentColor, dark: Boolean): ColorScheme =
    if (dark) darkScheme(accent) else lightScheme(accent)

private fun lightScheme(accent: AccentColor): ColorScheme {
    val roles = lightRoles(accent)
    val surface = Color(0xFFF8F8F8)
    val container = Color(0xFFEDEDED)
    val containerHigh = Color(0xFFE4E4E4)
    val onSurface = Color(0xFF1B1B1B)
    return lightColorScheme(
        primary = roles.primary,
        onPrimary = roles.onPrimary,
        primaryContainer = roles.container,
        onPrimaryContainer = roles.onContainer,
        inversePrimary = darkRoles(accent).primary,
        secondary = roles.primary,
        onSecondary = roles.onPrimary,
        secondaryContainer = Color(0xFFFFDCBE),
        onSecondaryContainer = Color(0xFF2C1600),
        tertiary = roles.primary,
        onTertiary = roles.onPrimary,
        tertiaryContainer = roles.container,
        onTertiaryContainer = roles.onContainer,
        background = surface,
        onBackground = onSurface,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = Color(0xFFE2E2E2),
        onSurfaceVariant = Color(0xFF474747),
        surfaceTint = roles.primary,
        inverseSurface = Color(0xFF303030),
        inverseOnSurface = Color(0xFFF1F1F1),
        error = Color(0xFFA3161A),
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        outline = Color(0xFF767676),
        outlineVariant = Color(0xFFC6C6C6),
        scrim = Color.Black,
        surfaceBright = surface,
        surfaceDim = containerHigh,
        surfaceContainerLowest = surface,
        surfaceContainerLow = container,
        surfaceContainer = container,
        surfaceContainerHigh = containerHigh,
        surfaceContainerHighest = containerHigh,
    )
}

private fun darkScheme(accent: AccentColor): ColorScheme {
    val roles = darkRoles(accent)
    val surface = Color(0xFF121212)
    val container = Color(0xFF1E1E1E)
    val containerHigh = Color(0xFF2A2A2A)
    val onSurface = Color(0xFFE6E6E6)
    return darkColorScheme(
        primary = roles.primary,
        onPrimary = roles.onPrimary,
        primaryContainer = roles.container,
        onPrimaryContainer = roles.onContainer,
        inversePrimary = lightRoles(accent).primary,
        secondary = roles.primary,
        onSecondary = roles.onPrimary,
        secondaryContainer = Color(0xFF693C00),
        onSecondaryContainer = Color(0xFFFFDCBE),
        tertiary = roles.primary,
        onTertiary = roles.onPrimary,
        tertiaryContainer = roles.container,
        onTertiaryContainer = roles.onContainer,
        background = surface,
        onBackground = onSurface,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = Color(0xFF303030),
        onSurfaceVariant = Color(0xFFC6C6C6),
        surfaceTint = roles.primary,
        inverseSurface = onSurface,
        inverseOnSurface = Color(0xFF303030),
        error = Color(0xFFFF8E84),
        onError = Color(0xFF410002),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        outline = Color(0xFF929292),
        outlineVariant = Color(0xFF474747),
        scrim = Color.Black,
        surfaceBright = containerHigh,
        surfaceDim = surface,
        surfaceContainerLowest = surface,
        surfaceContainerLow = container,
        surfaceContainer = container,
        surfaceContainerHigh = containerHigh,
        surfaceContainerHighest = containerHigh,
    )
}

/** The always-dark scheme of the viewer and the editor. It keeps its own blue, whatever accent is chosen. */
fun viewerColorScheme(): ColorScheme = darkScheme(AccentColor.BLUE).copy(background = Color.Black)

/** The empty map while tiles load and the attribution's backing; tiles keep their own colours. */
@Immutable
data class NextGalleryMapColors(val land: Color, val water: Color)

fun nextGalleryMapColors(darkTheme: Boolean) = if (darkTheme) {
    NextGalleryMapColors(land = Color(0xFF202326), water = Color(0xFF1C3442))
} else {
    NextGalleryMapColors(land = Color(0xFFECE9E1), water = Color(0xFFAFCBDB))
}

/** Colours over photos, the same in every theme. */
object NextGalleryOverlayColors {
    val badgeBacking = Color.Black.copy(alpha = 0.6f)
    val badgeOn = Color.White
    val cropFrame = Color.White
    val cropScrim = Color.Black.copy(alpha = 0.6f)

    /** White, black, coral, orange, green and blue (DESIGN.md). */
    val penColors = listOf(
        Color.White,
        Color.Black,
        Color(0xFFFF7A66),
        Color(0xFFFF9800),
        Color(0xFF4CAF50),
        Color(0xFF2196F3),
    )
}
