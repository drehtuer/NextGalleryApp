package net.drehtuer.nextgallery.core.media

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaStoreQueryTest {
    @Test
    fun allImages_selectsSupportedMimeTypes() {
        val selection = MediaStoreQuery.allImages()

        assertEquals(
            Selection(
                "mime_type IN (?,?,?,?,?,?)",
                arrayOf("image/gif", "image/heic", "image/heif", "image/jpeg", "image/png", "image/webp"),
            ),
            selection,
        )
    }

    @Test
    fun changedSince_addsGenerationCondition() {
        val selection = MediaStoreQuery.changedSince(42)

        assertEquals(
            "mime_type IN (?,?,?,?,?,?) AND generation_modified > ?" to "42",
            selection.sql to selection.args.last(),
        )
    }
}
