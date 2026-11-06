package net.drehtuer.nextgallery.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
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
    fun observeCount_returnsNumberOfRows() = runTest {
        dao.upsertAll(listOf(mediaEntity(1), mediaEntity(2)))

        assertEquals(2, dao.observeCount().first())
    }
}
