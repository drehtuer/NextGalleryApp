package net.drehtuer.nextgallery.core.platform

import android.Manifest
import android.app.Application
import android.location.Location
import android.location.LocationManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import net.drehtuer.nextgallery.core.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class AndroidLocationProviderTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val manager = application.getSystemService(LocationManager::class.java)
    private val provider = AndroidLocationProvider(application)

    private fun location(providerName: String, latitude: Double, longitude: Double, time: Long) =
        Location(providerName).apply {
            this.latitude = latitude
            this.longitude = longitude
            this.time = time
        }

    @Before
    fun setUp() {
        shadowOf(application).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    @Test
    fun currentLocation_lastKnownPosition_isReturned() = runTest {
        shadowOf(manager).setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
        shadowOf(manager).simulateLocation(location(LocationManager.NETWORK_PROVIDER, 45.5, 10.5, 1_000))

        assertEquals(GeoPoint(45.5, 10.5), provider.currentLocation())
    }

    @Test
    fun currentLocation_noFixInTime_returnsNull() = runTest {
        shadowOf(manager).setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)

        assertNull(provider.currentLocation())
    }

    @Test
    fun currentLocation_permissionMissing_returnsNull() = runTest {
        shadowOf(application).denyPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        shadowOf(manager).setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)

        assertNull(provider.currentLocation())
    }

    @Test
    fun currentLocation_noProviderEnabled_returnsNull() = runTest {
        manager.allProviders.forEach { shadowOf(manager).setProviderEnabled(it, false) }

        assertNull(provider.currentLocation())
    }
}
