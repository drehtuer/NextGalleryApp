package net.drehtuer.nextgallery.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import net.drehtuer.nextgallery.core.model.AccentColor
import org.junit.Assert.assertEquals
import org.junit.Test

class NextGalleryColorsTest {
    @Test
    fun mapColors_light_matchDesign() {
        assertEquals(
            NextGalleryMapColors(Color(0xFFECE9E1), Color(0xFFAFCBDB)),
            nextGalleryMapColors(darkTheme = false),
        )
    }

    @Test
    fun mapColors_dark_matchDesign() {
        assertEquals(NextGalleryMapColors(Color(0xFF202326), Color(0xFF1C3442)), nextGalleryMapColors(darkTheme = true))
    }

    @Test
    fun colorScheme_lightAccent_usesItsPrimary() {
        assertEquals(Color(0xFF00696E), nextGalleryColorScheme(AccentColor.TEAL, dark = false).primary)
    }

    @Test
    fun colorScheme_darkAccent_usesItsPrimary() {
        assertEquals(Color(0xFFFFB4A6), nextGalleryColorScheme(AccentColor.CORAL, dark = true).primary)
    }

    @Test
    fun colorScheme_inversePrimary_comesFromOppositeTheme() {
        assertEquals(Color(0xFF99CBFF), nextGalleryColorScheme(AccentColor.BLUE, dark = false).inversePrimary)
    }

    @Test
    fun colorScheme_everyAccentHasDifferentLightAndDarkPrimary() {
        AccentColor.entries.forEach { accent ->
            val light = nextGalleryColorScheme(accent, dark = false).primary
            val dark = nextGalleryColorScheme(accent, dark = true).primary

            assert(light != dark) { accent }
        }
    }

    @Test
    fun colorScheme_errorDoesNotChangeWithAccent() {
        assertEquals(
            nextGalleryColorScheme(AccentColor.BLUE, dark = false).error,
            nextGalleryColorScheme(AccentColor.PLUM, dark = false).error,
        )
    }

    @Test
    fun colorScheme_lightSurface_isNeutralGrey() {
        assertEquals(Color(0xFFF8F8F8), nextGalleryColorScheme(AccentColor.BLUE, dark = false).surface)
    }

    @Test
    fun viewerColorScheme_backgroundIsBlack() {
        assertEquals(Color.Black, viewerColorScheme().background)
    }

    @Test
    fun viewerColorScheme_keepsItsOwnBlue() {
        assertEquals(Color(0xFF99CBFF), viewerColorScheme().primary)
    }

    @Test
    fun colorScheme_textRolesHaveEnoughContrastInEveryAccentAndMode() {
        val tooLow = AccentColor.entries.flatMap { accent ->
            listOf(false, true).flatMap { dark ->
                val colors = nextGalleryColorScheme(accent, dark)
                listOf(
                    "onSurface" to contrast(colors.onSurface, colors.surface),
                    "onSurfaceVariant" to contrast(colors.onSurfaceVariant, colors.surface),
                    "onPrimary" to contrast(colors.onPrimary, colors.primary),
                    "onPrimaryContainer" to contrast(colors.onPrimaryContainer, colors.primaryContainer),
                    "onErrorContainer" to contrast(colors.onErrorContainer, colors.errorContainer),
                ).filter { (_, ratio) -> ratio < WCAG_AA_TEXT }.map { "$accent dark=$dark ${it.first} ${it.second}" }
            }
        }

        assertEquals(emptyList<String>(), tooLow)
    }

    /** The WCAG contrast ratio. */
    private fun contrast(foreground: Color, background: Color): Double {
        val lighter = maxOf(foreground.luminance(), background.luminance())
        val darker = minOf(foreground.luminance(), background.luminance())
        return (lighter + 0.05) / (darker + 0.05)
    }

    private companion object {
        const val WCAG_AA_TEXT = 4.5
    }
}
