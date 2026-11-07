package net.drehtuer.nextgallery.core.media

import android.Manifest
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class AndroidMediaPermissionSourceTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun grantedPermissions_noneGranted_returnsEmptySet() {
        assertEquals(emptySet<String>(), AndroidMediaPermissionSource(application).grantedPermissions())
    }

    @Test
    fun grantedPermissions_coarseLocationGranted_containsIt() {
        shadowOf(application).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)

        assertEquals(
            setOf(Manifest.permission.ACCESS_COARSE_LOCATION),
            AndroidMediaPermissionSource(application).grantedPermissions(),
        )
    }

    @Test
    fun grantedPermissions_readMediaImagesGranted_containsIt() {
        shadowOf(application).grantPermissions(Manifest.permission.READ_MEDIA_IMAGES)

        assertEquals(
            setOf(Manifest.permission.READ_MEDIA_IMAGES),
            AndroidMediaPermissionSource(application).grantedPermissions(),
        )
    }
}
