package net.drehtuer.nextgallery.core.database

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimelineDaoTest {
    private lateinit var database: NextGalleryDatabase
    private lateinit var mediaDao: MediaDao
    private lateinit var dao: TimelineDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NextGalleryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        mediaDao = database.mediaDao()
        dao = database.timelineDao()
    }

    @After
    fun tearDown() = database.close()

    private fun takenOn(id: Long, year: Int, month: Int, day: Int, takenMillis: Long = 1) =
        mediaEntity(id).copy(localYear = year, localMonth = month, localDay = day, dateTakenMillis = takenMillis)

    private suspend fun firstPageIds(): List<Long> {
        val params = PagingSource.LoadParams.Refresh<Int>(key = null, loadSize = 2, placeholdersEnabled = false)
        return (dao.paging().load(params) as PagingSource.LoadResult.Page).data.map { it.id }
    }

    @Test
    fun paging_ordersByLocalDateBeforeTakenTime() = runTest {
        mediaDao.upsertAll(listOf(takenOn(1, 2024, 3, 15, takenMillis = 99), takenOn(2, 2024, 3, 16, takenMillis = 1)))

        assertEquals(listOf(2L, 1L), firstPageIds())
    }

    @Test
    fun paging_sameDay_ordersByTakenTimeDescending() = runTest {
        mediaDao.upsertAll(listOf(takenOn(1, 2024, 3, 15, takenMillis = 20), takenOn(2, 2024, 3, 15, takenMillis = 10)))

        assertEquals(listOf(1L, 2L), firstPageIds())
    }

    @Test
    fun paging_includesAllBuckets() = runTest {
        mediaDao.upsertAll(listOf(mediaEntity(1), mediaEntity(2).copy(bucketId = 8)))

        assertEquals(setOf(1L, 2L), firstPageIds().toSet())
    }

    @Test
    fun ids_matchesPagingOrder() = runTest {
        mediaDao.upsertAll(listOf(takenOn(1, 2023, 12, 31), takenOn(2, 2024, 1, 1), takenOn(3, 2024, 1, 1)))

        assertEquals(listOf(3L, 2L, 1L), dao.ids())
    }

    @Test
    fun indexOf_countsImagesOfNewerDays() = runTest {
        mediaDao.upsertAll(listOf(takenOn(1, 2023, 12, 31), takenOn(2, 2024, 1, 1), takenOn(3, 2024, 2, 1)))

        assertEquals(2, dao.indexOf(1))
    }

    @Test
    fun indexOf_newerTimeOnOlderDay_stillCountsDayFirst() = runTest {
        mediaDao.upsertAll(listOf(takenOn(1, 2024, 3, 15, takenMillis = 99), takenOn(2, 2024, 3, 16, takenMillis = 1)))

        assertEquals(1, dao.indexOf(1))
    }

    @Test
    fun indexOf_equalDates_matchesPagingOrder() = runTest {
        mediaDao.upsertAll(listOf(mediaEntity(1), mediaEntity(2), mediaEntity(3)))

        assertEquals(2, dao.indexOf(1))
    }

    @Test
    fun observeDayCounts_countsImagesPerDay() = runTest {
        mediaDao.upsertAll(listOf(takenOn(1, 2024, 3, 15), takenOn(2, 2024, 3, 15), takenOn(3, 2024, 3, 16)))

        assertEquals(
            listOf(DayCountRow(2024, 3, 16, 1), DayCountRow(2024, 3, 15, 2)),
            dao.observeDayCounts().first(),
        )
    }

    @Test
    fun observeDayCounts_ordersByYearBeforeMonth() = runTest {
        mediaDao.upsertAll(listOf(takenOn(1, 2023, 12, 31), takenOn(2, 2024, 1, 1)))

        assertEquals(2024, dao.observeDayCounts().first().first().year)
    }

    @Test
    fun observeDayCounts_afterDelete_emitsNewCounts() = runTest {
        mediaDao.upsertAll(listOf(mediaEntity(1)))

        mediaDao.deleteByIds(listOf(1))

        assertEquals(emptyList<DayCountRow>(), dao.observeDayCounts().first())
    }
}
