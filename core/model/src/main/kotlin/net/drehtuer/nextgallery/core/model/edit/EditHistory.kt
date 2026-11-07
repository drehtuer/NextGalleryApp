package net.drehtuer.nextgallery.core.model.edit

/** The operations of an edit with undo and redo. Undone operations stay until a new one replaces them. */
data class EditHistory(private val all: List<EditOperation> = emptyList(), private val cursor: Int = 0) {
    val operations: List<EditOperation> get() = all.take(cursor)

    val canUndo: Boolean get() = cursor > 0

    val canRedo: Boolean get() = cursor < all.size

    fun push(operation: EditOperation) = EditHistory(operations + operation, cursor + 1)

    fun undo() = if (canUndo) copy(cursor = cursor - 1) else this

    fun redo() = if (canRedo) copy(cursor = cursor + 1) else this
}
