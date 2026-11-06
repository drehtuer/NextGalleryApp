package net.drehtuer.nextgallery.core.data.media

import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.data.fakes.FakeMediaIndexStore
import net.drehtuer.nextgallery.core.data.fakes.mediaItem
import net.drehtuer.nextgallery.core.data.selection.SelectionRepository
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.PlatformConfirmation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class DefaultMediaActionsRepositoryTest {
    private val store = FakeMediaIndexStore()
    private val confirmation = PlatformConfirmation("handle")
    private val deleteSource = mockk<MediaDeleteSource>()
    private val selection = mockk<SelectionRepository>(relaxed = true)
    private val repository = DefaultMediaActionsRepository(store, deleteSource, selection)

    @Test
    fun delete_unknownIds_hasNothingToDelete() = runTest {
        assertEquals(DeleteOutcome.NothingToDelete, repository.delete(listOf(MediaId(1))))
    }

    @Test
    fun delete_default_requestsTrash() = runTest {
        store.upsertAll(listOf(mediaItem(1)))
        coEvery { deleteSource.trashRequest(listOf("content://media/1")) } returns confirmation

        val outcome = repository.delete(listOf(MediaId(1)))

        assertSame(confirmation, (outcome as DeleteOutcome.NeedsUserConfirmation).confirmation)
    }

    @Test
    fun delete_permanent_requestsDeletion() = runTest {
        store.upsertAll(listOf(mediaItem(1)))
        coEvery { deleteSource.deleteRequest(listOf("content://media/1")) } returns confirmation

        val outcome = repository.delete(listOf(MediaId(1)), permanent = true)

        assertSame(confirmation, (outcome as DeleteOutcome.NeedsUserConfirmation).confirmation)
    }

    @Test
    fun delete_requestFails_returnsFailed() = runTest {
        store.upsertAll(listOf(mediaItem(1)))
        coEvery { deleteSource.trashRequest(any()) } throws IOException("failed")

        assertTrue(repository.delete(listOf(MediaId(1))) is DeleteOutcome.Failed)
    }

    @Test
    fun onDeleteConfirmed_removesRowsFromIndex() = runTest {
        store.upsertAll(listOf(mediaItem(1), mediaItem(2)))

        repository.onDeleteConfirmed(listOf(MediaId(1)))

        assertEquals(setOf(2L), store.items.value.keys)
    }

    @Test
    fun onDeleteConfirmed_removesImagesFromSelection() = runTest {
        repository.onDeleteConfirmed(listOf(MediaId(1)))

        verify { selection.deselect(listOf(MediaId(1))) }
    }

    @Test
    fun restore_returnsConfirmation() = runTest {
        coEvery { deleteSource.restoreRequest(listOf("uri")) } returns confirmation

        assertEquals(AppResult.Success(confirmation), repository.restore(listOf("uri")))
    }

    @Test
    fun restore_requestFails_returnsFailure() = runTest {
        coEvery { deleteSource.restoreRequest(any()) } throws IOException("failed")

        assertTrue(repository.restore(listOf("uri")) is AppResult.Failure)
    }

    @Test
    fun shareSpec_usesUrisOfKnownImages() = runTest {
        store.upsertAll(listOf(mediaItem(1)))

        assertEquals(listOf("content://media/1"), repository.shareSpec(listOf(MediaId(1)))?.contentUris)
    }

    @Test
    fun shareSpec_unknownIds_returnsNull() = runTest {
        assertNull(repository.shareSpec(listOf(MediaId(1))))
    }
}
