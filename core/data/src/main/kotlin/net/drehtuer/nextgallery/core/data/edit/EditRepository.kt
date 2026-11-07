package net.drehtuer.nextgallery.core.data.edit

import kotlinx.coroutines.withContext
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.common.Clock
import net.drehtuer.nextgallery.core.common.DispatcherProvider
import net.drehtuer.nextgallery.core.common.runCatchingCancellable
import net.drehtuer.nextgallery.core.data.index.IndexMode
import net.drehtuer.nextgallery.core.data.index.IndexRepository
import net.drehtuer.nextgallery.core.data.index.MediaIndexStore
import net.drehtuer.nextgallery.core.data.index.WorkScheduler
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem
import net.drehtuer.nextgallery.core.model.PlatformConfirmation
import net.drehtuer.nextgallery.core.model.edit.EditOperation
import net.drehtuer.nextgallery.core.model.edit.ImageSize
import net.drehtuer.nextgallery.core.model.edit.PreviewImage
import java.io.IOException
import javax.inject.Inject

enum class SaveMode { NEW_COPY, OVERWRITE }

sealed interface SaveOutcome {
    /** [indexed] tells whether the viewer can show the copy right away. */
    data class SavedCopy(val id: MediaId, val bucketId: Long, val indexed: Boolean) : SaveOutcome

    data object Overwritten : SaveOutcome

    data class NeedsConfirmation(val confirmation: PlatformConfirmation) : SaveOutcome

    /** The gallery is as it was before. */
    data class Failed(val error: AppError) : SaveOutcome
}

/** An image opened for editing: [size] is the full upright size, [base] the downsampled bitmap for the preview. */
class EditSource(val item: MediaItem, val size: ImageSize, val base: PreviewImage)

interface EditRepository {
    suspend fun open(id: MediaId): AppResult<EditSource>

    suspend fun renderPreview(source: EditSource, operations: List<EditOperation>): AppResult<PreviewImage>

    /** Whether the full-resolution render of these operations fits into the memory (D30). */
    fun checkMemory(source: EditSource, operations: List<EditOperation>): MemoryCheck

    /** A new copy is saved right away; overwriting first asks the system, then goes on with [commitOverwrite]. */
    suspend fun save(id: MediaId, operations: List<EditOperation>, mode: SaveMode): SaveOutcome

    suspend fun commitOverwrite(id: MediaId, operations: List<EditOperation>): SaveOutcome

    /** Removes what a crash during saving left behind. */
    suspend fun cleanupPending()

    companion object {
        const val PREVIEW_MAX_SIDE = 2048
    }
}

class DefaultEditRepository @Inject constructor(
    private val indexStore: MediaIndexStore,
    private val renderer: ImageRenderer,
    private val writer: EditedImageWriter,
    private val memoryInfo: MemoryInfoSource,
    private val indexRepository: IndexRepository,
    private val workScheduler: WorkScheduler,
    private val dispatchers: DispatcherProvider,
    private val clock: Clock,
) : EditRepository {
    private val previewBytes = ImageSize(EditRepository.PREVIEW_MAX_SIDE, EditRepository.PREVIEW_MAX_SIDE).let {
        MemoryBudget.requiredBytes(it, it)
    }

    override suspend fun open(id: MediaId): AppResult<EditSource> {
        val item = indexStore.getById(id) ?: return AppResult.Failure(AppError.NotFound)
        return runCatchingCancellable {
            ImageFormat.fromMimeType(item.mimeType) ?: throw IOException("Cannot edit ${item.mimeType}")
            val size = renderer.readSize(item.contentUri)
            val sample = EditGeometry.sampleSizeFor(size, EditRepository.PREVIEW_MAX_SIDE)
            EditSource(item, size, renderer.decode(item.contentUri, sample))
        }.refusingOutOfMemory(memoryInfo) { previewBytes }
    }

    override suspend fun renderPreview(source: EditSource, operations: List<EditOperation>): AppResult<PreviewImage> {
        val plan = EditGeometry.steps(operations, source.size).fittedTo(EditRepository.PREVIEW_MAX_SIDE)
        return runCatchingCancellable {
            withContext(dispatchers.default) { renderer.render(source.base, plan) }
        }.refusingOutOfMemory(memoryInfo) { previewBytes }
    }

    override fun checkMemory(source: EditSource, operations: List<EditOperation>): MemoryCheck =
        MemoryBudget.check(source.size, EditGeometry.outputSize(operations, source.size), memoryInfo.availableBytes())

    override suspend fun save(id: MediaId, operations: List<EditOperation>, mode: SaveMode): SaveOutcome {
        val item = indexStore.getById(id) ?: return SaveOutcome.Failed(AppError.NotFound)
        return when (mode) {
            SaveMode.NEW_COPY -> saveNewCopy(item, operations)
            SaveMode.OVERWRITE -> writer.requestOverwrite(item, operations)
        }
    }

    override suspend fun commitOverwrite(id: MediaId, operations: List<EditOperation>): SaveOutcome {
        val item = indexStore.getById(id) ?: return SaveOutcome.Failed(AppError.NotFound)
        return when (val result = writer.overwrite(item, operations)) {
            is AppResult.Success -> {
                refreshIndex()
                SaveOutcome.Overwritten
            }

            is AppResult.Failure -> SaveOutcome.Failed(result.error)
        }
    }

    override suspend fun cleanupPending() {
        runCatchingCancellable { writer.cleanupPending(clock.nowMillis() - PENDING_MAX_AGE_MILLIS) }
    }

    private suspend fun saveNewCopy(item: MediaItem, operations: List<EditOperation>): SaveOutcome =
        when (val result = writer.saveCopy(item, operations)) {
            is AppResult.Success -> {
                refreshIndex()
                SaveOutcome.SavedCopy(result.value, item.bucketId, indexStore.getById(result.value) != null)
            }

            is AppResult.Failure -> SaveOutcome.Failed(result.error)
        }

    /** Brings the new image into the index at once; the worker later picks up what the media scanner adds to it. */
    private suspend fun refreshIndex() {
        indexRepository.sync(IndexMode.INCREMENTAL)
        workScheduler.enqueueIndex(IndexMode.INCREMENTAL)
    }

    private companion object {
        const val PENDING_MAX_AGE_MILLIS = 24L * 60 * 60 * 1000
    }
}
