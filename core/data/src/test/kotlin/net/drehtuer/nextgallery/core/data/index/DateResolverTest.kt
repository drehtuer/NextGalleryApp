package net.drehtuer.nextgallery.core.data.index

import net.drehtuer.nextgallery.core.model.DateSource
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class DateResolverTest {
    private val resolver = DateResolver()
    private val berlin = ZoneId.of("Europe/Berlin")

    @Test
    fun resolve_dateTakenPresent_usesDateTaken() {
        // 2024-03-15T23:30Z is already the 16th in Berlin.
        val result = resolver.resolve(1_710_545_400_000L, null, 0, berlin)

        assertEquals(ResolvedDate(1_710_545_400_000L, LocalDate.of(2024, 3, 16), DateSource.DATE_TAKEN), result)
    }

    @Test
    fun resolve_dateTakenZero_fallsBackToExif() {
        val exif = ExifDateTime(LocalDateTime.of(2024, 3, 15, 10, 0), null)

        assertEquals(DateSource.EXIF, resolver.resolve(0, exif, 0, berlin).source)
    }

    @Test
    fun resolve_exifWithoutOffset_keepsWrittenDate() {
        val exif = ExifDateTime(LocalDateTime.of(2024, 3, 15, 23, 30), null)

        val result = resolver.resolve(null, exif, 0, berlin)

        assertEquals(LocalDate.of(2024, 3, 15), result.localDate)
    }

    @Test
    fun resolve_exifWithOffset_convertsToDeviceZone() {
        // 23:30 at UTC-05:00 is 05:30 the next day in Berlin.
        val exif = ExifDateTime(LocalDateTime.of(2024, 3, 15, 23, 30), ZoneOffset.ofHours(-5))

        val result = resolver.resolve(null, exif, 0, berlin)

        assertEquals(LocalDate.of(2024, 3, 16), result.localDate)
    }

    @Test
    fun resolve_noDateTakenNoExif_usesDateModified() {
        val result = resolver.resolve(null, null, 1_710_500_000L, berlin)

        assertEquals(ResolvedDate(1_710_500_000_000L, LocalDate.of(2024, 3, 15), DateSource.DATE_MODIFIED), result)
    }
}
