package net.drehtuer.nextgallery.core.media

import android.database.MatrixCursor
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.ScannedImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CursorMapperTest {
    private fun cursorWith(dateTaken: Long?) = MatrixCursor(MediaStoreQuery.PROJECTION).apply {
        addRow(
            arrayOf<Any?>(
                5L, "external_primary", 7L, "Camera", "DCIM/Camera/", "IMG_5.jpg", "image/jpeg",
                1_000L, 4_000, 3_000, 90, dateTaken, 1_710_500_000L, 12L,
            ),
        )
        moveToFirst()
    }

    @Test
    fun map_fullRow_mapsAllColumns() {
        val cursor = cursorWith(dateTaken = 1_710_500_000_000L)

        val image = CursorMapper(cursor, "external_primary").map(cursor)

        assertEquals(
            ScannedImage(
                id = MediaId(5),
                volumeName = "external_primary",
                contentUri = "content://media/external_primary/images/media/5",
                bucketId = 7,
                bucketName = "Camera",
                relativePath = "DCIM/Camera/",
                displayName = "IMG_5.jpg",
                mimeType = "image/jpeg",
                sizeBytes = 1_000,
                width = 4_000,
                height = 3_000,
                orientation = 90,
                dateTakenMillis = 1_710_500_000_000L,
                dateModifiedSeconds = 1_710_500_000L,
                generationModified = 12,
            ),
            image,
        )
    }

    @Test
    fun map_nullDateTaken_returnsNullDate() {
        val cursor = cursorWith(dateTaken = null)

        assertNull(CursorMapper(cursor, "external_primary").map(cursor).dateTakenMillis)
    }
}
