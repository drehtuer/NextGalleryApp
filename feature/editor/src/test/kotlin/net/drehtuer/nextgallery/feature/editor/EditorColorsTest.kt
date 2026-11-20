package net.drehtuer.nextgallery.feature.editor

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.drehtuer.nextgallery.core.model.edit.AspectRatio
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** The editor's controls use the accent, never Material's `secondaryContainer`, which is the warning colour here. */
@RunWith(AndroidJUnit4::class)
class EditorColorsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE) // Draws the chip, so its pixels can be read.
    fun aspectChips_fillTheChosenChipWithTheAccentContainer() {
        var accentContainer = Color.Unspecified
        composeRule.setContent {
            NextGalleryTheme(darkTheme = true) {
                accentContainer = MaterialTheme.colorScheme.primaryContainer
                AspectChips(selected = AspectRatio.FREE, enabled = true, onSelect = {})
            }
        }

        val chip = composeRule.onNodeWithText("Free").captureToImage().toPixelMap()
        // The padding left of the label, clear of the rounded corners.
        assertEquals(accentContainer, chip[chip.width / 12, chip.height / 2])
    }

    @Test
    fun penWidthSliderColors_drawTheRestOfTheTrackInTheAccentContainer() {
        lateinit var colors: SliderColors
        var accentContainer = Color.Unspecified
        composeRule.setContent {
            NextGalleryTheme(darkTheme = true) {
                colors = penWidthSliderColors()
                accentContainer = MaterialTheme.colorScheme.primaryContainer
            }
        }

        assertEquals(accentContainer, colors.inactiveTrackColor)
    }
}
