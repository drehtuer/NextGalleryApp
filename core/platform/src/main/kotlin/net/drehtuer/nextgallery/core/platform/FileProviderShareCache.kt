package net.drehtuer.nextgallery.core.platform

import android.content.Context
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import net.drehtuer.nextgallery.core.data.nextcloud.RemoteShareCache
import java.io.File
import javax.inject.Inject

/** The one folder the `FileProvider` of this module gives access to (D48). */
class FileProviderShareCache @Inject constructor(@param:ApplicationContext private val context: Context) :
    RemoteShareCache {
    private val root: File get() = File(context.cacheDir, FOLDER)

    override fun clear() {
        root.deleteRecursively()
    }

    override fun newFile(index: Int, name: String): File {
        val folder = File(root, index.toString()).apply { mkdirs() }
        return File(folder, name)
    }

    override fun contentUri(file: File): String =
        FileProvider.getUriForFile(context, "${context.packageName}$AUTHORITY_SUFFIX", file).toString()

    private companion object {
        const val FOLDER = "remote_share"
        const val AUTHORITY_SUFFIX = ".remoteshare"
    }
}
