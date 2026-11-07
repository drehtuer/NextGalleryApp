package net.drehtuer.nextgallery.feature.viewer

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.drehtuer.nextgallery.core.data.media.ImageDetailsMapper
import net.drehtuer.nextgallery.core.data.media.MediaActionsRepository
import net.drehtuer.nextgallery.core.data.media.MediaRepository
import net.drehtuer.nextgallery.core.data.media.TrashFlow
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaSource
import net.drehtuer.nextgallery.core.model.ShareSpec
import net.drehtuer.nextgallery.core.model.TrashEffect
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewerViewModelTest {
    private val source = MediaSource.Folder(7)
    private val details = ImageDetailsMapper.from(null, mediaItem(1))
    private val spec = ShareSpec(listOf("content://media/1"), "image/jpeg")
    private val mediaRepository = mockk<MediaRepository> {
        every { pagedItems(source) } returns emptyFlow()
        coEvery { indexOf(source, MediaId(1)) } returns 5
        coEvery { imageDetails(MediaId(1)) } returns details
    }
    private val actions = mockk<MediaActionsRepository> {
        coEvery { shareSpec(listOf(MediaId(1))) } returns spec
    }
    private val trashFlow = mockk<TrashFlow>(relaxed = true)

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() =
        ViewerViewModel(ViewerRoute(mediaId = 1, bucketId = 7), mediaRepository, actions, trashFlow)

    private fun viewModelOnFirstImage() = viewModel().apply { onEvent(ViewerEvent.PageChanged(mediaItem(1))) }

    @Test
    fun init_findsPositionOfOpenedImage() {
        assertEquals(5, viewModel().state.value.initialIndex)
    }

    @Test
    fun init_routeWithoutBucket_pagesThroughTimeline() {
        every { mediaRepository.pagedItems(MediaSource.Timeline) } returns emptyFlow()
        coEvery { mediaRepository.indexOf(MediaSource.Timeline, MediaId(1)) } returns 9

        val viewModel = ViewerViewModel(ViewerRoute(mediaId = 1, bucketId = null), mediaRepository, actions, trashFlow)

        assertEquals(9, viewModel.state.value.initialIndex)
    }

    @Test
    fun onEvent_edit_opensEditorForCurrentImage() = runTest {
        val viewModel = viewModelOnFirstImage()

        viewModel.effects.test {
            viewModel.onEvent(ViewerEvent.Edit)

            assertEquals(ViewerEffect.OpenEditor(MediaId(1)), awaitItem())
        }
    }

    @Test
    fun onEvent_pageChanged_updatesCurrentImage() {
        assertEquals(mediaItem(1), viewModelOnFirstImage().state.value.current)
    }

    @Test
    fun onEvent_toggleChrome_hidesChrome() {
        val viewModel = viewModel()

        viewModel.onEvent(ViewerEvent.ToggleChrome)

        assertFalse(viewModel.state.value.chromeVisible)
    }

    @Test
    fun onEvent_showInfo_loadsDetailsOfCurrentImage() {
        val viewModel = viewModelOnFirstImage()

        viewModel.onEvent(ViewerEvent.ShowInfo)

        assertEquals(details, viewModel.state.value.details)
    }

    @Test
    fun onEvent_showInfoWithoutImage_loadsNothing() {
        val viewModel = viewModel()

        viewModel.onEvent(ViewerEvent.ShowInfo)

        assertNull(viewModel.state.value.details)
    }

    @Test
    fun onEvent_hideInfo_clearsDetails() {
        val viewModel = viewModelOnFirstImage()
        viewModel.onEvent(ViewerEvent.ShowInfo)

        viewModel.onEvent(ViewerEvent.HideInfo)

        assertNull(viewModel.state.value.details)
    }

    @Test
    fun onEvent_share_sendsShareEffect() = runTest {
        val viewModel = viewModelOnFirstImage()

        viewModel.onEvent(ViewerEvent.Share)

        viewModel.effects.test { assertEquals(ViewerEffect.Share(spec), awaitItem()) }
    }

    @Test
    fun onEvent_delete_requestsTrashForCurrentImage() = runTest {
        val viewModel = viewModelOnFirstImage()
        coEvery { trashFlow.request(listOf(MediaId(1))) } returns TrashEffect.Failed

        viewModel.onEvent(ViewerEvent.Delete)

        viewModel.effects.test { assertEquals(ViewerEffect.Trash(TrashEffect.Failed), awaitItem()) }
    }

    @Test
    fun onEvent_confirmationResult_goesToTrashFlow() {
        viewModel().onEvent(ViewerEvent.ConfirmationResult(confirmed = true))

        coVerify { trashFlow.onConfirmationResult(true) }
    }

    @Test
    fun onEvent_undoDelete_goesToTrashFlow() {
        viewModel().onEvent(ViewerEvent.UndoDelete)

        coVerify { trashFlow.undo() }
    }

    @Test
    fun onEvent_noImagesLeft_closesViewer() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(ViewerEvent.ItemCountChanged(0))

        viewModel.effects.test { assertEquals(ViewerEffect.Close, awaitItem()) }
    }

    @Test
    fun onEvent_imagesLeft_staysOpen() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(ViewerEvent.ItemCountChanged(1))

        viewModel.effects.test { expectNoEvents() }
    }
}
