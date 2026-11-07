package net.drehtuer.nextgallery.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.drehtuer.nextgallery.core.model.AccentColor
import net.drehtuer.nextgallery.core.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class NextGalleryThemeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun nextGalleryTheme_rendersContent() {
        composeRule.setContent {
            NextGalleryTheme { Text("content") }
        }

        composeRule.onNodeWithText("content").assertIsDisplayed()
    }

    @Test
    fun nextGalleryTheme_accent_setsPrimary() {
        var primary: Color? = null
        composeRule.setContent {
            NextGalleryTheme(darkTheme = false, accent = AccentColor.MOSS) {
                primary = MaterialTheme.colorScheme.primary
            }
        }

        assertEquals(Color(0xFF4C6600), primary)
    }

    @Test
    fun nextGalleryViewerTheme_setsBlackBackground() {
        var background: Color? = null
        composeRule.setContent {
            NextGalleryViewerTheme { background = MaterialTheme.colorScheme.background }
        }

        assertEquals(Color.Black, background)
    }

    @Test
    fun nextGalleryTheme_appliesTypeScale() {
        var size: TextUnit? = null
        composeRule.setContent {
            NextGalleryTheme { size = MaterialTheme.typography.bodySmall.fontSize }
        }

        assertEquals(13.sp, size)
    }

    @Test
    fun dimensions_insideTheme_areProvided() {
        var dimensions: NextGalleryDimensions? = null
        composeRule.setContent {
            NextGalleryTheme { dimensions = NextGalleryTheme.dimensions }
        }

        assertEquals(NextGalleryDimensions(), dimensions)
    }

    @Test
    fun nextGalleryTheme_gridColumns_setsDimension() {
        var columns: Int? = null
        composeRule.setContent {
            NextGalleryTheme(gridColumns = 5) { columns = NextGalleryTheme.dimensions.gridColumns }
        }

        assertEquals(5, columns)
    }

    @Test
    @Config(qualifiers = "night")
    fun isDark_light_isFalseEvenOnDarkSystem() {
        assertFalse(isDarkOn(ThemeMode.LIGHT))
    }

    @Test
    @Config(qualifiers = "notnight")
    fun isDark_dark_isTrueEvenOnLightSystem() {
        assertTrue(isDarkOn(ThemeMode.DARK))
    }

    @Test
    @Config(qualifiers = "night")
    fun isDark_systemOnDarkSystem_isTrue() {
        assertTrue(isDarkOn(ThemeMode.SYSTEM))
    }

    @Test
    @Config(qualifiers = "notnight")
    fun isDark_systemOnLightSystem_isFalse() {
        assertFalse(isDarkOn(ThemeMode.SYSTEM))
    }

    private fun isDarkOn(mode: ThemeMode): Boolean {
        var dark: Boolean? = null
        composeRule.setContent { dark = mode.isDark() }
        return dark == true
    }
}
