package net.drehtuer.nextgallery.feature.map

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.data.media.MediaActionsRepository
import net.drehtuer.nextgallery.core.data.media.TrashFlow
import net.drehtuer.nextgallery.core.data.selection.SelectionRepository
import net.drehtuer.nextgallery.core.model.GeoBounds
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.ShareSpec
import net.drehtuer.nextgallery.core.model.TrashEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapSelectionActionsTest {
    private val id = MediaId(1)
    private val bounds = GeoBounds(45.0, 10.0, 46.0, 11.0)
    private val selected = MutableStateFlow<Set<MediaId>>(emptySet())
    private val spec = ShareSpec(listOf("content://media/1"), "image/jpeg")
    private val trashEffect = mockk<TrashEffect>()
    private val selectionRepository = mockk<SelectionRepository>(relaxed = true) {
        every { observe() } returns selected
    }
    private val mediaActions = mockk<MediaActionsRepository>()
    private val trashFlow = mockk<TrashFlow>()
    private val actions = MapSelectionActions(selectionRepository, mediaActions, trashFlow)

    @Test
    fun observe_isTheAppWideSelection() {
        assertEquals(selected, actions.observe())
    }

    @Test
    fun itemClicked_nothingSelected_opensViewerInTheCluster() = runTest {
        assertEquals(MapEffect.OpenViewer(id, bounds), actions.handle(MapSelectionEvent.ItemClicked(id), bounds))
    }

    @Test
    fun itemClicked_duringSelection_togglesIt() = runTest {
        selected.value = setOf(MediaId(2))

        val effect = actions.handle(MapSelectionEvent.ItemClicked(id), bounds)

        assertNull(effect)
        verify { selectionRepository.toggle(id) }
    }

    @Test
    fun itemClicked_noOpenCluster_togglesIt() = runTest {
        actions.handle(MapSelectionEvent.ItemClicked(id), openBounds = null)

        verify { selectionRepository.toggle(id) }
    }

    @Test
    fun itemLongPressed_togglesIt() = runTest {
        val effect = actions.handle(MapSelectionEvent.ItemLongPressed(id), bounds)

        assertNull(effect)
        verify { selectionRepository.toggle(id) }
    }

    @Test
    fun clear_clearsSelection() = runTest {
        val effect = actions.handle(MapSelectionEvent.Clear, bounds)

        assertNull(effect)
        verify { selectionRepository.clear() }
    }

    @Test
    fun share_appliesToWholeSelection() = runTest {
        selected.value = setOf(id)
        coEvery { mediaActions.shareSpec(setOf(id)) } returns spec

        assertEquals(MapEffect.Share(spec), actions.handle(MapSelectionEvent.Share, bounds))
    }

    @Test
    fun share_nothingToShare_doesNothing() = runTest {
        coEvery { mediaActions.shareSpec(emptySet()) } returns null

        assertNull(actions.handle(MapSelectionEvent.Share, bounds))
    }

    @Test
    fun delete_appliesToWholeSelection() = runTest {
        selected.value = setOf(id)
        coEvery { trashFlow.request(setOf(id)) } returns trashEffect

        assertEquals(MapEffect.Trash(trashEffect), actions.handle(MapSelectionEvent.Delete, bounds))
    }

    @Test
    fun delete_nothingHappens_noEffect() = runTest {
        coEvery { trashFlow.request(emptySet()) } returns null

        assertNull(actions.handle(MapSelectionEvent.Delete, bounds))
    }

    @Test
    fun confirmationResult_goesToTrashFlow() = runTest {
        coEvery { trashFlow.onConfirmationResult(true) } returns trashEffect

        assertEquals(MapEffect.Trash(trashEffect), actions.handle(MapSelectionEvent.ConfirmationResult(true), bounds))
    }

    @Test
    fun undoDelete_goesToTrashFlow() = runTest {
        coEvery { trashFlow.undo() } returns trashEffect

        assertEquals(MapEffect.Trash(trashEffect), actions.handle(MapSelectionEvent.UndoDelete, bounds))
    }
}
