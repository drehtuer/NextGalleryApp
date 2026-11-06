package net.drehtuer.nextgallery.core.ui.platform

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import net.drehtuer.nextgallery.core.model.ShareSpec

object ShareIntentFactory {
    /** The read permission is always granted explicitly; implicit grants end with Android 18 (D40). */
    fun create(spec: ShareSpec): Intent {
        val uris = spec.contentUris.map { it.toUri() }
        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris.single())
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }
        return intent.apply {
            type = spec.mimeType
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri(null, uris.first()).apply {
                uris.drop(1).forEach { addItem(ClipData.Item(it)) }
            }
        }
    }
}

fun Context.shareImages(spec: ShareSpec) = startActivity(Intent.createChooser(ShareIntentFactory.create(spec), null))
