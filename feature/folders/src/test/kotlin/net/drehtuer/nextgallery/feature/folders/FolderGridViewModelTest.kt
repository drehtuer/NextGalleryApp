package net.drehtuer.nextgallery.feature.folders

import androidx.paging.PagingData
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.drehtuer.nextgallery.core.data.media.FolderRepository
import net.drehtuer.nextgallery.core.data.media.MediaActionsRepository
import net.drehtuer.nextgallery.core.data.media.MediaRepository
import net.drehtuer.nextgallery.core.data.media.TrashFlow
import net.drehtuer.nextgallery.core.data.selection.SelectionRepository
import net.drehtuer.nextgallery.core.data.upload.UploadHandoffRepository
import net.drehtuer.nextgallery.core.data.upload.UploadStateRepository
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaSource
import net.drehtuer.nextgallery.core.model.ShareSpec
import net.drehtuer.nextgallery.core.model.TrashEffect
import net.drehtuer.nextgallery.core.model.UploadAvailability
import net.drehtuer.nextgallery.core.model.UploadHandoff
import net.drehtuer.nextgallery.core.model.UploadState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FolderGridViewModelTest {
    private val selectedIds = MutableStateFlow<Set<MediaId>>(emptySet())
    private val mediaRepository = mockk<MediaRepository> {
        every { pagedItems(MediaSource.Folder(7)) } returns emptyFlow()
        coEvery { folderItemIds(7) } returns listOf(MediaId(1), MediaId(2))
        every { pagedItems(MediaSource.VirtualFolder(listOf(7, 8))) } returns flowOf(PagingData.empty())
        coEvery { virtualFolderItemIds(listOf(7, 8)) } returns listOf(MediaId(1), MediaId(3))
    }
    private val folderRepository = mockk<FolderRepository> {
        every { observeFolder(7) } returns flowOf(folder(name = "Camera"))
        every { observeVirtualFolder("v1") } returns flowOf(virtualFolder())
    }
    private val selectionRepository = mockk<SelectionRepository>(relaxed = true) {
        every { observe() } returns selectedIds
    }

    private val spec = ShareSpec(listOf("content://media/1"), "image/jpeg")
    private val actions = mockk<MediaActionsRepository> {
        coEvery { shareSpec(setOf(MediaId(1))) } returns spec
    }
    private val trashFlow = mockk<TrashFlow>(relaxed = true)
    private val visibleIds = slot<Flow<Set<MediaId>>>()
    private val badges = MutableStateFlow<Map<MediaId, UploadState>>(emptyMap())
    private val uploadStates = mockk<UploadStateRepository> {
        every { observeBadges(capture(visibleIds)) } returns badges
    }
    private val uploadHandoff = mockk<UploadHandoffRepository>(relaxed = true) {
        every { observeAvailability() } returns flowOf(UploadAvailability.APP_MISSING)
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun virtualViewModel() = viewModel(FolderGridRoute(virtualFolderId = "v1"))

    private fun viewModel(route: FolderGridRoute = FolderGridRoute(7)) = FolderGridViewModel(
        route,
        mediaRepository,
        folderRepository,
        selectionRepository,
        actions,
        trashFlow,
        uploadStates,
        uploadHandoff,
    )

    @Test
    fun state_containsUploadBadges() = runTest {
        badges.value = mapOf(MediaId(1) to UploadState.NotUploaded)

        val state = viewModel().state.first { it.uploadBadges.isNotEmpty() }

        assertEquals(mapOf(MediaId(1) to UploadState.NotUploaded), state.uploadBadges)
    }

    @Test
    fun state_containsUploadAvailability() = runTest {
        val state = viewModel().state.first { it.title.isNotEmpty() }

        assertEquals(UploadAvailability.APP_MISSING, state.uploadAvailability)
    }

    @Test
    fun onEvent_visibleItemsChanged_reachesTheBadges() = runTest {
        val viewModel = viewModel()
        viewModel.state.first { it.title.isNotEmpty() }

        viewModel.onEvent(FolderGridEvent.VisibleItemsChanged(setOf(MediaId(3))))

        assertEquals(setOf(MediaId(3)), visibleIds.captured.first())
    }

    @Test
    fun onEvent_uploadSelected_sendsTheHandoffOfTheSelection() = runTest {
        selectedIds.value = setOf(MediaId(1))
        coEvery { uploadHandoff.prepare(setOf(MediaId(1))) } returns UploadHandoff.NothingToUpload
        val viewModel = viewModel()

        viewModel.onEvent(FolderGridEvent.UploadSelected)

        viewModel.effects.test {
            assertEquals(FolderGridEffect.Upload(UploadHandoff.NothingToUpload), awaitItem())
            cancel()
        }
    }

    @Test
    fun onEvent_resumed_tellsTheRepository() {
        viewModel().onEvent(FolderGridEvent.Resumed)

        verify { uploadHandoff.onAppResumed() }
    }

    @Test
    fun virtualFolder_usesItsNameAsTitle() = runTest {
        assertEquals("Everything", virtualViewModel().state.first { it.title.isNotEmpty() }.title)
    }

    @Test
    fun virtualFolder_stateHasItsMembers() = runTest {
        assertEquals(
            listOf(7L, 8L),
            virtualViewModel().state.first { it.virtualBucketIds != null }.virtualBucketIds,
        )
    }

    @Test
    fun virtualFolder_loadsItemsOfItsMembers() = runTest {
        virtualViewModel().items.first()

        verify { mediaRepository.pagedItems(MediaSource.VirtualFolder(listOf(7, 8))) }
    }

    @Test
    fun virtualFolder_selectAll_selectsImagesOfItsMembers() {
        virtualViewModel().onEvent(FolderGridEvent.SelectAll)

        verify { selectionRepository.selectAll(listOf(MediaId(1), MediaId(3))) }
    }

    @Test
    fun virtualFolder_itemClicked_opensViewerWithItsMembers() = runTest {
        val viewModel = virtualViewModel()

        viewModel.onEvent(FolderGridEvent.ItemClicked(MediaId(1)))

        viewModel.effects.test {
            assertEquals(FolderGridEffect.OpenViewer(MediaId(1), listOf(7, 8)), awaitItem())
        }
    }

    @Test
    fun init_loadsItemsOfRouteBucket() {
        viewModel()

        verify { mediaRepository.pagedItems(MediaSource.Folder(7)) }
    }

    @Test
    fun state_usesFolderNameAsTitle() = runTest {
        assertEquals("Camera", viewModel().state.first { it.title.isNotEmpty() }.title)
    }

    @Test
    fun state_containsSelection() = runTest {
        selectedIds.value = setOf(MediaId(1))

        assertEquals(setOf(MediaId(1)), viewModel().state.first { it.selectedIds.isNotEmpty() }.selectedIds)
    }

    @Test
    fun onEvent_itemClickedWithoutSelection_opensViewer() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(FolderGridEvent.ItemClicked(MediaId(1)))

        viewModel.effects.test {
            assertEquals(FolderGridEffect.OpenViewer(MediaId(1)), awaitItem())
        }
    }

    @Test
    fun onEvent_itemClickedDuringSelection_togglesItem() {
        selectedIds.value = setOf(MediaId(9))

        viewModel().onEvent(FolderGridEvent.ItemClicked(MediaId(1)))

        verify { selectionRepository.toggle(MediaId(1)) }
    }

    @Test
    fun onEvent_startSelecting_setsSelecting() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(FolderGridEvent.StartSelecting)

        assertTrue(viewModel.state.first { it.title.isNotEmpty() }.selecting)
    }

    @Test
    fun onEvent_itemClickedAfterStartSelecting_togglesItem() {
        val viewModel = viewModel()
        viewModel.onEvent(FolderGridEvent.StartSelecting)

        viewModel.onEvent(FolderGridEvent.ItemClicked(MediaId(1)))

        verify { selectionRepository.toggle(MediaId(1)) }
    }

    @Test
    fun onEvent_clearSelection_stopsSelecting() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(FolderGridEvent.StartSelecting)

        viewModel.onEvent(FolderGridEvent.ClearSelection)

        assertFalse(viewModel.state.first { it.title.isNotEmpty() }.selecting)
    }

    @Test
    fun onEvent_itemLongPressed_togglesItem() {
        viewModel().onEvent(FolderGridEvent.ItemLongPressed(MediaId(1)))

        verify { selectionRepository.toggle(MediaId(1)) }
    }

    @Test
    fun onEvent_selectAll_selectsAllItemsOfFolder() {
        viewModel().onEvent(FolderGridEvent.SelectAll)

        verify { selectionRepository.selectAll(listOf(MediaId(1), MediaId(2))) }
    }

    @Test
    fun onEvent_clearSelection_clearsRepository() {
        viewModel().onEvent(FolderGridEvent.ClearSelection)

        verify { selectionRepository.clear() }
    }

    @Test
    fun onEvent_shareSelected_sendsShareEffect() = runTest {
        selectedIds.value = setOf(MediaId(1))
        val viewModel = viewModel()

        viewModel.onEvent(FolderGridEvent.ShareSelected)

        viewModel.effects.test { assertEquals(FolderGridEffect.Share(spec), awaitItem()) }
    }

    @Test
    fun onEvent_deleteSelected_requestsTrashForWholeSelection() = runTest {
        selectedIds.value = setOf(MediaId(1), MediaId(9))
        coEvery { trashFlow.request(setOf(MediaId(1), MediaId(9))) } returns TrashEffect.Failed
        val viewModel = viewModel()

        viewModel.onEvent(FolderGridEvent.DeleteSelected)

        viewModel.effects.test { assertEquals(FolderGridEffect.Trash(TrashEffect.Failed), awaitItem()) }
    }

    @Test
    fun onEvent_confirmationResult_goesToTrashFlow() {
        viewModel().onEvent(FolderGridEvent.ConfirmationResult(confirmed = true))

        coVerify { trashFlow.onConfirmationResult(true) }
    }

    @Test
    fun onEvent_undoDelete_goesToTrashFlow() {
        viewModel().onEvent(FolderGridEvent.UndoDelete)

        coVerify { trashFlow.undo() }
    }
}
