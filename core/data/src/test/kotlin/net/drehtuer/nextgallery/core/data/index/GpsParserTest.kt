package net.drehtuer.nextgallery.core.data.index

import net.drehtuer.nextgallery.core.model.RawExif
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GpsParserTest {
    private fun exif(
        latitude: String? = "45/1,30/1,0/1",
        latitudeRef: String? = "N",
        longitude: String? = "10/1,15/1,0/1",
        longitudeRef: String? = "E",
    ) = RawExif(
        null,
        null,
        null,
        gpsLatitude = latitude,
        gpsLatitudeRef = latitudeRef,
        gpsLongitude = longitude,
        gpsLongitudeRef = longitudeRef,
    )

    @Test
    fun parse_northEast_addsMinutesAndSeconds() {
        val point = GpsParser.parse(exif(latitude = "45/1,37/1,1800/100", longitude = "10/1,40/1,3600/100"))

        assertEquals(45.621_666_667, point?.latitude ?: 0.0, 1e-6)
        assertEquals(10.676_666_667, point?.longitude ?: 0.0, 1e-6)
    }

    @Test
    fun parse_south_isNegative() {
        assertEquals(-45.5, GpsParser.parse(exif(latitudeRef = "S"))?.latitude ?: 0.0, 1e-9)
    }

    @Test
    fun parse_west_isNegative() {
        assertEquals(-10.25, GpsParser.parse(exif(longitudeRef = "W"))?.longitude ?: 0.0, 1e-9)
    }

    @Test
    fun parse_noGpsTags_returnsNull() {
        assertNull(GpsParser.parse(RawExif(null, null, null)))
    }

    @Test
    fun parse_missingRef_returnsNull() {
        assertNull(GpsParser.parse(exif(latitudeRef = null)))
    }

    @Test
    fun parse_unknownRef_returnsNull() {
        assertNull(GpsParser.parse(exif(longitudeRef = "X")))
    }

    @Test
    fun parse_tooFewParts_returnsNull() {
        assertNull(GpsParser.parse(exif(latitude = "45/1,30/1")))
    }

    @Test
    fun parse_notANumber_returnsNull() {
        assertNull(GpsParser.parse(exif(latitude = "north,30/1,0/1")))
    }

    @Test
    fun parse_zeroDenominator_returnsNull() {
        assertNull(GpsParser.parse(exif(latitude = "45/0,30/1,0/1")))
    }

    @Test
    fun parse_plainNumbers_areAccepted() {
        assertNotNull(GpsParser.parse(exif(latitude = "45,30,0")))
    }

    @Test
    fun parse_latitudeOutOfRange_returnsNull() {
        assertNull(GpsParser.parse(exif(latitude = "91/1,0/1,0/1")))
    }

    @Test
    fun parse_longitudeOutOfRange_returnsNull() {
        assertNull(GpsParser.parse(exif(longitude = "181/1,0/1,0/1")))
    }

    @Test
    fun parse_zeroZero_isTreatedAsRedacted() {
        assertNull(GpsParser.parse(exif(latitude = "0/1,0/1,0/1", longitude = "0/1,0/1,0/1")))
    }
}
