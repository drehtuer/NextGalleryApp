package net.drehtuer.nextgallery.core.database

import android.content.Context
import androidx.paging.PagingConfig
import androidx.paging.testing.asSnapshot
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.model.DayCount
import net.drehtuer.nextgallery.core.model.MediaId
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class RoomTimelineStoreTest {
    private lateinit var database: NextGalleryDatabase
    private lateinit var store: RoomTimelineStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NextGalleryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        store = RoomTimelineStore(database.timelineDao())
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun paged_emitsMappedModels() = runTest {
        database.mediaDao().upsertAll(listOf(mediaEntity(1)))

        val snapshot = store.paged(PagingConfig(pageSize = 10)).asSnapshot()

        assertEquals(listOf(mediaEntity(1).toModel()), snapshot)
    }

    @Test
    fun ids_mapsToMediaIds() = runTest {
        database.mediaDao().upsertAll(listOf(mediaEntity(1)))

        assertEquals(listOf(MediaId(1)), store.ids())
    }

    @Test
    fun indexOf_returnsPosition() = runTest {
        database.mediaDao().upsertAll(listOf(mediaEntity(1), mediaEntity(2)))

        assertEquals(1, store.indexOf(MediaId(1)))
    }

    @Test
    fun observeDayCounts_mapsRowsToLocalDates() = runTest {
        database.mediaDao().upsertAll(listOf(mediaEntity(1)))

        assertEquals(listOf(DayCount(LocalDate.of(2024, 3, 15), 1)), store.observeDayCounts().first())
    }
}
