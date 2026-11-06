package net.drehtuer.nextgallery.core.data.selection

import net.drehtuer.nextgallery.core.model.MediaItem
import net.drehtuer.nextgallery.core.model.ViewContext

/** Can a view show an image? The map's rule (the image has GPS data) follows in Phase 6. */
object SelectionRepresentability {
    fun canShow(item: MediaItem, context: ViewContext): Boolean = when (context) {
        is ViewContext.Folder -> item.bucketId == context.bucketId
        ViewContext.Timeline -> true
    }
}
