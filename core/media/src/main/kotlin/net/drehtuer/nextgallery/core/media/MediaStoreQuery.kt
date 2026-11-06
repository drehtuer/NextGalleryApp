package net.drehtuer.nextgallery.core.media

import android.provider.MediaStore.Images.Media
import net.drehtuer.nextgallery.core.model.SupportedMimeTypes

/** SQL selection plus its arguments for a MediaStore query. */
data class Selection(val sql: String, val args: Array<String>) {
    override fun equals(other: Any?): Boolean = other is Selection && sql == other.sql && args.contentEquals(other.args)

    override fun hashCode(): Int = 31 * sql.hashCode() + args.contentHashCode()
}

/** Columns and selections for image queries. MediaStore already hides pending and trashed rows by default. */
object MediaStoreQuery {
    val PROJECTION = arrayOf(
        Media._ID,
        Media.VOLUME_NAME,
        Media.BUCKET_ID,
        Media.BUCKET_DISPLAY_NAME,
        Media.RELATIVE_PATH,
        Media.DISPLAY_NAME,
        Media.MIME_TYPE,
        Media.SIZE,
        Media.WIDTH,
        Media.HEIGHT,
        Media.ORIENTATION,
        Media.DATE_TAKEN,
        Media.DATE_MODIFIED,
        Media.GENERATION_MODIFIED,
    )

    const val SORT_ORDER = "${Media._ID} ASC"

    private val mimeTypes = SupportedMimeTypes.all.sorted()
    private val mimeTypeSql = "${Media.MIME_TYPE} IN (${mimeTypes.joinToString(",") { "?" }})"

    fun allImages() = Selection(mimeTypeSql, mimeTypes.toTypedArray())

    fun changedSince(generation: Long) = Selection(
        "$mimeTypeSql AND ${Media.GENERATION_MODIFIED} > ?",
        (mimeTypes + generation.toString()).toTypedArray(),
    )
}
