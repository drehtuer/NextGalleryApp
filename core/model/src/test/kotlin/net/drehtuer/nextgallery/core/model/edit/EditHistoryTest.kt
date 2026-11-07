package net.drehtuer.nextgallery.core.model.edit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditHistoryTest {
    private val first = EditOperation.Rotate(1)
    private val second = EditOperation.Scale(10, 10)

    @Test
    fun new_hasNothingToUndoOrRedo() {
        val history = EditHistory()

        assertEquals(listOf(false, false), listOf(history.canUndo, history.canRedo))
    }

    @Test
    fun push_appendsOperation() {
        assertEquals(listOf(first, second), EditHistory().push(first).push(second).operations)
    }

    @Test
    fun push_enablesUndo() {
        assertTrue(EditHistory().push(first).canUndo)
    }

    @Test
    fun undo_removesLastOperation() {
        assertEquals(listOf(first), EditHistory().push(first).push(second).undo().operations)
    }

    @Test
    fun undo_enablesRedo() {
        assertTrue(EditHistory().push(first).undo().canRedo)
    }

    @Test
    fun undo_empty_changesNothing() {
        assertEquals(EditHistory(), EditHistory().undo())
    }

    @Test
    fun redo_bringsOperationBack() {
        assertEquals(listOf(first), EditHistory().push(first).undo().redo().operations)
    }

    @Test
    fun redo_nothingUndone_changesNothing() {
        val history = EditHistory().push(first)

        assertEquals(history, history.redo())
    }

    @Test
    fun push_afterUndo_truncatesRedo() {
        val history = EditHistory().push(first).undo().push(second)

        assertEquals(listOf<EditOperation>(second), history.operations)
        assertFalse(history.canRedo)
    }
}
