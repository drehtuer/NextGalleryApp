package net.drehtuer.nextgallery.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncStateDaoTest {
    private lateinit var database: NextGalleryDatabase
    private lateinit var dao: SyncStateDao
    private val state = SyncStateEntity("external_primary", 12, "v1", 5_000)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NextGalleryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.syncStateDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun get_emptyTable_returnsNull() = runTest {
        assertNull(dao.get("external_primary"))
    }

    @Test
    fun put_thenGet_returnsStoredState() = runTest {
        dao.put(state)

        assertEquals(state, dao.get("external_primary"))
    }

    @Test
    fun hasAny_emptyTable_returnsFalse() = runTest {
        assertFalse(dao.hasAny())
    }

    @Test
    fun hasAny_afterPut_returnsTrue() = runTest {
        dao.put(state)

        assertTrue(dao.hasAny())
    }
}
