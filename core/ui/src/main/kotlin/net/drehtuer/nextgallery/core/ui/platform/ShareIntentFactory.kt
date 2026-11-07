package net.drehtuer.nextgallery.core.ui.platform

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import net.drehtuer.nextgallery.core.model.ShareSpec

object ShareIntentFactory {
    /**
     * The read permission is always granted explicitly; implicit grants end with Android 18 (D40). A target package
     * makes the intent explicit (D31).
     */
    fun create(spec: ShareSpec): Intent {
        val uris = spec.contentUris.map { it.toUri() }
        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris.single())
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }
        return intent.apply {
            type = spec.mimeType
            spec.targetPackage?.let { setPackage(it) }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri(null, uris.first()).apply {
                uris.drop(1).forEach { addItem(ClipData.Item(it)) }
            }
        }
    }
}

/**
 * Opens the share sheet. False if the system refuses: an intent with too many images for its transaction limit raises
 * a plain [RuntimeException] (see [handOffImages]), and "select all" gets there easily.
 */
internal fun Context.shareImages(spec: ShareSpec): Boolean = try {
    startActivity(Intent.createChooser(ShareIntentFactory.create(spec), null))
    true
} catch (
    @Suppress("TooGenericExceptionCaught", "SwallowedException") _: RuntimeException,
) {
    false
}

internal enum class HandoffStart { STARTED, APP_MISSING, FAILED }

/**
 * Opens the target app directly, without a chooser. The system refuses an intent with too many images for its
 * transaction limit with a plain [RuntimeException], and "select all" gets there easily, so that must not end the app.
 */
internal fun Context.handOffImages(spec: ShareSpec): HandoffStart = try {
    startActivity(ShareIntentFactory.create(spec))
    HandoffStart.STARTED
} catch (
    @Suppress("SwallowedException") _: ActivityNotFoundException,
) {
    HandoffStart.APP_MISSING
} catch (
    @Suppress("TooGenericExceptionCaught", "SwallowedException") _: RuntimeException,
) {
    HandoffStart.FAILED
}
