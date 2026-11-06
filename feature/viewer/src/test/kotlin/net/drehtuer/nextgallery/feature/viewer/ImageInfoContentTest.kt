package net.drehtuer.nextgallery.feature.viewer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.drehtuer.nextgallery.core.data.media.ImageDetailsMapper
import net.drehtuer.nextgallery.core.model.GeoPoint
import net.drehtuer.nextgallery.core.model.ImageDetails
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageInfoContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val details: ImageDetails = ImageDetailsMapper.from(null, mediaItem(1))

    @Test
    fun imageInfo_showsFileValues() {
        composeRule.setContent { ImageInfoContent(details) }

        composeRule.onNodeWithText("IMG_1.jpg").assertIsDisplayed()
    }

    @Test
    fun imageInfo_showsResolution() {
        composeRule.setContent { ImageInfoContent(details.copy(width = 4000, height = 3000)) }

        composeRule.onNodeWithText("4000 × 3000").assertIsDisplayed()
    }

    @Test
    fun imageInfo_withCamera_showsIt() {
        composeRule.setContent { ImageInfoContent(details.copy(camera = "Pixel 10a")) }

        composeRule.onNodeWithText("Pixel 10a").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun imageInfo_withoutCamera_hidesLabel() {
        composeRule.setContent { ImageInfoContent(details) }

        composeRule.onNodeWithText("Camera").assertDoesNotExist()
    }

    @Test
    fun imageInfo_withLocation_showsCoordinates() {
        composeRule.setContent { ImageInfoContent(details.copy(location = GeoPoint(48.1, 11.5))) }

        composeRule.onNodeWithText("48.1, 11.5").performScrollTo().assertIsDisplayed()
    }
}
