package net.drehtuer.nextgallery.core.database

import android.content.Context
import androidx.paging.PagingConfig
import androidx.paging.testing.asSnapshot
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomMediaIndexStorePagingTest {
    private lateinit var database: NextGalleryDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NextGalleryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun pagedByBuckets_emitsMappedModels() = runTest {
        database.mediaDao().upsertAll(listOf(mediaEntity(1)))
        val store = RoomMediaIndexStore(database.mediaDao(), database.folderDao())

        val snapshot = store.pagedByBuckets(listOf(7, 8), PagingConfig(pageSize = 10)).asSnapshot()

        assertEquals(listOf(mediaEntity(1).toModel()), snapshot)
    }
}
