package net.drehtuer.nextgallery.core.data.edit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ExifPolicyTest {
    @Test
    fun forOutput_copiesCameraDateAndLocationTags() {
        val tags = mapOf(
            "Make" to "Google",
            "DateTimeOriginal" to "2024:03:15 14:30:05",
            "GPSLatitude" to "48/1,8/1,0/1",
        )

        val output = ExifPolicy.forOutput(tags, 10, 20)

        assertEquals(tags, output.filterKeys { it in tags })
    }

    @Test
    fun forOutput_resetsOrientation() {
        assertEquals("1", ExifPolicy.forOutput(mapOf("Orientation" to "6"), 10, 20)["Orientation"])
    }

    @Test
    fun forOutput_setsNewDimensions() {
        val output = ExifPolicy.forOutput(mapOf("ImageWidth" to "4000", "PixelXDimension" to "4000"), 10, 20)

        assertEquals(
            listOf("10", "20", "10", "20"),
            listOf(
                output["ImageWidth"],
                output["ImageLength"],
                output["PixelXDimension"],
                output["PixelYDimension"],
            ),
        )
    }

    @Test
    fun forOutput_dropsTagsNotInThePolicy() {
        assertFalse("ThumbnailImageWidth" in ExifPolicy.forOutput(mapOf("ThumbnailImageWidth" to "160"), 10, 20))
    }
}
