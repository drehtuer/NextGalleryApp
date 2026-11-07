package net.drehtuer.nextgallery.core.platform

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.drehtuer.nextgallery.core.data.nextcloud.ACCESS_LOCAL_NETWORK_PERMISSION
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class AndroidLocalNetworkPermissionSourceTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun isGranted_notGranted_isFalse() {
        assertFalse(AndroidLocalNetworkPermissionSource(application).isGranted())
    }

    @Test
    fun isGranted_granted_isTrue() {
        shadowOf(application).grantPermissions(ACCESS_LOCAL_NETWORK_PERMISSION)

        assertTrue(AndroidLocalNetworkPermissionSource(application).isGranted())
    }
}
