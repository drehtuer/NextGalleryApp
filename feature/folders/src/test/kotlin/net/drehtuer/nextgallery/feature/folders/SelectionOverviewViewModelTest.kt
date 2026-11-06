package net.drehtuer.nextgallery.feature.folders

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.drehtuer.nextgallery.core.data.media.MediaActionsRepository
import net.drehtuer.nextgallery.core.data.media.TrashFlow
import net.drehtuer.nextgallery.core.data.selection.SelectionOverviewItem
import net.drehtuer.nextgallery.core.data.selection.SelectionRepository
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.ShareSpec
import net.drehtuer.nextgallery.core.model.TrashEffect
import net.drehtuer.nextgallery.core.model.ViewContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SelectionOverviewViewModelTest {
    private val selected = setOf(MediaId(1))
    private val overview = listOf(SelectionOverviewItem(mediaItem(1), greyedOut = true))
    private val spec = ShareSpec(listOf("content://media/1"), "image/jpeg")
    private val selectionRepository = mockk<SelectionRepository>(relaxed = true) {
        every { observe() } returns MutableStateFlow(selected)
        every { observeOverview(ViewContext.Folder(7)) } returns flowOf(overview)
    }
    private val actions = mockk<MediaActionsRepository> { coEvery { shareSpec(selected) } returns spec }
    private val trashFlow = mockk<TrashFlow>(relaxed = true)

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = SelectionOverviewViewModel(7, selectionRepository, actions, trashFlow)

    @Test
    fun state_containsOverviewForCallingFolder() = runTest {
        assertEquals(overview, viewModel().state.first { it.items != null }.items)
    }

    @Test
    fun onEvent_itemClicked_togglesItem() {
        viewModel().onEvent(SelectionOverviewEvent.ItemClicked(MediaId(1)))

        verify { selectionRepository.toggle(MediaId(1)) }
    }

    @Test
    fun onEvent_share_sendsShareEffect() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(SelectionOverviewEvent.Share)

        viewModel.effects.test { assertEquals(SelectionOverviewEffect.Share(spec), awaitItem()) }
    }

    @Test
    fun onEvent_delete_requestsTrashForWholeSelection() = runTest {
        coEvery { trashFlow.request(selected) } returns TrashEffect.Failed
        val viewModel = viewModel()

        viewModel.onEvent(SelectionOverviewEvent.Delete)

        viewModel.effects.test { assertEquals(SelectionOverviewEffect.Trash(TrashEffect.Failed), awaitItem()) }
    }

    @Test
    fun onEvent_confirmationResult_goesToTrashFlow() {
        viewModel().onEvent(SelectionOverviewEvent.ConfirmationResult(confirmed = false))

        coVerify { trashFlow.onConfirmationResult(false) }
    }

    @Test
    fun onEvent_undoDelete_goesToTrashFlow() {
        viewModel().onEvent(SelectionOverviewEvent.UndoDelete)

        coVerify { trashFlow.undo() }
    }
}
