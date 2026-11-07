package net.drehtuer.nextgallery.core.data.timeline

import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.data.fakes.FakeTimelineStore
import net.drehtuer.nextgallery.core.model.DayCount
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DefaultTimelineRepositoryTest {
    private val store = FakeTimelineStore()
    private val repository = DefaultTimelineRepository(store)
    private val day = DayCount(LocalDate.of(2024, 3, 16), 2)

    @Test
    fun observeIndex_buildsIndexFromDayCounts() = runTest {
        store.dayCounts.value = listOf(day)

        assertEquals(TimelineIndex(listOf(day)), repository.observeIndex().first())
    }

    @Test
    fun observeIndex_changedCounts_emitsAgain() = runTest {
        repository.observeIndex().test {
            awaitItem()

            store.dayCounts.value = listOf(day)

            assertEquals(4, awaitItem().rowCount)
        }
    }
}
