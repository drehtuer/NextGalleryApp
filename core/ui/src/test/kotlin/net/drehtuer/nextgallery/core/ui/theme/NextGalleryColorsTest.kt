package net.drehtuer.nextgallery.core.ui.theme

import androidx.compose.ui.graphics.Color
import net.drehtuer.nextgallery.core.model.AccentColor
import org.junit.Assert.assertEquals
import org.junit.Test

class NextGalleryColorsTest {
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
}
