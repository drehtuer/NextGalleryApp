package net.drehtuer.nextgallery.core.database

import android.content.Context
import androidx.paging.PagingConfig
import androidx.paging.testing.asSnapshot
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.model.GeoBounds
import net.drehtuer.nextgallery.core.model.GeoPoint
import net.drehtuer.nextgallery.core.model.GeotaggedImage
import net.drehtuer.nextgallery.core.model.MediaId
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomGeoIndexStoreTest {
    private lateinit var database: NextGalleryDatabase
    private lateinit var store: RoomGeoIndexStore
    private val bounds = GeoBounds(south = 48.0, west = 11.0, north = 49.0, east = 12.0)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NextGalleryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        store = RoomGeoIndexStore(database.geoDao())
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun observeGeotagged_mapsRows() = runTest {
        val entity = mediaEntity(1)
        database.mediaDao().upsertAll(listOf(entity))

        assertEquals(
            listOf(GeotaggedImage(MediaId(1), GeoPoint(48.1, 11.5), entity.dateTakenMillis)),
            store.observeGeotagged().first(),
        )
    }

    @Test
    fun unscanned_mapsRows() = runTest {
        database.mediaDao().upsertAll(listOf(mediaEntity(1)))

        assertEquals(listOf(MediaId(1)), store.unscanned(limit = 10).map { it.id })
    }

    @Test
    fun saveLocations_storesLocation() = runTest {
        database.mediaDao().upsertAll(listOf(mediaEntity(1)))

        store.saveLocations(mapOf(MediaId(1) to GeoPoint(45.5, 10.5)))

        val stored = database.mediaDao().getById(1)
        assertEquals(listOf(45.5, 10.5), listOf(stored?.latitude, stored?.longitude))
    }

    @Test
    fun saveLocations_noLocation_clearsIt() = runTest {
        database.mediaDao().upsertAll(listOf(mediaEntity(1)))

        store.saveLocations(mapOf(MediaId(1) to null))

        assertEquals(null, database.mediaDao().getById(1)?.latitude)
    }

    @Test
    fun pagedInBounds_emitsMappedModels() = runTest {
        database.mediaDao().upsertAll(listOf(mediaEntity(1)))

        val snapshot = store.pagedInBounds(bounds, PagingConfig(pageSize = 10)).asSnapshot()

        assertEquals(listOf(mediaEntity(1).toModel()), snapshot)
    }

    @Test
    fun indexInBounds_returnsPosition() = runTest {
        database.mediaDao().upsertAll(listOf(mediaEntity(1)))

        assertEquals(0, store.indexInBounds(bounds, MediaId(1)))
    }
}
