package net.drehtuer.nextgallery.core.data.index

import net.drehtuer.nextgallery.core.model.RawExif
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class ExifDateParserTest {
    private val parser = ExifDateParser()

    @Test
    fun parse_dateTimeOnly_returnsDateWithoutOffset() {
        val result = parser.parse(RawExif("2024:03:15 14:30:05", null, null))

        assertEquals(ExifDateTime(LocalDateTime.of(2024, 3, 15, 14, 30, 5), null), result)
    }

    @Test
    fun parse_withOffset_returnsOffset() {
        val result = parser.parse(RawExif("2024:03:15 14:30:05", "+02:00", null))

        assertEquals(ZoneOffset.ofHours(2), result?.offset)
    }

    @Test
    fun parse_withSubSeconds_addsFraction() {
        val result = parser.parse(RawExif("2024:03:15 14:30:05", null, "25"))

        assertEquals(250_000_000, result?.dateTime?.nano)
    }

    @Test
    fun parse_missingDateTime_returnsNull() {
        assertNull(parser.parse(RawExif(null, "+02:00", null)))
    }

    @Test
    fun parse_malformedDateTime_returnsNull() {
        assertNull(parser.parse(RawExif("0000:00:00 00:00:00", null, null)))
    }

    @Test
    fun parse_malformedOffset_ignoresOffset() {
        val result = parser.parse(RawExif("2024:03:15 14:30:05", "nonsense", null))

        assertNull(result?.offset)
    }
}
