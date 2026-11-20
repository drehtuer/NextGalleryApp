package net.drehtuer.nextgallery.feature.remote

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.ImageLoader
import coil3.serviceLoaderEnabled
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteDownloadProgress
import net.drehtuer.nextgallery.core.ui.remote.LocalRemoteImageLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RemoteViewerScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<RemoteViewerEvent>()
    private var closed = false
    private val loaded = RemoteViewerUiState(
        images = listOf(IMAGE_A, IMAGE_B),
        initialIndex = 0,
        currentIndex = 0,
        urls = FakeUrls,
    )

    private fun show(state: RemoteViewerUiState) = composeRule.setContent {
        RemoteViewerScreen(
            state = state,
            onEvent = { events += it },
            onBack = { closed = true },
            pageContent = { item, onTap -> Text("Page ${item.name}", Modifier.clickable(onClick = onTap)) },
        )
    }

    @Test
    fun loaded_showsThePageAndTheFileName() {
        show(loaded)

        composeRule.onNodeWithText("Page a.jpg").assertIsDisplayed()
        composeRule.onNodeWithText("a.jpg").assertIsDisplayed()
    }

    @Test
    fun loaded_reportsTheCurrentPage() {
        show(loaded)

        composeRule.runOnIdle { assertTrue(RemoteViewerEvent.PageChanged(0) in events) }
    }

    @Test
    fun pageTapped_togglesTheBar() {
        show(loaded)

        composeRule.onNodeWithText("Page a.jpg").performClick()

        assertTrue(RemoteViewerEvent.ToggleChrome in events)
    }

    @Test
    fun chromeHidden_hasNoBar() {
        show(loaded.copy(chromeVisible = false))

        composeRule.onNodeWithContentDescription("Back").assertDoesNotExist()
    }

    @Test
    fun backClicked_closes() {
        show(loaded)

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertTrue(closed)
    }

    @Test
    fun detailsClicked_showsTheInfo() {
        show(loaded)

        composeRule.onNodeWithContentDescription("Details").performClick()

        assertTrue(RemoteViewerEvent.ShowInfo in events)
    }

    @Test
    fun progress_showsTheWordingOfTheDesign() {
        val total = (6.1f * 1024 * 1024).toLong()
        val read = (2.4f * 1024 * 1024).toLong()
        show(loaded.copy(progress = RemoteDownloadProgress(read, total)))

        composeRule.onNodeWithText("Loading full image · 2.4 of 6.1 MB").assertIsDisplayed()
    }

    @Test
    fun progress_unknownTotal_showsOnlyWhatArrived() {
        show(loaded.copy(progress = RemoteDownloadProgress(1024 * 1024, -1)))

        composeRule.onNodeWithText("Loading full image · 1.0 MB").assertIsDisplayed()
    }

    @Test
    fun originalFailedBar_tryAgain_retries() {
        var retried = false
        composeRule.setContent { OriginalFailedBar(onRetry = { retried = true }) }

        composeRule.onNodeWithText("Try again").performClick()

        assertTrue(retried)
    }

    @Test
    fun zoomablePage_originalFails_saysSo() {
        // Without a network fetcher every request fails, as a failed download does.
        val failingLoader = ImageLoader.Builder(ApplicationProvider.getApplicationContext())
            .serviceLoaderEnabled(false)
            .build()
        composeRule.setContent {
            CompositionLocalProvider(LocalRemoteImageLoader provides failingLoader) {
                RemoteZoomablePage(IMAGE_A, FakeUrls, onTap = {})
            }
        }

        composeRule.waitUntil(LOAD_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText("The full image couldn't be loaded.").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun error_isShown() {
        show(RemoteViewerUiState(error = AppError.NotLoggedIn))

        composeRule.onNodeWithText("Sign in to your Nextcloud first.").assertIsDisplayed()
    }

    @Test
    fun loading_showsTheIndicator() {
        show(RemoteViewerUiState())

        composeRule.onNodeWithContentDescription("Loading").assertIsDisplayed()
    }

    @Test
    fun infoContent_showsTheValues() {
        composeRule.setContent { RemoteInfoContent(IMAGE_A) }

        composeRule.onNodeWithText("a.jpg").assertIsDisplayed()
        composeRule.onNodeWithText("image/jpeg").assertIsDisplayed()
        composeRule.onNodeWithText("/P").assertIsDisplayed()
    }

    @Test
    fun infoContent_noDate_leavesItOut() {
        composeRule.setContent { RemoteInfoContent(IMAGE_B) }

        composeRule.onNodeWithText("Date").assertDoesNotExist()
    }

    @Test
    fun infoContent_withDate_showsIt() {
        composeRule.setContent { RemoteInfoContent(IMAGE_A) }

        composeRule.onNodeWithText("Date").assertIsDisplayed()
        assertEquals(1_000L, IMAGE_A.lastModifiedMillis)
    }

    private companion object {
        const val LOAD_TIMEOUT_MILLIS = 5_000L
    }
}
