package net.drehtuer.nextgallery.core.database

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaDaoTest {
    private lateinit var database: NextGalleryDatabase
    private lateinit var dao: MediaDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NextGalleryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.mediaDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun upsertAll_newRows_areStored() = runTest {
        dao.upsertAll(listOf(mediaEntity(1)))

        assertEquals(mediaEntity(1), dao.getById(1))
    }

    @Test
    fun upsertAll_existingRow_isReplaced() = runTest {
        dao.upsertAll(listOf(mediaEntity(1)))

        dao.upsertAll(listOf(mediaEntity(1).copy(displayName = "renamed.jpg")))

        assertEquals("renamed.jpg", dao.getById(1)?.displayName)
    }

    @Test
    fun deleteByIds_removesOnlyGivenRows() = runTest {
        dao.upsertAll(listOf(mediaEntity(1), mediaEntity(2), mediaEntity(3)))

        dao.deleteByIds(listOf(1, 3))

        assertEquals(listOf(2L), dao.idsForVolume("external_primary"))
    }

    @Test
    fun idsForVolume_returnsOnlyRowsOfThatVolume() = runTest {
        dao.upsertAll(listOf(mediaEntity(1, "external_primary"), mediaEntity(2, "1234-abcd")))

        assertEquals(listOf(2L), dao.idsForVolume("1234-abcd"))
    }

    @Test
    fun getById_unknownId_returnsNull() = runTest {
        assertNull(dao.getById(42))
    }

    @Test
    fun pagingByBucket_ordersByTakenDateDescending() = runTest {
        dao.upsertAll(listOf(mediaEntity(1).copy(dateTakenMillis = 10), mediaEntity(2).copy(dateTakenMillis = 20)))

        val page = dao.pagingByBucket(7).load(refresh(loadSize = 2)) as PagingSource.LoadResult.Page

        assertEquals(listOf(2L, 1L), page.data.map { it.id })
    }

    @Test
    fun pagingByBucket_equalTakenDates_ordersByIdDescending() = runTest {
        dao.upsertAll(listOf(mediaEntity(1), mediaEntity(2)))

        val page = dao.pagingByBucket(7).load(refresh(loadSize = 2)) as PagingSource.LoadResult.Page

        assertEquals(listOf(2L, 1L), page.data.map { it.id })
    }

    @Test
    fun pagingByBucket_limitsPageToLoadSize() = runTest {
        dao.upsertAll(listOf(mediaEntity(1), mediaEntity(2), mediaEntity(3)))

        val page = dao.pagingByBucket(7).load(refresh(loadSize = 2)) as PagingSource.LoadResult.Page

        assertEquals(2, page.data.size)
    }

    @Test
    fun pagingByBucket_skipsOtherBuckets() = runTest {
        dao.upsertAll(listOf(mediaEntity(1), mediaEntity(2).copy(bucketId = 8)))

        val page = dao.pagingByBucket(7).load(refresh(loadSize = 2)) as PagingSource.LoadResult.Page

        assertEquals(listOf(1L), page.data.map { it.id })
    }

    @Test
    fun idsInBucket_returnsIdsOfThatBucketNewestFirst() = runTest {
        dao.upsertAll(
            listOf(
                mediaEntity(1).copy(dateTakenMillis = 10),
                mediaEntity(2).copy(dateTakenMillis = 20),
                mediaEntity(3).copy(bucketId = 8),
            ),
        )

        assertEquals(listOf(2L, 1L), dao.idsInBucket(7))
    }

    @Test
    fun getByIds_returnsOnlyGivenRows() = runTest {
        dao.upsertAll(listOf(mediaEntity(1), mediaEntity(2)))

        assertEquals(listOf(2L), dao.getByIds(listOf(2, 9)).map { it.id })
    }

    @Test
    fun indexInBucket_countsNewerImages() = runTest {
        dao.upsertAll(
            listOf(
                mediaEntity(1).copy(dateTakenMillis = 30),
                mediaEntity(2).copy(dateTakenMillis = 20),
                mediaEntity(3).copy(dateTakenMillis = 10),
            ),
        )

        assertEquals(1, dao.indexInBucket(7, 2))
    }

    @Test
    fun indexInBucket_equalDates_matchesPagingOrder() = runTest {
        dao.upsertAll(listOf(mediaEntity(1), mediaEntity(2), mediaEntity(3)))

        assertEquals(2, dao.indexInBucket(7, 1))
    }

    @Test
    fun indexInBucket_ignoresOtherBuckets() = runTest {
        dao.upsertAll(listOf(mediaEntity(1), mediaEntity(2).copy(bucketId = 8, dateTakenMillis = Long.MAX_VALUE)))

        assertEquals(0, dao.indexInBucket(7, 1))
    }

    private fun refresh(loadSize: Int) =
        PagingSource.LoadParams.Refresh<Int>(key = null, loadSize = loadSize, placeholdersEnabled = false)
}
