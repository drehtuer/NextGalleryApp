package net.drehtuer.nextgallery.core.media

import android.content.Context
import net.drehtuer.nextgallery.core.common.deleteIfPossible
import java.io.File

/** Temporary files of the editor: encoded images and backups. */
internal object EditorFiles {
    private const val DIRECTORY = "editor"

    fun newTemporaryFile(context: Context, prefix: String): File {
        val directory = File(context.cacheDir, DIRECTORY).apply { mkdirs() }
        return File.createTempFile(prefix, null, directory)
    }

    /** A crash can leave files behind; anything this old is not part of a running save. */
    fun deleteOlderThan(context: Context, olderThanMillis: Long) {
        File(context.cacheDir, DIRECTORY).listFiles().orEmpty()
            .filter { it.lastModified() < olderThanMillis }
            .forEach { it.deleteIfPossible() }
    }
}
