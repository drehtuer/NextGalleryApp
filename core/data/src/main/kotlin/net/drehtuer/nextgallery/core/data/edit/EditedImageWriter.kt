package net.drehtuer.nextgallery.core.data.edit

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import net.drehtuer.nextgallery.core.common.AppError
import net.drehtuer.nextgallery.core.common.AppResult
import net.drehtuer.nextgallery.core.common.DispatcherProvider
import net.drehtuer.nextgallery.core.common.OriginalDamagedException
import net.drehtuer.nextgallery.core.common.runCatchingCancellable
import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.MediaItem
import net.drehtuer.nextgallery.core.model.edit.EditOperation
import java.io.File
import java.io.IOException
import javax.inject.Inject

/**
 * Renders an edit at full resolution and writes it to the gallery, without ever endangering an image: everything
 * before the commit only touches temporary files, and the commit itself always runs to its end
 * (ARCHITECTURE.md §5.4 and §8).
 */
class EditedImageWriter @Inject constructor(
    private val renderer: ImageRenderer,
    private val sink: MediaWriteSink,
    private val exifWriter: ExifWriter,
    private val memoryInfo: MemoryInfoSource,
    private val dispatchers: DispatcherProvider,
) {
    suspend fun saveCopy(item: MediaItem, operations: List<EditOperation>): AppResult<MediaId> =
        encodeAndCommit(item, operations) { insertCopy(item, it) }

    suspend fun overwrite(item: MediaItem, operations: List<EditOperation>): AppResult<Unit> =
        encodeAndCommit(item, operations) { replaceContent(item, it) }

    /** Refuses before the system asks the user, if the save could not run anyway. */
    suspend fun requestOverwrite(item: MediaItem, operations: List<EditOperation>): SaveOutcome {
        val result = runCatchingCancellable {
            val plan = EditGeometry.steps(operations, renderer.readSize(item.contentUri))
            val refusal = memoryRefusal(plan)
            if (refusal != null) {
                SaveOutcome.Failed(refusal)
            } else {
                SaveOutcome.NeedsConfirmation(sink.requestOverwrite(item.contentUri))
            }
        }
        return when (result) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> SaveOutcome.Failed(result.error)
        }
    }

    suspend fun cleanupPending(olderThanMillis: Long) = sink.cleanupPending(olderThanMillis)

    private fun memoryRefusal(plan: RenderPlan): AppError.InsufficientMemory? =
        when (val check = MemoryBudget.check(plan.sourceSize, plan.outputSize, memoryInfo.availableBytes())) {
            MemoryCheck.Fits -> null
            is MemoryCheck.Refused -> AppError.InsufficientMemory(check.requiredBytes, check.availableBytes)
        }

    private suspend fun <T> encodeAndCommit(
        item: MediaItem,
        operations: List<EditOperation>,
        commit: suspend (EncodedImage) -> T,
    ): AppResult<T> {
        var requiredBytes = 0L
        return runCatchingCancellable {
            val format = ImageFormat.fromMimeType(item.mimeType) ?: throw IOException("Cannot edit ${item.mimeType}")
            val plan = EditGeometry.steps(operations, renderer.readSize(item.contentUri))
            requiredBytes = MemoryBudget.requiredBytes(plan.sourceSize, plan.outputSize)
            memoryRefusal(plan)?.let { return AppResult.Failure(it) }
            val encoded = encode(item, plan, format)
            try {
                val tags = exifWriter.readTags(item.contentUri, ExifPolicy.COPIED_TAGS)
                exifWriter.writeTags(
                    encoded,
                    ExifPolicy.forOutput(tags, plan.outputSize.width, plan.outputSize.height),
                )
                currentCoroutineContext().ensureActive()
                withContext(NonCancellable) { commit(encoded) }
            } finally {
                encoded.file.delete()
            }
        }.refusingOutOfMemory(memoryInfo) { requiredBytes }
    }

    private suspend fun encode(item: MediaItem, plan: RenderPlan, format: ImageFormat): EncodedImage {
        val rendered = withContext(dispatchers.default) {
            val decoded = renderer.decode(item.contentUri, EditGeometry.decodeSampleSize(plan))
            try {
                renderer.render(decoded, plan)
            } finally {
                renderer.release(decoded)
            }
        }
        return try {
            renderer.encode(rendered, format)
        } finally {
            renderer.release(rendered)
        }
    }

    /** The entry stays hidden until it is complete; if anything fails, it is removed again. */
    private suspend fun insertCopy(item: MediaItem, encoded: EncodedImage): MediaId {
        val name = CopyNamer.nameFor(item.displayName, sink.existingNames(item.volumeName, item.relativePath))
        val uri = sink.insertPending(item.volumeName, item.relativePath, name, item.mimeType)
        var published = false
        try {
            return sink.run {
                write(uri, encoded)
                publish(uri)
            }.also { published = true }
        } finally {
            if (!published) runCatchingCancellable { sink.discard(uri) }
        }
    }

    /**
     * If the backup fails, the original was not touched; if writing fails, the backup goes back in its place. If that
     * fails too, the backup is the only intact version: it is saved as a new image, and only deleted once that worked.
     */
    private suspend fun replaceContent(item: MediaItem, encoded: EncodedImage) {
        val backup = sink.backup(item.contentUri)
        val failure = (runCatchingCancellable { sink.write(item.contentUri, encoded) } as? AppResult.Failure)
            ?: return backup.file.deleteAndForget()
        if (runCatchingCancellable { sink.write(item.contentUri, backup) } is AppResult.Success) {
            backup.file.delete()
            throw failure.toException()
        }
        val recovered = runCatchingCancellable { insertCopy(item, backup) } is AppResult.Success
        if (recovered) backup.file.delete()
        throw OriginalDamagedException(recovered, failure.toException())
    }

    private fun File.deleteAndForget() {
        delete()
    }

    private fun AppResult.Failure.toException(): Exception = when (val cause = error) {
        is AppError.Unknown -> cause.cause as? Exception ?: IOException(cause.cause)
        is AppError.Io -> IOException(cause.message)
        is AppError.PermissionDenied -> SecurityException()
        else -> IOException(cause.toString())
    }
}

/** Turns a [RenderOutOfMemoryException] into the same refusal as the memory check before it (D30). */
internal inline fun <T> AppResult<T>.refusingOutOfMemory(
    memoryInfo: MemoryInfoSource,
    requiredBytes: () -> Long,
): AppResult<T> {
    val cause = ((this as? AppResult.Failure)?.error as? AppError.Unknown)?.cause
    return if (cause is RenderOutOfMemoryException) {
        AppResult.Failure(AppError.InsufficientMemory(requiredBytes(), memoryInfo.availableBytes()))
    } else {
        this
    }
}
