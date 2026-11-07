package net.drehtuer.nextgallery.core.platform

import android.app.Application
import android.content.pm.PackageInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class PackageManagerNextcloudAppSourceTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun isInstalled_notInstalled_isFalse() {
        assertFalse(PackageManagerNextcloudAppSource(application).isInstalled())
    }

    @Test
    fun isInstalled_installed_isTrue() {
        val nextcloud = PackageInfo().apply { packageName = "com.nextcloud.client" }
        shadowOf(application.packageManager).installPackage(nextcloud)

        assertTrue(PackageManagerNextcloudAppSource(application).isInstalled())
    }
}
