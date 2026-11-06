package net.drehtuer.nextgallery.core.data.index

import net.drehtuer.nextgallery.core.model.RawExif
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject

/** Date and optional UTC offset from EXIF `DateTimeOriginal`, `OffsetTimeOriginal` and `SubSecTimeOriginal`. */
data class ExifDateTime(val dateTime: LocalDateTime, val offset: ZoneOffset?)

class ExifDateParser @Inject constructor() {
    fun parse(raw: RawExif): ExifDateTime? {
        val dateTime = raw.dateTimeOriginal?.trim()?.let(::dateTime) ?: return null
        return ExifDateTime(dateTime.plusNanos(subSecondNanos(raw.subSecTimeOriginal)), offset(raw.offsetTimeOriginal))
    }

    private fun dateTime(text: String): LocalDateTime? = try {
        LocalDateTime.parse(text, FORMAT)
    } catch (_: DateTimeParseException) {
        null
    }

    private fun subSecondNanos(subSec: String?): Long {
        val digits = subSec?.trim().orEmpty()
        if (digits.isEmpty() || !digits.all(Char::isDigit)) return 0
        return digits.take(NANO_DIGITS).padEnd(NANO_DIGITS, '0').toLong()
    }

    private fun offset(text: String?): ZoneOffset? = try {
        text?.trim()?.let(ZoneOffset::of)
    } catch (_: java.time.DateTimeException) {
        null
    }

    private companion object {
        val FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
        const val NANO_DIGITS = 9
    }
}
