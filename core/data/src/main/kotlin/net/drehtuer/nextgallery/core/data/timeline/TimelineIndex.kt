package net.drehtuer.nextgallery.core.data.timeline

import net.drehtuer.nextgallery.core.model.DayCount
import java.time.LocalDate
import java.time.YearMonth

/** A row of the timeline grid. [Photo.photoIndex] is the image's position in the paged list of images. */
sealed interface TimelineRow {
    data class MonthHeader(val month: YearMonth) : TimelineRow

    data class DayHeader(val date: LocalDate) : TimelineRow

    data class Photo(val photoIndex: Int) : TimelineRow
}

/** The dates of the date rail (D43): the day at a position and the days with images above and below it. */
data class DayRailDates(val above: LocalDate?, val current: LocalDate, val below: LocalDate?)

/**
 * The layout of the timeline, from the number of images per day, newest day first: every month starts with a
 * month header, every day with a day header. All positions are known without loading any image, so the date rail
 * can jump anywhere. Lookups are binary searches. [days] must not be empty for the `At` functions.
 */
data class TimelineIndex(val days: List<DayCount>) {
    /** Position of the first row of each day: its month header if it starts a month, else its day header. */
    private val firstRows = IntArray(days.size)

    /** Position of each day's header. */
    private val dayHeaders = IntArray(days.size)

    /** Position in the paged list of each day's first image. */
    private val firstPhotos = IntArray(days.size)

    val rowCount: Int

    init {
        var row = 0
        var photo = 0
        days.forEachIndexed { day, (_, count) ->
            firstRows[day] = row
            if (startsMonth(day)) row++
            dayHeaders[day] = row
            firstPhotos[day] = photo
            row += 1 + count
            photo += count
        }
        rowCount = row
    }

    val isEmpty: Boolean get() = days.isEmpty()

    fun isHeader(position: Int): Boolean = position <= dayHeaders[dayAt(position)]

    fun rowAt(position: Int): TimelineRow {
        val day = dayAt(position)
        return when {
            position == dayHeaders[day] -> TimelineRow.DayHeader(days[day].date)
            position < dayHeaders[day] -> TimelineRow.MonthHeader(YearMonth.from(days[day].date))
            else -> TimelineRow.Photo(firstPhotos[day] + position - dayHeaders[day] - 1)
        }
    }

    /** The month a position belongs to, for the sticky header. */
    fun monthAt(position: Int): YearMonth = YearMonth.from(days[dayAt(position)].date)

    fun dayRailAt(position: Int): DayRailDates {
        val day = dayAt(position)
        return DayRailDates(
            above = days.getOrNull(day - 1)?.date,
            current = days[day].date,
            below = days.getOrNull(day + 1)?.date,
        )
    }

    private fun startsMonth(day: Int) = day == 0 || YearMonth.from(days[day - 1].date) != YearMonth.from(days[day].date)

    private fun dayAt(position: Int): Int {
        val found = firstRows.binarySearch(position)
        return if (found >= 0) found else -found - 2
    }
}
