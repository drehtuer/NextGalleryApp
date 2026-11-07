package net.drehtuer.nextgallery.feature.remote

import net.drehtuer.nextgallery.core.model.RemoteImageUrls
import net.drehtuer.nextgallery.core.model.RemoteItem

internal val IMAGE_A =
    RemoteItem("/P/a.jpg", "a.jpg", false, mime = "image/jpeg", fileId = 1, size = 2048, lastModifiedMillis = 1_000)
internal val IMAGE_B = RemoteItem("/P/b.jpg", "b.jpg", false, mime = "image/jpeg", fileId = 2)
internal val FOLDER = RemoteItem("/P/Italy", "Italy", true, childCount = 3)

internal object FakeUrls : RemoteImageUrls {
    override fun preview(item: RemoteItem, sizePx: Int): String? = item.fileId?.let { "https://x/preview/$it" }

    override fun original(item: RemoteItem): String = "https://x/original${item.path}"
}
