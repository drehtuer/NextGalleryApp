package net.drehtuer.nextgallery.core.data.selection

import net.drehtuer.nextgallery.core.model.MediaItem
import net.drehtuer.nextgallery.core.model.ViewContext

/** Can a view show an image? */
object SelectionRepresentability {
    fun canShow(item: MediaItem, context: ViewContext): Boolean = when (context) {
        is ViewContext.Folder -> item.bucketId == context.bucketId
        is ViewContext.VirtualFolder -> item.bucketId in context.bucketIds
        ViewContext.Timeline -> true
        ViewContext.Map -> item.location != null
    }
}
