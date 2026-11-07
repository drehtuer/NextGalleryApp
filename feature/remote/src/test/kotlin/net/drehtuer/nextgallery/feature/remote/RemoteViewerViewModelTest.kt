package net.drehtuer.nextgallery.feature.remote

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.data.nextcloud.NextcloudAccountRepository
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteBrowseRepository
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteDownloadProgress
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteDownloads
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteListing
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteViewerViewModelTest {
    private val browse = mockk<RemoteBrowseRepository> {
        every { list("/P") } returns flowOf(RemoteListing("/P", listOf(FOLDER, IMAGE_A, IMAGE_B)))
    }
    private val accounts = mockk<NextcloudAccountRepository> { every { observeImageUrls() } returns flowOf(FakeUrls) }
    private val progressOfB = MutableStateFlow<RemoteDownloadProgress?>(null)
    private val downloads = mockk<RemoteDownloads> {
        every { observe(any()) } returns flowOf(null)
        every { observe(FakeUrls.original(IMAGE_B)) } returns progressOfB
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(path: String = IMAGE_B.path): RemoteViewerViewModel {
        val viewModel = RemoteViewerViewModel(path, browse, accounts, downloads)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        return viewModel
    }

    @Test
    fun state_listsTheImagesOfTheParentFolderWithoutFolders() = runTest {
        assertEquals(listOf(IMAGE_A, IMAGE_B), viewModel().state.value.images)
    }

    @Test
    fun state_startsAtTheOpenedImage() = runTest {
        val state = viewModel().state.value

        assertEquals(1, state.initialIndex)
        assertEquals(IMAGE_B, state.current)
    }

    @Test
    fun state_imageGone_hasNoStartPage() = runTest {
        assertNull(viewModel("/P/gone.jpg").state.value.initialIndex)
    }

    @Test
    fun state_nothingLoadedAndError_showsTheError() = runTest {
        every { browse.list("/P") } returns flowOf(RemoteListing("/P", emptyList(), error = AppError.NotLoggedIn))

        assertEquals(AppError.NotLoggedIn, viewModel().state.value.error)
    }

    @Test
    fun state_cachedImagesAndError_showsTheImages() = runTest {
        every { browse.list("/P") } returns flowOf(RemoteListing("/P", listOf(IMAGE_B), error = AppError.Network(null)))

        assertNull(viewModel().state.value.error)
    }

    @Test
    fun onEvent_pageChanged_changesTheCurrentImage() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(RemoteViewerEvent.PageChanged(0))

        assertEquals(IMAGE_A, viewModel.state.value.current)
    }

    @Test
    fun onEvent_toggleChrome_hidesTheBar() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(RemoteViewerEvent.ToggleChrome)

        assertFalse(viewModel.state.value.chromeVisible)
    }

    @Test
    fun onEvent_showAndHideInfo() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(RemoteViewerEvent.ShowInfo)
        assertTrue(viewModel.state.value.showInfo)

        viewModel.onEvent(RemoteViewerEvent.HideInfo)
        assertFalse(viewModel.state.value.showInfo)
    }

    @Test
    fun progress_ofTheCurrentImage_isShown() = runTest {
        val viewModel = viewModel()

        progressOfB.value = RemoteDownloadProgress(1, 2)

        assertEquals(RemoteDownloadProgress(1, 2), viewModel.state.value.progress)
    }

    @Test
    fun progress_ofAnotherImage_isNotShown() = runTest {
        val viewModel = viewModel()
        progressOfB.value = RemoteDownloadProgress(1, 2)

        viewModel.onEvent(RemoteViewerEvent.PageChanged(0))

        assertNull(viewModel.state.value.progress)
        verify { downloads.observe(FakeUrls.original(IMAGE_A)) }
    }
}
