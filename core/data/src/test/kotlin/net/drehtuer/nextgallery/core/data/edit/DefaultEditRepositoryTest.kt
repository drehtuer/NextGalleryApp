package net.drehtuer.nextgallery.core.data.edit

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.common.DispatcherProvider
import net.drehtuer.nextgallery.core.data.fakes.FakeClock
import net.drehtuer.nextgallery.core.data.fakes.FakeMediaIndexStore
import net.drehtuer.nextgallery.core.data.fakes.FakeWorkScheduler
import net.drehtuer.nextgallery.core.data.fakes.mediaItem
import net.drehtuer.nextgallery.core.data.index.IndexMode
import net.drehtuer.nextgallery.core.data.index.IndexRepository
import net.drehtuer.nextgallery.core.data.index.IndexStats
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.edit.EditOperation
import net.drehtuer.nextgallery.core.model.edit.ImageSize
import net.drehtuer.nextgallery.core.model.edit.NormalizedRect
import net.drehtuer.nextgallery.core.model.edit.PreviewImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import kotlin.coroutines.ContinuationInterceptor

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultEditRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val id = MediaId(1)
    private val item = mediaItem(1)
    private val uri = item.contentUri
    private val rotate = listOf<EditOperation>(EditOperation.Rotate(1))
    private val store = FakeMediaIndexStore()
    private val exif = FakeExifWriter()
    private val scheduler = FakeWorkScheduler()
    private val clock = FakeClock(now = 100_000_000)
    private var availableBytes = Long.MAX_VALUE
    private val indexRepository = mockk<IndexRepository> {
        coEvery { sync(any()) } returns AppResult.Success(IndexStats.EMPTY)
    }
    private lateinit var renderer: FakeImageRenderer
    private lateinit var sink: FakeMediaWriteSink

    @Before
    fun setUp() = runTest {
        renderer = FakeImageRenderer(folder.root)
        sink = FakeMediaWriteSink(folder.root)
        store.upsertAll(listOf(item))
    }

    private fun TestScope.repository(
        dispatchers: DispatcherProvider = object : DispatcherProvider {
            override val io = UnconfinedTestDispatcher(testScheduler)
            override val default = io
        },
    ) = DefaultEditRepository(
        store,
        renderer,
        EditedImageWriter(renderer, sink, exif, { availableBytes }, dispatchers),
        { availableBytes },
        indexRepository,
        scheduler,
        dispatchers,
        clock,
    )

    private fun TestScope.source() = EditSource(item, ImageSize(4000, 3000), PreviewImage(Any(), 2000, 1500))

    private fun temporaryFiles() = folder.root.listFiles().orEmpty().toList()

    // open

    @Test
    fun open_unknownImage_returnsNotFound() = runTest {
        assertEquals(AppResult.Failure(AppError.NotFound), repository().open(MediaId(5)))
    }

    @Test
    fun open_formatThatCannotBeEdited_fails() = runTest {
        store.upsertAll(listOf(mediaItem(2, mimeType = "image/heic")))

        assertTrue(repository().open(MediaId(2)) is AppResult.Failure)
    }

    @Test
    fun open_returnsUprightSizeOfFile() = runTest {
        val result = repository().open(id) as AppResult.Success

        assertEquals(ImageSize(4000, 3000), result.value.size)
    }

    @Test
    fun open_decodesDownsampledBase() = runTest {
        repository().open(id)

        assertEquals(listOf(2), renderer.decodedSamples)
    }

    @Test
    fun open_unreadableFile_returnsIoError() = runTest {
        renderer.failure = IOException("corrupt")

        assertEquals(AppResult.Failure(AppError.Io("corrupt")), repository().open(id))
    }

    // renderPreview

    @Test
    fun renderPreview_keepsPreviewWithinMaxSide() = runTest {
        repository().renderPreview(source(), rotate)

        assertEquals(ImageSize(1536, 2048), renderer.plans.single().outputSize)
    }

    @Test
    fun renderPreview_keepsFullSizeAsSourceOfThePlan() = runTest {
        repository().renderPreview(source(), rotate)

        assertEquals(ImageSize(4000, 3000), renderer.plans.single().sourceSize)
    }

    @Test
    fun renderPreview_runsOnDefaultDispatcher() = runTest {
        val default: CoroutineDispatcher = UnconfinedTestDispatcher(testScheduler)
        val dispatchers = object : DispatcherProvider {
            override val io = StandardTestDispatcher(testScheduler)
            override val default = default
        }

        repository(dispatchers).renderPreview(source(), rotate)

        assertSame(default, renderer.renderDispatcher)
    }

    @Test
    fun renderPreview_failure_returnsError() = runTest {
        renderer.failRenderWith = IOException("broken")

        assertEquals(AppResult.Failure(AppError.Io("broken")), repository().renderPreview(source(), rotate))
    }

    @Test
    fun renderPreview_outOfMemory_returnsInsufficientMemory() = runTest {
        renderer.failRenderWith = RenderOutOfMemoryException(OutOfMemoryError())
        availableBytes = 5

        val error = (repository().renderPreview(source(), rotate) as AppResult.Failure).error

        assertEquals(5, (error as AppError.InsufficientMemory).availableBytes)
    }

    // checkMemory

    @Test
    fun checkMemory_enough_fits() = runTest {
        assertEquals(MemoryCheck.Fits, repository().checkMemory(source(), rotate))
    }

    @Test
    fun checkMemory_tooLittle_isRefusedForTheOutputOfTheOperations() = runTest {
        availableBytes = 300_000_000

        val check = repository().checkMemory(source(), listOf(EditOperation.Scale(12000, 9000)))

        assertEquals(MemoryCheck.Refused(480_000_000, 300_000_000), check)
    }

    // save: new copy

    @Test
    fun saveNewCopy_unknownImage_returnsNotFound() = runTest {
        val outcome = repository().save(MediaId(5), rotate, SaveMode.NEW_COPY)

        assertEquals(SaveOutcome.Failed(AppError.NotFound), outcome)
    }

    @Test
    fun saveNewCopy_formatThatCannotBeEdited_failsWithoutWriting() = runTest {
        store.upsertAll(listOf(mediaItem(2, mimeType = "image/heic")))

        repository().save(MediaId(2), rotate, SaveMode.NEW_COPY)

        assertTrue(sink.calls.isEmpty())
    }

    @Test
    fun saveNewCopy_writesThenPublishes() = runTest {
        repository().save(id, rotate, SaveMode.NEW_COPY)

        assertEquals(listOf("insert", "write", "publish"), sink.calls)
    }

    @Test
    fun saveNewCopy_entryIsNamedForTheCopy() = runTest {
        sink.existing = setOf("IMG_1_edited.jpg")

        repository().save(id, rotate, SaveMode.NEW_COPY)

        assertEquals("IMG_1_edited_2.jpg", sink.insertedName)
    }

    @Test
    fun saveNewCopy_entryIsInTheSameFolderAndFormat() = runTest {
        repository().save(id, rotate, SaveMode.NEW_COPY)

        assertEquals(
            listOf(item.volumeName, item.relativePath, item.mimeType),
            listOf(sink.insertedVolume, sink.insertedPath, sink.insertedMime),
        )
    }

    @Test
    fun saveNewCopy_writesTheRenderedImage() = runTest {
        repository().save(id, rotate, SaveMode.NEW_COPY)

        assertEquals(FakeImageRenderer.EDITED, sink.contents[FakeMediaWriteSink.NEW_URI])
    }

    @Test
    fun saveNewCopy_doesNotTouchTheOriginal() = runTest {
        repository().save(id, rotate, SaveMode.NEW_COPY)

        assertFalse(uri in sink.contents)
    }

    @Test
    fun saveNewCopy_rendersAtFullResolution() = runTest {
        repository().save(id, rotate, SaveMode.NEW_COPY)

        assertEquals(ImageSize(3000, 4000), renderer.plans.single().outputSize)
    }

    @Test
    fun saveNewCopy_decodesAtFullResolution() = runTest {
        repository().save(id, rotate, SaveMode.NEW_COPY)

        assertEquals(listOf(1), renderer.decodedSamples)
    }

    @Test
    fun saveNewCopy_scaledDown_decodesOnlyWhatTheOutputNeeds() = runTest {
        repository().save(id, listOf(EditOperation.Scale(1000, 750)), SaveMode.NEW_COPY)

        assertEquals(listOf(4), renderer.decodedSamples)
    }

    @Test
    fun saveNewCopy_keepsTheFormatOfTheSource() = runTest {
        store.upsertAll(listOf(mediaItem(2, mimeType = "image/png")))

        repository().save(MediaId(2), rotate, SaveMode.NEW_COPY)

        assertEquals(listOf(ImageFormat.PNG), renderer.encodedFormats)
    }

    @Test
    fun saveNewCopy_asksForThePolicyTags() = runTest {
        repository().save(id, rotate, SaveMode.NEW_COPY)

        assertEquals(ExifPolicy.COPIED_TAGS, exif.requestedTags)
    }

    @Test
    fun saveNewCopy_writesExifWithNormalOrientationAndNewSize() = runTest {
        repository().save(id, rotate, SaveMode.NEW_COPY)

        assertEquals(ExifPolicy.forOutput(exif.sourceTags, 3000, 4000), exif.written)
    }

    @Test
    fun saveNewCopy_freesBothBitmaps() = runTest {
        repository().save(id, rotate, SaveMode.NEW_COPY)

        assertEquals(renderer.live, renderer.released)
    }

    @Test
    fun saveNewCopy_deletesTemporaryFile() = runTest {
        repository().save(id, rotate, SaveMode.NEW_COPY)

        assertTrue(temporaryFiles().isEmpty())
    }

    @Test
    fun saveNewCopy_inIndexAfterSync_reportsIndexed() = runTest {
        coEvery { indexRepository.sync(IndexMode.INCREMENTAL) } coAnswers {
            store.upsertAll(listOf(mediaItem(99)))
            AppResult.Success(IndexStats.EMPTY)
        }

        val outcome = repository().save(id, rotate, SaveMode.NEW_COPY)

        assertEquals(SaveOutcome.SavedCopy(FakeMediaWriteSink.NEW_ID, item.bucketId, indexed = true), outcome)
    }

    @Test
    fun saveNewCopy_notYetInIndex_reportsNotIndexed() = runTest {
        val outcome = repository().save(id, rotate, SaveMode.NEW_COPY)

        assertEquals(SaveOutcome.SavedCopy(FakeMediaWriteSink.NEW_ID, item.bucketId, indexed = false), outcome)
    }

    @Test
    fun saveNewCopy_triggersIncrementalIndex() = runTest {
        repository().save(id, rotate, SaveMode.NEW_COPY)

        coVerify { indexRepository.sync(IndexMode.INCREMENTAL) }
        assertEquals(listOf(IndexMode.INCREMENTAL), scheduler.enqueued)
    }

    @Test
    fun saveNewCopy_notEnoughMemory_isRefusedWithBothNumbers() = runTest {
        availableBytes = 1000

        val outcome = repository().save(id, rotate, SaveMode.NEW_COPY)

        assertEquals(SaveOutcome.Failed(AppError.InsufficientMemory(96_000_000, 1000)), outcome)
    }

    @Test
    fun saveNewCopy_notEnoughMemory_decodesAndWritesNothing() = runTest {
        availableBytes = 1000

        repository().save(id, rotate, SaveMode.NEW_COPY)

        assertTrue(renderer.decodedSamples.isEmpty() && sink.calls.isEmpty())
    }

    @Test
    fun saveNewCopy_outOfMemoryDespiteCheck_reportsInsufficientMemory() = runTest {
        renderer.failRenderWith = RenderOutOfMemoryException(OutOfMemoryError())
        availableBytes = 100_000_000

        val outcome = repository().save(id, rotate, SaveMode.NEW_COPY)

        assertEquals(SaveOutcome.Failed(AppError.InsufficientMemory(96_000_000, 100_000_000)), outcome)
    }

    @Test
    fun saveNewCopy_renderFails_freesDecodedBitmapAndWritesNothing() = runTest {
        renderer.failRenderWith = IOException("broken")

        repository().save(id, rotate, SaveMode.NEW_COPY)

        assertEquals(renderer.live, renderer.released)
        assertTrue(sink.calls.isEmpty())
    }

    @Test
    fun saveNewCopy_exifFails_writesNothingAndDeletesTemporaryFile() = runTest {
        exif.failWrite = true

        repository().save(id, rotate, SaveMode.NEW_COPY)

        assertTrue(sink.calls.isEmpty() && temporaryFiles().isEmpty())
    }

    @Test
    fun saveNewCopy_writeFails_removesPendingEntry() = runTest {
        sink.failNextWrite = true

        repository().save(id, rotate, SaveMode.NEW_COPY)

        assertEquals(listOf("insert", "write", "discard"), sink.calls)
        assertTrue(sink.pending.isEmpty())
    }

    @Test
    fun saveNewCopy_writeFails_reportsTheError() = runTest {
        sink.failNextWrite = true

        val outcome = repository().save(id, rotate, SaveMode.NEW_COPY)

        assertEquals(SaveOutcome.Failed(AppError.Io("disk full")), outcome)
    }

    @Test
    fun saveNewCopy_writeFails_doesNotSyncTheIndex() = runTest {
        sink.failNextWrite = true

        repository().save(id, rotate, SaveMode.NEW_COPY)

        assertTrue(scheduler.enqueued.isEmpty())
    }

    @Test
    fun saveNewCopy_publishFails_removesPendingEntry() = runTest {
        sink.failPublish = true

        repository().save(id, rotate, SaveMode.NEW_COPY)

        assertTrue(sink.pending.isEmpty())
    }

    @Test
    fun saveNewCopy_discardAlsoFails_stillReportsTheFirstError() = runTest {
        sink.failNextWrite = true
        sink.failDiscard = true

        val outcome = repository().save(id, rotate, SaveMode.NEW_COPY)

        assertEquals(SaveOutcome.Failed(AppError.Io("disk full")), outcome)
    }

    @Test
    fun saveNewCopy_writeFails_deletesTemporaryFile() = runTest {
        sink.failNextWrite = true

        repository().save(id, rotate, SaveMode.NEW_COPY)

        assertTrue(temporaryFiles().isEmpty())
    }

    @Test
    fun saveNewCopy_cancelledWhileEncoding_writesNothingAndDeletesNothingVisible() = runTest {
        val repository = repository()
        val saving = backgroundScope.async {
            renderer.onEncode = { cancel() }
            repository.save(id, rotate, SaveMode.NEW_COPY)
        }
        runCurrent()

        assertTrue(saving.isCancelled)
        assertTrue(sink.calls.isEmpty())
    }

    @Test
    fun saveNewCopy_cancelledDuringCommit_stillPublishes() = runTest {
        val repository = repository()
        val saving = backgroundScope.async {
            val job = currentCoroutineContext()[Job]!!
            sink.onWrite = { job.cancel() }
            repository.save(id, rotate, SaveMode.NEW_COPY)
        }
        runCurrent()

        assertTrue(saving.isCancelled)
        assertEquals(listOf("insert", "write", "publish"), sink.calls)
        assertTrue(temporaryFiles().isEmpty())
    }

    // save: overwrite

    @Test
    fun saveOverwrite_asksTheSystemFirst() = runTest {
        val outcome = repository().save(id, rotate, SaveMode.OVERWRITE)

        assertEquals(SaveOutcome.NeedsConfirmation(sink.confirmation), outcome)
    }

    @Test
    fun saveOverwrite_writesNothingBeforeTheConfirmation() = runTest {
        repository().save(id, rotate, SaveMode.OVERWRITE)

        assertEquals(listOf("request"), sink.calls)
    }

    @Test
    fun saveOverwrite_notEnoughMemory_isRefusedWithoutAsking() = runTest {
        availableBytes = 1000

        val outcome = repository().save(id, rotate, SaveMode.OVERWRITE)

        assertEquals(SaveOutcome.Failed(AppError.InsufficientMemory(96_000_000, 1000)), outcome)
        assertTrue(sink.calls.isEmpty())
    }

    @Test
    fun saveOverwrite_unreadableFile_reportsTheError() = runTest {
        renderer.failure = IOException("corrupt")

        assertEquals(SaveOutcome.Failed(AppError.Io("corrupt")), repository().save(id, rotate, SaveMode.OVERWRITE))
    }

    @Test
    fun commitOverwrite_unknownImage_returnsNotFound() = runTest {
        assertEquals(SaveOutcome.Failed(AppError.NotFound), repository().commitOverwrite(MediaId(5), rotate))
    }

    @Test
    fun commitOverwrite_backsUpThenWrites() = runTest {
        sink.contents[uri] = ORIGINAL

        val outcome = repository().commitOverwrite(id, rotate)

        assertEquals(SaveOutcome.Overwritten, outcome)
        assertEquals(listOf("backup", "write"), sink.calls)
    }

    @Test
    fun commitOverwrite_replacesTheContent() = runTest {
        sink.contents[uri] = ORIGINAL

        repository().commitOverwrite(id, rotate)

        assertEquals(FakeImageRenderer.EDITED, sink.contents[uri])
    }

    @Test
    fun commitOverwrite_deletesTemporaryFiles() = runTest {
        sink.contents[uri] = ORIGINAL

        repository().commitOverwrite(id, rotate)

        assertTrue(temporaryFiles().isEmpty())
    }

    @Test
    fun commitOverwrite_triggersIncrementalIndex() = runTest {
        sink.contents[uri] = ORIGINAL

        repository().commitOverwrite(id, rotate)

        assertEquals(listOf(IndexMode.INCREMENTAL), scheduler.enqueued)
    }

    @Test
    fun commitOverwrite_notEnoughMemoryAfterConfirmation_leavesOriginal() = runTest {
        sink.contents[uri] = ORIGINAL
        availableBytes = 1000

        val outcome = repository().commitOverwrite(id, rotate)

        assertEquals(SaveOutcome.Failed(AppError.InsufficientMemory(96_000_000, 1000)), outcome)
        assertEquals(ORIGINAL, sink.contents[uri])
    }

    @Test
    fun commitOverwrite_renderFails_leavesOriginal() = runTest {
        sink.contents[uri] = ORIGINAL
        renderer.failRenderWith = IOException("broken")

        repository().commitOverwrite(id, rotate)

        assertEquals(ORIGINAL, sink.contents[uri])
        assertTrue(sink.calls.isEmpty())
    }

    @Test
    fun commitOverwrite_backupFails_leavesOriginal() = runTest {
        sink.contents[uri] = ORIGINAL
        sink.failBackup = true

        val outcome = repository().commitOverwrite(id, rotate)

        assertEquals(SaveOutcome.Failed(AppError.Io("no space for backup")), outcome)
        assertEquals(ORIGINAL, sink.contents[uri])
    }

    @Test
    fun commitOverwrite_restoreFailsToo_savesBackupAsNewImage() = runTest {
        val id = indexed()
        sink.failWrites = 2

        val outcome = repository().commitOverwrite(id, rotate)

        assertEquals(SaveOutcome.Failed(AppError.OriginalDamaged(recoveredAsCopy = true)), outcome)
    }

    @Test
    fun commitOverwrite_restoreAndRecoveryFail_reportsNoCopy() = runTest {
        val id = indexed()
        sink.failWrites = 3

        val outcome = repository().commitOverwrite(id, rotate)

        assertEquals(SaveOutcome.Failed(AppError.OriginalDamaged(recoveredAsCopy = false)), outcome)
    }

    @Test
    fun commitOverwrite_writeFails_restoresOriginal() = runTest {
        sink.contents[uri] = ORIGINAL
        sink.failNextWrite = true

        repository().commitOverwrite(id, rotate)

        assertEquals(ORIGINAL, sink.contents[uri])
    }

    @Test
    fun commitOverwrite_writeFails_reportsTheError() = runTest {
        sink.contents[uri] = ORIGINAL
        sink.failNextWrite = true

        val outcome = repository().commitOverwrite(id, rotate)

        assertEquals(SaveOutcome.Failed(AppError.Io("disk full")), outcome)
    }

    @Test
    fun commitOverwrite_writeFails_deletesTemporaryFiles() = runTest {
        sink.contents[uri] = ORIGINAL
        sink.failNextWrite = true

        repository().commitOverwrite(id, rotate)

        assertTrue(temporaryFiles().isEmpty())
    }

    @Test
    fun commitOverwrite_cancelledDuringCommit_stillFinishes() = runTest {
        sink.contents[uri] = ORIGINAL
        val repository = repository()
        val saving = backgroundScope.async {
            val job = currentCoroutineContext()[Job]!!
            sink.onWrite = { job.cancel() }
            repository.commitOverwrite(id, rotate)
        }
        runCurrent()

        assertTrue(saving.isCancelled)
        assertEquals(FakeImageRenderer.EDITED, sink.contents[uri])
    }

    // cleanupPending

    @Test
    fun cleanupPending_removesEntriesOlderThanOneDay() = runTest {
        repository().cleanupPending()

        assertEquals(100_000_000L - 86_400_000L, sink.cleanupBefore)
    }

    @Test
    fun cleanupPending_failure_isIgnored() = runTest {
        sink.failCleanup = true

        repository().cleanupPending()

        assertNull(sink.cleanupBefore)
    }

    private companion object {
        const val ORIGINAL = "original pixels"
    }
}
