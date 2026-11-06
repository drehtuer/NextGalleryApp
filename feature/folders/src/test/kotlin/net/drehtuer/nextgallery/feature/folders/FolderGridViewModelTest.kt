package net.drehtuer.nextgallery.feature.folders

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.drehtuer.nextgallery.core.data.media.FolderRepository
import net.drehtuer.nextgallery.core.data.media.MediaRepository
import net.drehtuer.nextgallery.core.data.selection.SelectionRepository
import net.drehtuer.nextgallery.core.model.MediaId
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FolderGridViewModelTest {
    private val selectedIds = MutableStateFlow<Set<MediaId>>(emptySet())
    private val mediaRepository = mockk<MediaRepository> {
        every { pagedFolderItems(7) } returns emptyFlow()
        coEvery { folderItemIds(7) } returns listOf(MediaId(1), MediaId(2))
    }
    private val folderRepository = mockk<FolderRepository> {
        every { observeFolder(7) } returns flowOf(folder(name = "Camera"))
    }
    private val selectionRepository = mockk<SelectionRepository>(relaxed = true) {
        every { observe() } returns selectedIds
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = FolderGridViewModel(7, mediaRepository, folderRepository, selectionRepository)

    @Test
    fun init_loadsItemsOfRouteBucket() {
        viewModel()

        verify { mediaRepository.pagedFolderItems(7) }
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
}
