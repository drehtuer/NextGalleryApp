package net.drehtuer.nextgallery.core.data.index

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.data.fakes.FakeExifSource
import net.drehtuer.nextgallery.core.data.fakes.FakeGeoIndexStore
import net.drehtuer.nextgallery.core.data.fakes.mediaItem
import net.drehtuer.nextgallery.core.data.map.MapPermissions
import net.drehtuer.nextgallery.core.model.GeoPoint
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.RawExif
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationBackfillTest {
    private val gardaExif = RawExif(
        null,
        null,
        null,
        gpsLatitude = "45/1,30/1,0/1",
        gpsLatitudeRef = "N",
        gpsLongitude = "10/1,30/1,0/1",
        gpsLongitudeRef = "E",
    )
    private val store = FakeGeoIndexStore()
    private val permissions = mockk<MapPermissions> { every { photoLocationsAllowed() } returns true }

    private fun backfill(exif: RawExif? = gardaExif, source: FakeExifSource = FakeExifSource(exif)) =
        LocationBackfill(store, source, permissions)

    @Test
    fun run_permissionMissing_readsNothing() = runTest {
        every { permissions.photoLocationsAllowed() } returns false
        store.unscannedItems += mediaItem(1)
        val source = FakeExifSource(gardaExif)

        backfill(source = source).run()

        assertTrue(source.readUris.isEmpty())
    }

    @Test
    fun run_permissionMissing_leavesImagesUnscanned() = runTest {
        every { permissions.photoLocationsAllowed() } returns false
        store.unscannedItems += mediaItem(1)

        backfill().run()

        assertTrue(store.saved.isEmpty())
    }

    @Test
    fun run_imageWithGps_storesLocation() = runTest {
        store.unscannedItems += mediaItem(1)

        backfill().run()

        assertEquals(mapOf(MediaId(1) to GeoPoint(45.5, 10.5)), store.saved.toMap())
    }

    @Test
    fun run_onlyReadsUnscannedImages() = runTest {
        store.unscannedItems += mediaItem(1)
        val source = FakeExifSource(gardaExif)

        backfill(source = source).run()

        assertEquals(listOf("content://media/1"), source.readUris)
    }

    @Test
    fun run_imageWithoutGps_isMarkedScannedWithoutLocation() = runTest {
        store.unscannedItems += mediaItem(1)

        backfill(exif = RawExif(null, null, null)).run()

        assertEquals(mapOf(MediaId(1) to null), store.saved.toMap())
    }

    @Test
    fun run_unreadableFile_isMarkedScannedWithoutLocation() = runTest {
        store.unscannedItems += mediaItem(1)

        backfill(exif = null).run()

        assertEquals(mapOf(MediaId(1) to null), store.saved.toMap())
    }

    @Test
    fun run_formatWithoutGps_doesNotOpenTheFile() = runTest {
        store.unscannedItems += mediaItem(1, mimeType = "image/png")
        val source = FakeExifSource(gardaExif)

        backfill(source = source).run()

        assertTrue(source.readUris.isEmpty())
    }

    @Test
    fun run_moreThanOneBatch_scansAllImages() = runTest {
        store.unscannedItems += (1L..450L).map { mediaItem(it) }

        val scanned = backfill().run()

        assertEquals(450 to 450, scanned to store.saved.size)
    }
}
