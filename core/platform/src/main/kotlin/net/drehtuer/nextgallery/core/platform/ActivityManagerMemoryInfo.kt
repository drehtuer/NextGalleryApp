package net.drehtuer.nextgallery.core.platform

import android.app.ActivityManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import net.drehtuer.nextgallery.core.data.edit.MemoryInfoSource
import javax.inject.Inject

/**
 * Memory an edit may use (D30). Bitmap pixels are native memory, so the Java heap limit does not count. Android 17
 * also limits each app, based on the device's RAM, but has no API to read that limit (D40), so an edit never gets
 * more than a part of the RAM, whatever is free. Check this against `am memory-limiter status` on the device.
 */
class ActivityManagerMemoryInfo @Inject constructor(@param:ApplicationContext private val context: Context) :
    MemoryInfoSource {
    override fun availableBytes(): Long {
        val info = ActivityManager.MemoryInfo()
        context.getSystemService(ActivityManager::class.java).getMemoryInfo(info)
        val free = info.availMem - info.threshold - SAFETY_MARGIN_BYTES
        return minOf(free, info.totalMem / MAX_PART_OF_RAM).coerceAtLeast(0)
    }

    internal companion object {
        const val SAFETY_MARGIN_BYTES = 200_000_000L
        const val MAX_PART_OF_RAM = 2
    }
}
