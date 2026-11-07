package net.drehtuer.nextgallery.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ShareSpecTest {
    @Test
    fun from_noItems_returnsNull() {
        assertNull(ShareSpec.from(emptyList()))
    }

    @Test
    fun from_noTarget_hasNoTargetPackage() {
        assertNull(ShareSpec.from(listOf(item(1, "image/png")))?.targetPackage)
    }

    @Test
    fun from_target_keepsTheTargetPackage() {
        val spec = ShareSpec.from(listOf(item(1, "image/png")), targetPackage = "com.nextcloud.client")

        assertEquals("com.nextcloud.client", spec?.targetPackage)
    }

    @Test
    fun from_sameMimeType_usesThatType() {
        val spec = ShareSpec.from(listOf(item(1, "image/png"), item(2, "image/png")))

        assertEquals("image/png", spec?.mimeType)
    }

    @Test
    fun from_differentMimeTypes_usesAnyImage() {
        val spec = ShareSpec.from(listOf(item(1, "image/png"), item(2, "image/jpeg")))

        assertEquals("image/*", spec?.mimeType)
    }

    @Test
    fun from_keepsAllUrisInOrder() {
        val spec = ShareSpec.from(listOf(item(2, "image/png"), item(1, "image/png")))

        assertEquals(listOf("content://media/2", "content://media/1"), spec?.contentUris)
    }

    private fun item(id: Long, mimeType: String) = MediaItem(
        id = MediaId(id),
        volumeName = "external_primary",
        contentUri = "content://media/$id",
        bucketId = 7,
        bucketName = "Camera",
        relativePath = "DCIM/Camera/",
        displayName = "IMG_$id.jpg",
        mimeType = mimeType,
        sizeBytes = 1,
        width = 1,
        height = 1,
        orientation = 0,
        dateTakenMillis = 1,
        dateSource = DateSource.DATE_TAKEN,
        takenLocalDate = LocalDate.of(2024, 3, 15),
        dateModifiedSeconds = 1,
        generationModified = 1,
        location = null,
    )
}
