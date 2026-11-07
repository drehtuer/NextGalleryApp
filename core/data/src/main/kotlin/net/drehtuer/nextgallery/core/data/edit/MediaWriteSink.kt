package net.drehtuer.nextgallery.core.data.edit

import net.drehtuer.nextgallery.core.model.MediaId
import net.drehtuer.nextgallery.core.model.PlatformConfirmation

/** Port: writes images to the gallery. Implemented with MediaStore in `:core:media`. */
interface MediaWriteSink {
    /** File names in a folder, as far as the app can see them. */
    suspend fun existingNames(volumeName: String, relativePath: String): Set<String>

    /** Creates a hidden entry and returns its content URI; only [publish] makes it visible. */
    suspend fun insertPending(volumeName: String, relativePath: String, displayName: String, mimeType: String): String

    /** Replaces the content of an entry. */
    suspend fun write(contentUri: String, image: EncodedImage)

    suspend fun publish(contentUri: String): MediaId

    /** Removes a pending entry that was not finished. */
    suspend fun discard(contentUri: String)

    /** The system dialog that allows overwriting an image. */
    suspend fun requestOverwrite(contentUri: String): PlatformConfirmation

    /** A temporary copy of the current content of an entry, to put back if overwriting fails. */
    suspend fun backup(contentUri: String): EncodedImage

    /** Deletes pending entries and temporary files a crash left behind before [olderThanMillis]. */
    suspend fun cleanupPending(olderThanMillis: Long)
}
