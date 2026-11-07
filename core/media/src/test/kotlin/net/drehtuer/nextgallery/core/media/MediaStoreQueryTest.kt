package net.drehtuer.nextgallery.core.media

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaStoreQueryTest {
    private val mimeTypes = arrayOf("image/gif", "image/heic", "image/heif", "image/jpeg", "image/png", "image/webp")

    @Test
    fun imagesIn_selectsSupportedMimeTypesInTheFolder() {
        val selection = MediaStoreQuery.imagesIn(listOf("DCIM/Camera"))

        assertEquals(
            Selection(
                "mime_type IN (?,?,?,?,?,?) AND (relative_path LIKE ? ESCAPE '\\')",
                mimeTypes + "DCIM/Camera/%",
            ),
            selection,
        )
    }

    @Test
    fun imagesIn_severalFolders_matchesAnyOfThem() {
        val selection = MediaStoreQuery.imagesIn(listOf("DCIM", "Pictures"))

        assertEquals(
            "mime_type IN (?,?,?,?,?,?) AND (relative_path LIKE ? ESCAPE '\\' OR relative_path LIKE ? ESCAPE '\\')" to
                listOf("DCIM/%", "Pictures/%"),
            selection.sql to selection.args.takeLast(2),
        )
    }

    @Test
    fun imagesIn_percentInFolderName_isEscaped() {
        val selection = MediaStoreQuery.imagesIn(listOf("100%"))

        assertEquals("100\\%/%", selection.args.last())
    }

    @Test
    fun imagesIn_underscoreInFolderName_isEscaped() {
        val selection = MediaStoreQuery.imagesIn(listOf("my_photos"))

        assertEquals("my\\_photos/%", selection.args.last())
    }

    @Test
    fun imagesIn_backslashInFolderName_isEscaped() {
        val selection = MediaStoreQuery.imagesIn(listOf("a\\b"))

        assertEquals("a\\\\b/%", selection.args.last())
    }

    @Test(expected = IllegalArgumentException::class)
    fun imagesIn_noFolder_isRefused() {
        MediaStoreQuery.imagesIn(emptyList())
    }

    @Test
    fun changedSince_addsGenerationCondition() {
        val selection = MediaStoreQuery.changedSince(42, listOf("DCIM"))

        assertEquals(
            "mime_type IN (?,?,?,?,?,?) AND (relative_path LIKE ? ESCAPE '\\') AND generation_modified > ?" to
                listOf("DCIM/%", "42"),
            selection.sql to selection.args.takeLast(2),
        )
    }
}
