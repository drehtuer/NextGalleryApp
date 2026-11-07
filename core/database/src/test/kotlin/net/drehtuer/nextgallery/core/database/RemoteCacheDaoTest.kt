package net.drehtuer.nextgallery.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RemoteCacheDaoTest {
    private lateinit var database: NextGalleryDatabase
    private lateinit var dao: RemoteCacheDao

    private fun entity(path: String, parent: String) = RemoteCacheEntity(
        path = path,
        parentPath = parent,
        name = path.substringAfterLast('/'),
        isDirectory = false,
        sizeBytes = 1,
        contentType = "image/jpeg",
        etag = "e",
        fileId = 1,
        checksums = null,
        permissions = null,
        hasPreview = true,
        childCount = null,
        lastModifiedMillis = 1,
        fetchedAtMillis = 2,
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NextGalleryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.remoteCacheDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun getFolder_returnsOnlyThatFolder() = runTest {
        dao.insert(listOf(entity("/A/a.jpg", "/A"), entity("/B/b.jpg", "/B")))

        assertEquals(listOf("/A/a.jpg"), dao.getFolder("/A").map { it.path })
    }

    @Test
    fun replaceFolder_removesWhatIsGone() = runTest {
        dao.insert(listOf(entity("/A/old.jpg", "/A")))

        dao.replaceFolder("/A", listOf(entity("/A/new.jpg", "/A")))

        assertEquals(listOf("/A/new.jpg"), dao.getFolder("/A").map { it.path })
    }

    @Test
    fun replaceFolder_leavesOtherFoldersAlone() = runTest {
        dao.insert(listOf(entity("/B/b.jpg", "/B")))

        dao.replaceFolder("/A", emptyList())

        assertEquals(1, dao.getFolder("/B").size)
    }

    @Test
    fun clear_removesEverything() = runTest {
        dao.insert(listOf(entity("/A/a.jpg", "/A"), entity("/B/b.jpg", "/B")))

        dao.clear()

        assertTrue(dao.getFolder("/A").isEmpty())
        assertTrue(dao.getFolder("/B").isEmpty())
    }
}
