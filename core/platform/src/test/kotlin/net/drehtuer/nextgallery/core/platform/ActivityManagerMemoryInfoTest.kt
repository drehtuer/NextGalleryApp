package net.drehtuer.nextgallery.core.platform

import android.app.ActivityManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class ActivityManagerMemoryInfoTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun availableWith(availMem: Long, threshold: Long, totalMem: Long): Long {
        val memoryInfo = ActivityManager.MemoryInfo().also {
            it.availMem = availMem
            it.threshold = threshold
            it.totalMem = totalMem
        }
        shadowOf(context.getSystemService(ActivityManager::class.java)).setMemoryInfo(memoryInfo)
        return ActivityManagerMemoryInfo(context).availableBytes()
    }

    @Test
    fun availableBytes_subtractsThresholdAndMargin() {
        val available = availableWith(1_000_000_000, 100_000_000, 8_000_000_000)

        assertEquals(1_000_000_000 - 100_000_000 - ActivityManagerMemoryInfo.SAFETY_MARGIN_BYTES, available)
    }

    @Test
    fun availableBytes_nearlyAllFree_isLimitedToAPartOfTheRam() {
        assertEquals(4_000_000_000, availableWith(7_900_000_000, 100_000_000, 8_000_000_000))
    }

    @Test
    fun availableBytes_lessThanTheMargin_isZero() {
        assertEquals(0, availableWith(100_000_000, 50_000_000, 8_000_000_000))
    }
}
