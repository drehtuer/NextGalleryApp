package net.drehtuer.nextgallery.feature.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.drehtuer.nextgallery.core.ui.theme.NextGalleryTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsComponentsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun segmentColors_fillTheChosenSegmentWithTheAccentContainer() {
        lateinit var colors: SegmentedButtonColors
        var accentContainer = Color.Unspecified
        composeRule.setContent {
            NextGalleryTheme(darkTheme = true) {
                colors = segmentColors()
                accentContainer = MaterialTheme.colorScheme.primaryContainer
            }
        }

        assertEquals(accentContainer, colors.activeContainerColor)
    }
}
