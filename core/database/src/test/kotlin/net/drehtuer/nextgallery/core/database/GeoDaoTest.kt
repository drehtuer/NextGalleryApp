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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GeoDaoTest {
    private lateinit var database: NextGalleryDatabase
    private lateinit var dao: GeoDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NextGalleryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.geoDao()
    }

    @After
    fun tearDown() = database.close()

    private fun entity(id: Long, latitude: Double? = 45.5, longitude: Double? = 10.5, date: Long = id) =
        mediaEntity(id).copy(latitude = latitude, longitude = longitude, dateTakenMillis = date)

    private suspend fun insert(vararg entities: MediaEntity) = database.mediaDao().upsertAll(entities.toList())

    private suspend fun pagedIds(source: PagingSource<Int, MediaEntity>): List<Long> {
        val page = source.load(PagingSource.LoadParams.Refresh(null, 10, false))
        return (page as PagingSource.LoadResult.Page).data.map { it.id }
    }

    @Test
    fun observeGeotagged_leavesOutImagesWithoutLocation() = runTest {
        insert(entity(1), entity(2, latitude = null, longitude = null))

        assertEquals(listOf(1L), dao.observeGeotagged().first().map { it.id })
    }

    @Test
    fun observeGeotagged_isNewestFirst() = runTest {
        insert(entity(1, date = 100), entity(2, date = 200))

        assertEquals(listOf(2L, 1L), dao.observeGeotagged().first().map { it.id })
    }

    @Test
    fun observeGeotagged_readsPositionAndDate() = runTest {
        insert(entity(1, date = 100))

        assertEquals(GeotaggedRow(1, 45.5, 10.5, 100), dao.observeGeotagged().first().single())
    }

    @Test
    fun unscanned_returnsOnlyUnscanned() = runTest {
        insert(entity(1), entity(2).copy(exifScanned = true))

        assertEquals(listOf(1L), dao.unscanned(limit = 10).map { it.id })
    }

    @Test
    fun unscanned_respectsLimit() = runTest {
        insert(entity(1), entity(2))

        assertEquals(1, dao.unscanned(limit = 1).size)
    }

    @Test
    fun setLocations_storesLocationAndMarksScanned() = runTest {
        insert(entity(1, latitude = null, longitude = null))

        dao.setLocations(listOf(LocationRow(1, 46.0, 11.0)))

        val stored = database.mediaDao().getById(1)
        assertEquals(listOf(46.0, 11.0), listOf(stored?.latitude, stored?.longitude))
        assertEquals(true, stored?.exifScanned)
    }

    @Test
    fun setLocations_noLocation_stillMarksScanned() = runTest {
        insert(entity(1))

        dao.setLocations(listOf(LocationRow(1, null, null)))

        assertTrue(dao.unscanned(limit = 10).isEmpty())
    }

    @Test
    fun pagingInBounds_returnsOnlyImagesInside() = runTest {
        insert(entity(1), entity(2, latitude = 60.0), entity(3, longitude = 20.0), entity(4, null, null))

        assertEquals(listOf(1L), pagedIds(dao.pagingInBounds(45.0, 10.0, 46.0, 11.0)))
    }

    @Test
    fun pagingInBounds_pointBounds_findsExactlyThatImage() = runTest {
        insert(entity(1, latitude = 45.123456789, longitude = 10.987654321))

        assertEquals(listOf(1L), pagedIds(dao.pagingInBounds(45.123456789, 10.987654321, 45.123456789, 10.987654321)))
    }

    @Test
    fun pagingInBounds_isNewestFirst() = runTest {
        insert(entity(1, date = 100), entity(2, date = 200))

        assertEquals(listOf(2L, 1L), pagedIds(dao.pagingInBounds(45.0, 10.0, 46.0, 11.0)))
    }

    @Test
    fun indexInBounds_countsOnlyImagesInsideBeforeIt() = runTest {
        insert(entity(1, date = 100), entity(2, date = 200), entity(3, date = 300, latitude = 60.0))

        assertEquals(1, dao.indexInBounds(45.0, 10.0, 46.0, 11.0, id = 1))
    }

    @Test
    fun indexInBounds_sameDate_ordersByIdLikePaging() = runTest {
        insert(entity(1, date = 100), entity(2, date = 100))

        assertEquals(1, dao.indexInBounds(45.0, 10.0, 46.0, 11.0, id = 1))
    }
}
