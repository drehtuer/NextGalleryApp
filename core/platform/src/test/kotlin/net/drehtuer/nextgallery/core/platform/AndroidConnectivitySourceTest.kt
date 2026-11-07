package net.drehtuer.nextgallery.core.platform

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class AndroidConnectivitySourceTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val manager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @Test
    fun isOnline_withNetwork_isTrue() = runTest {
        assertTrue(AndroidConnectivitySource(application).isOnline.first())
    }

    @Test
    fun isOnline_withoutNetwork_isFalse() = runTest {
        shadowOf(manager).setActiveNetworkInfo(null)

        assertFalse(AndroidConnectivitySource(application).isOnline.first())
    }

    @Test
    fun isOnline_networkLost_emitsFalse() = runTest {
        AndroidConnectivitySource(application).isOnline.test {
            assertTrue(awaitItem())

            shadowOf(manager).networkCallbacks.single().onLost(checkNotNull(manager.activeNetwork))

            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun isOnline_networkBack_emitsTrue() = runTest {
        shadowOf(manager).setActiveNetworkInfo(null)

        AndroidConnectivitySource(application).isOnline.test {
            assertFalse(awaitItem())

            val network = org.robolectric.shadows.ShadowNetwork.newInstance(1)
            shadowOf(manager).networkCallbacks.single().onAvailable(network)

            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun isOnline_afterTheCollectorLeft_unregistersTheCallback() = runTest {
        AndroidConnectivitySource(application).isOnline.first()

        assertEquals(0, shadowOf(manager).networkCallbacks.size)
    }
}
