package net.drehtuer.nextgallery.core.data.index

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.data.fakes.FakeClock
import net.drehtuer.nextgallery.core.data.fakes.FakeExifSource
import net.drehtuer.nextgallery.core.data.fakes.FakeMediaIndexStore
import net.drehtuer.nextgallery.core.data.fakes.FakeMediaScanSource
import net.drehtuer.nextgallery.core.data.fakes.FakeMediaScanSource.Companion.VOLUME
import net.drehtuer.nextgallery.core.data.fakes.FakeSyncStateStore
import net.drehtuer.nextgallery.core.data.fakes.mediaItem
import net.drehtuer.nextgallery.core.data.fakes.scannedImage
import net.drehtuer.nextgallery.core.data.settings.SettingsRepository
import net.drehtuer.nextgallery.core.model.DateSource
import net.drehtuer.nextgallery.core.model.FolderRule
import net.drehtuer.nextgallery.core.model.GeoPoint
import net.drehtuer.nextgallery.core.model.RawExif
import net.drehtuer.nextgallery.core.model.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MediaIndexerTest {
    private val scanSource = FakeMediaScanSource()
    private val exifSource = FakeExifSource(RawExif("2024:01:01 10:00:00", null, null))
    private val store = FakeMediaIndexStore()
    private val clock = FakeClock(now = 5_000L)
    private val resolvedDate = ResolvedDate(1L, LocalDate.of(2024, 1, 1), DateSource.DATE_TAKEN)
    private val dateResolver = mockk<DateResolver> {
        every { resolve(any(), any(), any(), any()) } returns resolvedDate
    }
    private val exifDateParser = mockk<ExifDateParser> { every { parse(any()) } returns null }

    private fun indexer(
        syncStateStore: FakeSyncStateStore = FakeSyncStateStore(),
        rules: List<FolderRule> = emptyList(),
        excludePatterns: List<String> = emptyList(),
    ): MediaIndexer {
        val settingsRepository = mockk<SettingsRepository> {
            every { observeSettings() } returns flowOf(UserSettings(rules, excludePatterns))
        }
        return MediaIndexer(
            scanSource,
            exifSource,
            store,
            syncStateStore,
            settingsRepository,
            exifDateParser,
            dateResolver,
            clock,
        )
    }

    private fun previousState(generation: Long = 5, version: String = "v1") =
        SyncState(VOLUME, lastGeneration = generation, mediaStoreVersion = version, lastFullScanMillis = 100)

    @Test
    fun sync_newImage_hasNoLocationYet() = runTest {
        scanSource.images = listOf(scannedImage(1))

        indexer().sync(IndexMode.FULL)

        assertEquals(null to false, store.items.value.getValue(1).let { it.location to it.exifScanned })
    }

    @Test
    fun sync_unchangedScannedImage_keepsItsLocation() = runTest {
        val image = scannedImage(1)
        val location = GeoPoint(45.5, 10.5)
        store.upsertAll(
            listOf(
                mediaItem(1).copy(
                    sizeBytes = image.sizeBytes,
                    dateModifiedSeconds = image.dateModifiedSeconds,
                    location = location,
                    exifScanned = true,
                ),
            ),
        )
        scanSource.images = listOf(image)

        indexer().sync(IndexMode.FULL)

        assertEquals(location to true, store.items.value.getValue(1).let { it.location to it.exifScanned })
    }

    @Test
    fun sync_changedScannedImage_isScannedAgain() = runTest {
        val image = scannedImage(1)
        store.upsertAll(
            listOf(
                mediaItem(1).copy(
                    sizeBytes = image.sizeBytes + 1,
                    dateModifiedSeconds = image.dateModifiedSeconds,
                    location = GeoPoint(45.5, 10.5),
                    exifScanned = true,
                ),
            ),
        )
        scanSource.images = listOf(image)

        indexer().sync(IndexMode.FULL)

        assertEquals(null to false, store.items.value.getValue(1).let { it.location to it.exifScanned })
    }

    @Test
    fun sync_rewrittenWithSameSize_isScannedAgain() = runTest {
        val image = scannedImage(1)
        store.upsertAll(
            listOf(
                mediaItem(1).copy(
                    sizeBytes = image.sizeBytes,
                    dateModifiedSeconds = image.dateModifiedSeconds + 1,
                    location = GeoPoint(45.5, 10.5),
                    exifScanned = true,
                ),
            ),
        )
        scanSource.images = listOf(image)

        indexer().sync(IndexMode.FULL)

        assertEquals(false, store.items.value.getValue(1).exifScanned)
    }

    @Test
    fun sync_unscannedImage_staysUnscanned() = runTest {
        val image = scannedImage(1)
        store.upsertAll(
            listOf(mediaItem(1).copy(sizeBytes = image.sizeBytes, dateModifiedSeconds = image.dateModifiedSeconds)),
        )
        scanSource.images = listOf(image)

        indexer().sync(IndexMode.FULL)

        assertEquals(false, store.items.value.getValue(1).exifScanned)
    }

    @Test
    fun sync_firstRun_insertsAllImages() = runTest {
        scanSource.images = listOf(scannedImage(1), scannedImage(2))

        indexer().sync(IndexMode.INCREMENTAL)

        assertEquals(setOf(1L, 2L), store.items.value.keys)
    }

    @Test
    fun sync_excludedFolder_isSkipped() = runTest {
        scanSource.images = listOf(scannedImage(1, "DCIM/Camera/"), scannedImage(2, "Pictures/Screenshots/"))

        indexer(rules = listOf(FolderRule("Pictures/Screenshots", FolderRule.Type.EXCLUDE))).sync(IndexMode.FULL)

        assertEquals(setOf(1L), store.items.value.keys)
    }

    @Test
    fun sync_folderMatchingExcludePattern_isSkipped() = runTest {
        scanSource.images = listOf(scannedImage(1, "DCIM/Camera/"), scannedImage(2, "Pictures/Thumbs/"))

        indexer(excludePatterns = listOf("*/Thumbs")).sync(IndexMode.FULL)

        assertEquals(setOf(1L), store.items.value.keys)
    }

    @Test
    fun sync_fullScan_removesRowsNoLongerInMediaStore() = runTest {
        scanSource.images = listOf(scannedImage(1), scannedImage(2))
        indexer().sync(IndexMode.FULL)
        scanSource.images = listOf(scannedImage(1))

        val stats = indexer().sync(IndexMode.FULL)

        assertEquals(1, stats.deleted)
    }

    @Test
    fun sync_incremental_scansOnlyChangesSinceLastGeneration() = runTest {
        scanSource.images = listOf(scannedImage(1, generationModified = 3), scannedImage(2, generationModified = 8))

        indexer(FakeSyncStateStore(previousState(generation = 5))).sync(IndexMode.INCREMENTAL)

        assertEquals(setOf(2L), store.items.value.keys)
    }

    @Test
    fun sync_incremental_removesDeletedImages() = runTest {
        scanSource.images = listOf(scannedImage(1), scannedImage(2))
        indexer().sync(IndexMode.FULL)
        scanSource.images = listOf(scannedImage(1))

        indexer(FakeSyncStateStore(previousState())).sync(IndexMode.INCREMENTAL)

        assertEquals(setOf(1L), store.items.value.keys)
    }

    @Test
    fun sync_incremental_removesImageMovedIntoExcludedFolder() = runTest {
        scanSource.images = listOf(scannedImage(1, "DCIM/Camera/"))
        indexer().sync(IndexMode.FULL)
        scanSource.images = listOf(scannedImage(1, "Hidden/", generationModified = 9))

        indexer(FakeSyncStateStore(previousState()), listOf(FolderRule("Hidden", FolderRule.Type.EXCLUDE)))
            .sync(IndexMode.INCREMENTAL)

        assertTrue(store.items.value.isEmpty())
    }

    @Test
    fun sync_mediaStoreVersionChanged_runsFullScan() = runTest {
        scanSource.version = "v2"

        indexer(FakeSyncStateStore(previousState(version = "v1"))).sync(IndexMode.INCREMENTAL)

        assertEquals(1, scanSource.fullScans)
    }

    @Test
    fun sync_success_storesGenerationAndVersion() = runTest {
        val syncStateStore = FakeSyncStateStore()
        scanSource.generation = 42

        indexer(syncStateStore).sync(IndexMode.FULL)

        assertEquals(SyncState(VOLUME, 42, "v1", 5_000L), syncStateStore.states[VOLUME])
    }

    @Test
    fun sync_dateTakenMissing_readsExif() = runTest {
        scanSource.images = listOf(scannedImage(1, dateTakenMillis = null))

        indexer().sync(IndexMode.FULL)

        assertEquals(listOf(scannedImage(1).contentUri), exifSource.readUris)
    }

    @Test
    fun sync_dateTakenPresent_doesNotReadExif() = runTest {
        scanSource.images = listOf(scannedImage(1))

        indexer().sync(IndexMode.FULL)

        assertTrue(exifSource.readUris.isEmpty())
    }

    @Test
    fun sync_storesResolvedDate() = runTest {
        scanSource.images = listOf(scannedImage(1))

        indexer().sync(IndexMode.FULL)

        assertEquals(resolvedDate.localDate, store.items.value.getValue(1L).takenLocalDate)
    }
}
