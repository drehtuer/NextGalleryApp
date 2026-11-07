package net.drehtuer.nextgallery.core.model

/** The view a selection overview was opened from; it decides which selected images are greyed out (D33). */
sealed interface ViewContext {
    data class Folder(val bucketId: Long) : ViewContext

    data object Timeline : ViewContext

    data object Map : ViewContext
}
