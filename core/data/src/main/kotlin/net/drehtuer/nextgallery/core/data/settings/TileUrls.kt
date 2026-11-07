package net.drehtuer.nextgallery.core.data.settings

/** What a tile server address must look like: HTTPS only (D19), with placeholders for the tile. */
object TileUrls {
    private val PLACEHOLDERS = listOf("{z}", "{x}", "{y}")
    private const val HTTPS_PREFIX = "https://"

    fun isValid(url: String): Boolean = url.startsWith(HTTPS_PREFIX) &&
        url.length > HTTPS_PREFIX.length &&
        url.none { it.isWhitespace() } &&
        PLACEHOLDERS.all { it in url }
}
