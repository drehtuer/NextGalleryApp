package net.drehtuer.nextgallery.core.data.timeline

import net.drehtuer.nextgallery.core.model.DayCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * The rows of [index]:
 *
 *     0 month March 2024    1 day 16 March    2-3 photos 0-1
 *                           4 day 2 March     5 photo 2
 *     6 month December 2023 7 day 31 December 8 photo 3
 *     9 month December 2022 10 day 1 December 11 photo 4
 */
class TimelineIndexTest {
    private val march16 = LocalDate.of(2024, 3, 16)
    private val march2 = LocalDate.of(2024, 3, 2)
    private val december31 = LocalDate.of(2023, 12, 31)
    private val december2022 = LocalDate.of(2022, 12, 1)
    private val index = TimelineIndex(
        listOf(DayCount(march16, 2), DayCount(march2, 1), DayCount(december31, 1), DayCount(december2022, 1)),
    )

    @Test
    fun rowCount_countsPhotosAndHeaders() {
        assertEquals(12, index.rowCount)
    }

    @Test
    fun isEmpty_noDays_isTrue() {
        assertTrue(TimelineIndex(emptyList()).isEmpty)
    }

    @Test
    fun isEmpty_withDays_isFalse() {
        assertFalse(index.isEmpty)
    }

    @Test
    fun rowAt_firstRow_isMonthHeader() {
        assertEquals(TimelineRow.MonthHeader(YearMonth.of(2024, 3)), index.rowAt(0))
    }

    @Test
    fun rowAt_afterMonthHeader_isDayHeader() {
        assertEquals(TimelineRow.DayHeader(march16), index.rowAt(1))
    }

    @Test
    fun rowAt_afterDayHeader_isFirstPhoto() {
        assertEquals(TimelineRow.Photo(0), index.rowAt(2))
    }

    @Test
    fun rowAt_lastPhotoOfDay_countsWithinDay() {
        assertEquals(TimelineRow.Photo(1), index.rowAt(3))
    }

    @Test
    fun rowAt_newDayInSameMonth_hasNoMonthHeader() {
        assertEquals(TimelineRow.DayHeader(march2), index.rowAt(4))
    }

    @Test
    fun rowAt_photoAfterSeveralDays_skipsTheirHeaders() {
        assertEquals(TimelineRow.Photo(2), index.rowAt(5))
    }

    @Test
    fun rowAt_newMonth_startsWithMonthHeader() {
        assertEquals(TimelineRow.MonthHeader(YearMonth.of(2023, 12)), index.rowAt(6))
    }

    @Test
    fun rowAt_sameMonthOfAnotherYear_startsWithMonthHeader() {
        assertEquals(TimelineRow.MonthHeader(YearMonth.of(2022, 12)), index.rowAt(9))
    }

    @Test
    fun rowAt_lastRow_isLastPhoto() {
        assertEquals(TimelineRow.Photo(4), index.rowAt(11))
    }

    @Test
    fun isHeader_monthHeader_isTrue() {
        assertTrue(index.isHeader(6))
    }

    @Test
    fun isHeader_dayHeader_isTrue() {
        assertTrue(index.isHeader(7))
    }

    @Test
    fun isHeader_photo_isFalse() {
        assertFalse(index.isHeader(8))
    }

    @Test
    fun monthAt_photo_isMonthOfItsDay() {
        assertEquals(YearMonth.of(2023, 12), index.monthAt(8))
    }

    @Test
    fun dayRailAt_middle_hasBothNeighbours() {
        assertEquals(DayRailDates(march16, march2, december31), index.dayRailAt(5))
    }

    @Test
    fun dayRailAt_monthHeader_isItsFirstDay() {
        assertEquals(march16, index.dayRailAt(0).current)
    }

    @Test
    fun dayRailAt_newestDay_hasNoDayAbove() {
        assertNull(index.dayRailAt(2).above)
    }

    @Test
    fun dayRailAt_oldestDay_hasNoDayBelow() {
        assertNull(index.dayRailAt(11).below)
    }
}
