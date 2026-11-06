package net.drehtuer.nextgallery.core.data.selection

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.data.fakes.FakeMediaIndexStore
import net.drehtuer.nextgallery.core.data.fakes.mediaItem
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.ViewContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultSelectionRepositoryTest {
    private val store = FakeMediaIndexStore()
    private val repository = DefaultSelectionRepository(store)
    private val ids = listOf(MediaId(1), MediaId(2), MediaId(3), MediaId(4))

    @Test
    fun observe_initially_isEmpty() {
        assertTrue(repository.observe().value.isEmpty())
    }

    @Test
    fun toggle_unselectedItem_selectsIt() {
        repository.toggle(MediaId(1))

        assertEquals(setOf(MediaId(1)), repository.observe().value)
    }

    @Test
    fun toggle_selectedItem_unselectsIt() {
        repository.toggle(MediaId(1))

        repository.toggle(MediaId(1))

        assertTrue(repository.observe().value.isEmpty())
    }

    @Test
    fun selectRange_forward_selectsBothEndsAndEverythingBetween() {
        repository.selectRange(ids, from = MediaId(2), to = MediaId(4))

        assertEquals(setOf(MediaId(2), MediaId(3), MediaId(4)), repository.observe().value)
    }

    @Test
    fun selectRange_backward_selectsBothEndsAndEverythingBetween() {
        repository.selectRange(ids, from = MediaId(3), to = MediaId(1))

        assertEquals(setOf(MediaId(1), MediaId(2), MediaId(3)), repository.observe().value)
    }

    @Test
    fun selectRange_endNotInList_selectsNothing() {
        repository.selectRange(ids, from = MediaId(1), to = MediaId(9))

        assertTrue(repository.observe().value.isEmpty())
    }

    @Test
    fun selectAll_keepsImagesSelectedElsewhere() {
        repository.toggle(MediaId(9))

        repository.selectAll(listOf(MediaId(1)))

        assertEquals(setOf(MediaId(9), MediaId(1)), repository.observe().value)
    }

    @Test
    fun clear_removesEverything() {
        repository.selectAll(ids)

        repository.clear()

        assertTrue(repository.observe().value.isEmpty())
    }

    @Test
    fun deselect_removesOnlyGivenImages() {
        repository.selectAll(listOf(MediaId(1), MediaId(2)))

        repository.deselect(listOf(MediaId(1)))

        assertEquals(setOf(MediaId(2)), repository.observe().value)
    }

    @Test
    fun observeOverview_sortsNewestFirst() = runTest {
        store.upsertAll(listOf(mediaItem(1, dateTakenMillis = 10), mediaItem(2, dateTakenMillis = 20)))
        repository.selectAll(listOf(MediaId(1), MediaId(2)))

        val overview = repository.observeOverview(ViewContext.Timeline).first()

        assertEquals(listOf(MediaId(2), MediaId(1)), overview.map { it.item.id })
    }

    @Test
    fun observeOverview_imageOfOtherFolder_isGreyedOut() = runTest {
        store.upsertAll(listOf(mediaItem(1, bucketId = 8)))
        repository.toggle(MediaId(1))

        val overview = repository.observeOverview(ViewContext.Folder(7)).first()

        assertTrue(overview.single().greyedOut)
    }
}
