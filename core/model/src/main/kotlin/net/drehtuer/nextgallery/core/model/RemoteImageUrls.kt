package net.drehtuer.nextgallery.core.model

/** Where to load the images of the signed-in account from. Screens get this instead of the account and its secret. */
interface RemoteImageUrls {
    /** Null if the server has no preview for this item. */
    fun preview(item: RemoteItem, sizePx: Int): String?

    fun original(item: RemoteItem): String
}
