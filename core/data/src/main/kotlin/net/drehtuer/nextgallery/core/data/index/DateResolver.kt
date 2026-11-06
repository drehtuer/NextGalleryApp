package net.drehtuer.nextgallery.core.data.index

import net.drehtuer.nextgallery.core.model.DateSource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class ResolvedDate(val epochMillis: Long, val localDate: LocalDate, val source: DateSource)

/** Picks an image's date: `DATE_TAKEN` → EXIF `DateTimeOriginal` → `DATE_MODIFIED`, in the device time zone (D7). */
class DateResolver @Inject constructor() {
    fun resolve(dateTakenMillis: Long?, exif: ExifDateTime?, dateModifiedSeconds: Long, zone: ZoneId): ResolvedDate =
        when {
            dateTakenMillis != null && dateTakenMillis > 0 ->
                fromInstant(Instant.ofEpochMilli(dateTakenMillis), zone, DateSource.DATE_TAKEN)

            exif?.offset != null -> fromInstant(exif.dateTime.toInstant(exif.offset), zone, DateSource.EXIF)

            // Without an offset the written local time is taken as is, so the date matches what the camera showed.
            exif != null -> ResolvedDate(
                epochMillis = exif.dateTime.atZone(zone).toInstant().toEpochMilli(),
                localDate = exif.dateTime.toLocalDate(),
                source = DateSource.EXIF,
            )

            else -> fromInstant(Instant.ofEpochSecond(dateModifiedSeconds), zone, DateSource.DATE_MODIFIED)
        }

    private fun fromInstant(instant: Instant, zone: ZoneId, source: DateSource) =
        ResolvedDate(instant.toEpochMilli(), instant.atZone(zone).toLocalDate(), source)
}
