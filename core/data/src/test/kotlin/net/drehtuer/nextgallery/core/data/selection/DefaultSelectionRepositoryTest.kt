package net.drehtuer.nextgallery.core.data.selection

import net.drehtuer.nextgallery.core.model.MediaId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultSelectionRepositoryTest {
    private val repository = DefaultSelectionRepository()
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
}
