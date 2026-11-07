package net.drehtuer.nextgallery.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UploadStateDaoTest {
    private lateinit var database: NextGalleryDatabase
    private lateinit var dao: UploadStateDao
    private lateinit var mediaDao: MediaDao

    /** Matches the size and date of [mediaEntity]. */
    private fun row(id: Long, size: Long = 1_000, dateModified: Long = 1_710_500_000L) = UploadStateEntity(
        mediaId = id,
        remotePath = "/P/IMG_$id.jpg",
        state = UploadStateEntity.STATE_UPLOADED,
        checksumVerified = true,
        localSize = size,
        localDateModifiedSeconds = dateModified,
        remoteEtag = "file",
        remoteFolderEtag = "folder",
        checkedAtMillis = 5,
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NextGalleryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.uploadStateDao()
        mediaDao = database.mediaDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun upsertExisting_replacesTheRowOfAnImage() = runTest {
        mediaDao.upsertAll(listOf(mediaEntity(1)))
        dao.upsertExisting(listOf(row(1)))

        dao.upsertExisting(listOf(row(1).copy(state = UploadStateEntity.STATE_NOT_UPLOADED)))

        assertEquals(listOf(UploadStateEntity.STATE_NOT_UPLOADED), dao.statusFor(listOf(1)).map { it.state })
    }

    @Test
    fun upsertExisting_imageNoLongerIndexed_leavesItOutAndKeepsTheOthers() = runTest {
        mediaDao.upsertAll(listOf(mediaEntity(1)))

        dao.upsertExisting(listOf(row(1), row(2)))

        assertEquals(listOf(1L), dao.statusFor(listOf(1, 2)).map { it.mediaId })
    }

    @Test
    fun deletingAnImage_deletesItsRow() = runTest {
        mediaDao.upsertAll(listOf(mediaEntity(1)))
        dao.upsertExisting(listOf(row(1)))

        mediaDao.deleteByIds(listOf(1))

        assertTrue(dao.rowsInFolder("DCIM/Camera/").isEmpty())
    }

    @Test
    fun updatingAnImageInTheIndex_keepsItsRow() = runTest {
        mediaDao.upsertAll(listOf(mediaEntity(1)))
        dao.upsertExisting(listOf(row(1)))

        mediaDao.upsertAll(listOf(mediaEntity(1).copy(generationModified = 9)))

        assertEquals(1, dao.rowsInFolder("DCIM/Camera/").size)
    }

    @Test
    fun statusFor_returnsOnlyTheGivenIds() = runTest {
        mediaDao.upsertAll(listOf(mediaEntity(1), mediaEntity(2)))
        dao.upsertExisting(listOf(row(1), row(2)))

        assertEquals(listOf(2L), dao.statusFor(listOf(2)).map { it.mediaId })
    }

    @Test
    fun statusFor_imageChangedInSize_isLeftOut() = runTest {
        mediaDao.upsertAll(listOf(mediaEntity(1)))
        dao.upsertExisting(listOf(row(1, size = 999)))

        assertTrue(dao.statusFor(listOf(1)).isEmpty())
    }

    @Test
    fun statusFor_imageModified_isLeftOut() = runTest {
        mediaDao.upsertAll(listOf(mediaEntity(1)))
        dao.upsertExisting(listOf(row(1, dateModified = 1)))

        assertTrue(dao.statusFor(listOf(1)).isEmpty())
    }

    @Test
    fun observeStatusFor_emitsAgainOnChange() = runTest {
        mediaDao.upsertAll(listOf(mediaEntity(1)))

        dao.observeStatusFor(listOf(1)).test {
            assertTrue(awaitItem().isEmpty())
            dao.upsertExisting(listOf(row(1)))
            assertEquals(listOf(UploadStatusRow(1, UploadStateEntity.STATE_UPLOADED, true)), awaitItem())
            cancel()
        }
    }

    @Test
    fun localFolders_areDistinct() = runTest {
        mediaDao.upsertAll(
            listOf(mediaEntity(1), mediaEntity(2), mediaEntity(3).copy(relativePath = "Pictures/")),
        )

        assertEquals(setOf("DCIM/Camera/", "Pictures/"), dao.localFolders().toSet())
        assertEquals(2, dao.localFolders().size)
    }

    @Test
    fun mediaInFolder_returnsOnlyThatFolder() = runTest {
        mediaDao.upsertAll(listOf(mediaEntity(1), mediaEntity(2).copy(relativePath = "Pictures/")))

        assertEquals(listOf(2L), dao.mediaInFolder("Pictures/").map { it.id })
    }

    @Test
    fun rowsInFolder_returnsOnlyThatFolder() = runTest {
        mediaDao.upsertAll(listOf(mediaEntity(1), mediaEntity(2).copy(relativePath = "Pictures/")))
        dao.upsertExisting(listOf(row(1), row(2)))

        assertEquals(listOf(2L), dao.rowsInFolder("Pictures/").map { it.mediaId })
    }

    @Test
    fun clear_removesAllRowsButNoImages() = runTest {
        mediaDao.upsertAll(listOf(mediaEntity(1)))
        dao.upsertExisting(listOf(row(1)))

        dao.clear()

        assertTrue(dao.rowsInFolder("DCIM/Camera/").isEmpty())
        assertEquals(1, dao.mediaInFolder("DCIM/Camera/").size)
    }
}
