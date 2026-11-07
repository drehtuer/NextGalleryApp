package net.drehtuer.nextgallery.feature.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AboutScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val info = AboutInfo(
        versionName = "1.2.3",
        buildNumber = 45,
        iconBackground = R.drawable.feature_settings_ic_info,
        iconForeground = R.drawable.feature_settings_ic_info,
    )
    private var openedUri: String? = null
    private var licencesOpened = 0
    private var backClicks = 0

    private fun show() = composeRule.setContent {
        val uriHandler = object : UriHandler {
            override fun openUri(uri: String) {
                openedUri = uri
            }
        }
        CompositionLocalProvider(LocalUriHandler provides uriHandler) {
            AboutScreen(info, onBack = { backClicks++ }, onOpenLicences = { licencesOpened++ })
        }
    }

    @Test
    fun about_showsNameAndTagline() {
        show()

        composeRule.onNodeWithText("NextGallery").assertIsDisplayed()
        composeRule.onNodeWithText("Free software, GPL-3.0. No tracking.").assertIsDisplayed()
    }

    @Test
    fun about_showsVersionAndBuildNumber() {
        show()

        composeRule.onNodeWithText("1.2.3 (45)").assertIsDisplayed()
    }

    @Test
    fun about_showsAttribution() {
        show()

        composeRule
            .onNodeWithText(
                "Map data © OpenStreetMap contributors, available under the Open Database Licence. " +
                    "Place names from GeoNames (geonames.org), CC BY 4.0, looked up on the phone.",
            )
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun sourceCodeClicked_opensRepository() {
        show()

        composeRule.onNodeWithText("Source code").performScrollTo().performClick()

        assertEquals("https://github.com/drehtuer/NextGalleryApp", openedUri)
    }

    @Test
    fun licencesClicked_opensLicences() {
        show()

        composeRule.onNodeWithText("Open-source licences").performScrollTo().performClick()

        assertEquals(1, licencesOpened)
    }

    @Test
    fun backClicked_goesBack() {
        show()

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, backClicks)
    }
}
