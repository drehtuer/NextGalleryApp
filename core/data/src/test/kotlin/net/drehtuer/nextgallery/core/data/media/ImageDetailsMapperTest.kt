package net.drehtuer.nextgallery.core.data.media

import net.drehtuer.nextgallery.core.data.fakes.mediaItem
import net.drehtuer.nextgallery.core.model.ImageDetails
import net.drehtuer.nextgallery.core.model.RawExif
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageDetailsMapperTest {
    private fun details(exif: RawExif?): ImageDetails = ImageDetailsMapper.from(exif, mediaItem(1))

    private fun exif(
        make: String? = null,
        model: String? = null,
        exposureTime: String? = null,
        fNumber: String? = null,
        iso: String? = null,
        focalLength: String? = null,
    ) = RawExif(null, null, null, make, model, exposureTime, fNumber, iso, focalLength)

    @Test
    fun from_takesFileFieldsFromItem() {
        assertEquals("IMG_1.jpg", details(null).displayName)
    }

    @Test
    fun from_noExif_hasNoExifValues() {
        val details = details(null)

        assertEquals(
            listOf(null, null, null, null, null),
            listOf(details.camera, details.exposureTime, details.aperture, details.iso, details.focalLength),
        )
    }

    @Test
    fun from_makeAndModel_joinsThem() {
        assertEquals("Google Pixel 10a", details(exif(make = "Google", model = "Pixel 10a")).camera)
    }

    @Test
    fun from_modelRepeatsMake_usesModelOnly() {
        assertEquals("Canon EOS R6", details(exif(make = "Canon", model = "Canon EOS R6")).camera)
    }

    @Test
    fun from_onlyMake_usesMake() {
        assertEquals("Google", details(exif(make = "Google")).camera)
    }

    @Test
    fun from_shortExposure_isShownAsFraction() {
        assertEquals("1/125 s", details(exif(exposureTime = "0.008")).exposureTime)
    }

    @Test
    fun from_longExposure_isShownInSeconds() {
        assertEquals("2 s", details(exif(exposureTime = "2.0")).exposureTime)
    }

    @Test
    fun from_invalidExposure_isHidden() {
        assertNull(details(exif(exposureTime = "abc")).exposureTime)
    }

    @Test
    fun from_fNumber_isShownAsAperture() {
        assertEquals("f/1.8", details(exif(fNumber = "1.8")).aperture)
    }

    @Test
    fun from_focalLengthAsFraction_isCalculated() {
        assertEquals("5.4 mm", details(exif(focalLength = "27/5")).focalLength)
    }

    @Test
    fun from_focalLengthWithZeroDivisor_isHidden() {
        assertNull(details(exif(focalLength = "27/0")).focalLength)
    }

    @Test
    fun from_wholeFocalLength_hasNoDecimals() {
        assertEquals("50 mm", details(exif(focalLength = "50/1")).focalLength)
    }

    @Test
    fun from_blankIso_isHidden() {
        assertNull(details(exif(iso = " ")).iso)
    }
}
