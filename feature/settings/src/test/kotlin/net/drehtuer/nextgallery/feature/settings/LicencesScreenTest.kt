package net.drehtuer.nextgallery.feature.settings

import android.content.res.Resources
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import net.drehtuer.nextgallery.core.data.settings.LibraryLicence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LicencesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val coil = LibraryLicence("io.coil:coil", "Coil", "3.6.3", listOf("Apache-2.0"))

    @Test
    fun library_showsNameVersionAndLicence() {
        composeRule.setContent { LicencesScreen(listOf(coil), onBack = {}) }

        composeRule.onNodeWithText("Coil").assertIsDisplayed()
        composeRule.onNodeWithText("3.6.3 · Apache-2.0").assertIsDisplayed()
    }

    @Test
    fun library_withSeveralLicences_listsThemAll() {
        val both = coil.copy(licences = listOf("MIT", "Apache-2.0"))
        composeRule.setContent { LicencesScreen(listOf(both), onBack = {}) }

        composeRule.onNodeWithText("3.6.3 · MIT, Apache-2.0").assertIsDisplayed()
    }

    @Test
    fun geoNames_isAttributedBesidesTheLibraries() {
        composeRule.setContent { LicencesScreen(emptyList(), onBack = {}) }

        composeRule.onNodeWithText("GeoNames place data").assertIsDisplayed()
        composeRule.onNodeWithText("CC BY 4.0").assertIsDisplayed()
    }

    @Test
    fun openStreetMap_isAttributedBesidesTheLibraries() {
        composeRule.setContent { LicencesScreen(emptyList(), onBack = {}) }

        composeRule.onNodeWithText("OpenStreetMap map data").assertIsDisplayed()
        composeRule.onNodeWithText("ODbL 1.0").assertIsDisplayed()
    }

    @Test
    fun whileLoading_showsTheProgressIndicator() {
        composeRule.setContent { LicencesScreen(libraries = null, onBack = {}) }

        composeRule.onNodeWithContentDescription("Loading").assertExists()
    }

    @Test
    fun longList_canBeScrolledToItsEnd() {
        val many = (1..100).map { coil.copy(id = "id$it", name = "Library $it") }
        composeRule.setContent { LicencesScreen(many, onBack = {}) }

        composeRule.onNodeWithTag(LICENCES_LIST_TAG).performScrollToNode(hasText("Library 100"))

        composeRule.onNodeWithText("Library 100").assertIsDisplayed()
    }

    @Test
    fun backClicked_goesBack() {
        var clicks = 0
        composeRule.setContent { LicencesScreen(emptyList(), onBack = { clicks++ }) }

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun rememberLibraries_startsEmptyAndThenReadsTheResource() {
        val json = """{"libraries":[{"uniqueId":"a:b","artifactVersion":"1","name":"Lib","licenses":["MIT"]}]}"""
        val resources = mockk<Resources> { every { openRawResource(RAW_ID) } returns json.byteInputStream() }
        var libraries: List<LibraryLicence>? = emptyList()
        var first = true
        composeRule.setContent {
            CompositionLocalProvider(LocalResources provides resources) {
                libraries = rememberLibraries(RAW_ID)
                if (first) {
                    assertNull(libraries)
                    first = false
                }
            }
        }

        composeRule.waitUntil { libraries != null }

        assertEquals(listOf("Lib"), libraries?.map { it.name })
    }

    private companion object {
        const val RAW_ID = 1234
    }
}
