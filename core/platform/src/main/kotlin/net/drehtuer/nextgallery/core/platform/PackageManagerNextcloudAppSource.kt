package net.drehtuer.nextgallery.core.platform

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import net.drehtuer.nextgallery.core.data.upload.NextcloudAppSource
import net.drehtuer.nextgallery.core.model.NEXTCLOUD_APP_PACKAGE
import javax.inject.Inject

/** Needs the `<queries>` entry in the manifest: without it, Android 11+ reports the app as missing. */
class PackageManagerNextcloudAppSource @Inject constructor(@ApplicationContext private val context: Context) :
    NextcloudAppSource {
    override fun isInstalled(): Boolean = try {
        context.packageManager.getPackageInfo(NEXTCLOUD_APP_PACKAGE, 0)
        true
    } catch (
        @Suppress("SwallowedException") _: PackageManager.NameNotFoundException,
    ) {
        false
    }
}
