package net.drehtuer.nextgallery.core.data.edit

import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.PlatformConfirmation
import net.drehtuer.nextgallery.core.model.edit.ImageSize
import net.drehtuer.nextgallery.core.model.edit.PreviewImage
import java.io.File
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.coroutineContext

/** Records what it was asked to do. Images are plain objects; encoded images are real temporary files. */
class FakeImageRenderer(private val directory: File, var size: ImageSize = ImageSize(4000, 3000)) : ImageRenderer {
    val decodedSamples = mutableListOf<Int>()
    val plans = mutableListOf<RenderPlan>()
    val encodedFormats = mutableListOf<ImageFormat>()
    val released = mutableListOf<PreviewImage>()
    val live = mutableListOf<PreviewImage>()
    var renderDispatcher: Any? = null
    var failure: Exception? = null
    var failRenderWith: Exception? = null
    var onEncode: () -> Unit = {}

    override suspend fun readSize(contentUri: String): ImageSize {
        failure?.let { throw it }
        return size
    }

    override suspend fun decode(contentUri: String, sampleSize: Int): PreviewImage {
        decodedSamples += sampleSize
        return image(size.width / sampleSize, size.height / sampleSize)
    }

    override suspend fun render(base: PreviewImage, plan: RenderPlan): PreviewImage {
        renderDispatcher = coroutineContext[ContinuationInterceptor]
        failRenderWith?.let { throw it }
        plans += plan
        return image(plan.outputSize.width, plan.outputSize.height)
    }

    override suspend fun encode(image: PreviewImage, format: ImageFormat): EncodedImage {
        onEncode()
        encodedFormats += format
        val file = File.createTempFile("encoded", ".tmp", directory).apply { writeText(EDITED) }
        return EncodedImage(file)
    }

    override fun release(image: PreviewImage) {
        released += image
    }

    private fun image(width: Int, height: Int) = PreviewImage(Any(), width, height).also { live += it }

    companion object {
        const val EDITED = "edited pixels"
    }
}

/** Keeps one file per URI, like the gallery would, and records the order of the calls. */
class FakeMediaWriteSink(private val directory: File) : MediaWriteSink {
    val calls = mutableListOf<String>()
    val contents = mutableMapOf<String, String>()
    val pending = mutableSetOf<String>()
    var existing = emptySet<String>()
    var insertedName: String? = null
    var insertedPath: String? = null
    var insertedMime: String? = null
    var insertedVolume: String? = null
    var failWriteWith: Exception = java.io.IOException("disk full")
    var failNextWrite = false
    var failWrites = 0
    var failBackup = false
    var failPublish = false
    var failDiscard = false
    var failCleanup = false
    var cleanupBefore: Long? = null
    var onWrite: () -> Unit = {}
    val confirmation = PlatformConfirmation(Any())

    override suspend fun existingNames(volumeName: String, relativePath: String): Set<String> = existing

    override suspend fun insertPending(
        volumeName: String,
        relativePath: String,
        displayName: String,
        mimeType: String,
    ): String {
        calls += "insert"
        insertedVolume = volumeName
        insertedPath = relativePath
        insertedName = displayName
        insertedMime = mimeType
        pending += NEW_URI
        return NEW_URI
    }

    override suspend fun write(contentUri: String, image: EncodedImage) {
        calls += "write"
        onWrite()
        if (failNextWrite || failWrites > 0) {
            failNextWrite = false
            if (failWrites > 0) failWrites--
            // A failed write leaves a damaged file behind.
            contents[contentUri] = "damaged"
            throw failWriteWith
        }
        contents[contentUri] = image.file.readText()
    }

    override suspend fun publish(contentUri: String): MediaId {
        calls += "publish"
        if (failPublish) throw java.io.IOException("publish failed")
        pending -= contentUri
        return NEW_ID
    }

    override suspend fun discard(contentUri: String) {
        calls += "discard"
        if (failDiscard) throw java.io.IOException("discard failed")
        pending -= contentUri
        contents -= contentUri
    }

    override suspend fun requestOverwrite(contentUri: String): PlatformConfirmation {
        calls += "request"
        return confirmation
    }

    override suspend fun backup(contentUri: String): EncodedImage {
        calls += "backup"
        if (failBackup) throw java.io.IOException("no space for backup")
        val file = File.createTempFile("backup", ".tmp", directory).apply { writeText(contents.getValue(contentUri)) }
        return EncodedImage(file)
    }

    override suspend fun cleanupPending(olderThanMillis: Long) {
        if (failCleanup) throw java.io.IOException("cleanup failed")
        cleanupBefore = olderThanMillis
    }

    companion object {
        const val NEW_URI = "content://media/new"
        val NEW_ID = MediaId(99)
    }
}

class FakeExifWriter : ExifWriter {
    var sourceTags = mapOf("Make" to "Google", "Orientation" to "6", "GPSLatitude" to "1/1,2/1,3/1")
    var written: Map<String, String>? = null
    var failWrite = false
    var requestedTags: Set<String> = emptySet()

    override suspend fun readTags(contentUri: String, tags: Set<String>): Map<String, String> {
        requestedTags = tags
        return sourceTags
    }

    override suspend fun writeTags(image: EncodedImage, tags: Map<String, String>) {
        if (failWrite) throw java.io.IOException("exif failed")
        written = tags
    }
}
