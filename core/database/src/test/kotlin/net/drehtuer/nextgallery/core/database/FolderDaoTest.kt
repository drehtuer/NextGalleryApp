package net.drehtuer.nextgallery.core.database

import android.content.Context
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
class FolderDaoTest {
    private lateinit var database: NextGalleryDatabase
    private lateinit var mediaDao: MediaDao
    private lateinit var folderDao: FolderDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NextGalleryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        mediaDao = database.mediaDao()
        folderDao = database.folderDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun observeFolders_groupsImagesByBucket() = runTest {
        mediaDao.upsertAll(listOf(mediaEntity(1), mediaEntity(2), mediaEntity(3).copy(bucketId = 8)))

        val buckets = folderDao.observeFolders().first().map { it.cover.bucketId }

        assertEquals(setOf(7L, 8L), buckets.toSet())
    }

    @Test
    fun observeFolders_countsImagesOfEachBucket() = runTest {
        mediaDao.upsertAll(listOf(mediaEntity(1), mediaEntity(2), mediaEntity(3).copy(bucketId = 8)))

        val counts = folderDao.observeFolders().first().associate { it.cover.bucketId to it.itemCount }

        assertEquals(mapOf(7L to 2, 8L to 1), counts)
    }

    @Test
    fun observeFolders_coverIsNewestImage() = runTest {
        mediaDao.upsertAll(listOf(mediaEntity(1).copy(dateTakenMillis = 20), mediaEntity(2).copy(dateTakenMillis = 10)))

        assertEquals(1L, folderDao.observeFolders().first().single().cover.id)
    }

    @Test
    fun observeFolders_equalDates_coverIsHighestId() = runTest {
        mediaDao.upsertAll(listOf(mediaEntity(1), mediaEntity(2)))

        assertEquals(2L, folderDao.observeFolders().first().single().cover.id)
    }

    @Test
    fun observeFolders_sortsByNameIgnoringCase() = runTest {
        mediaDao.upsertAll(
            listOf(
                mediaEntity(1).copy(bucketId = 1, bucketName = "b"),
                mediaEntity(2).copy(bucketId = 2, bucketName = "C"),
                mediaEntity(3).copy(bucketId = 3, bucketName = "A"),
            ),
        )

        assertEquals(listOf("A", "b", "C"), folderDao.observeFolders().first().map { it.cover.bucketName })
    }
}
