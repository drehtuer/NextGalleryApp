package net.drehtuer.nextgallery.core.common

import java.time.ZoneId
import javax.inject.Inject

/** Testable access to the current time and the device time zone. */
interface Clock {
    fun nowMillis(): Long

    fun zone(): ZoneId
}

class SystemClock @Inject constructor() : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()

    override fun zone(): ZoneId = ZoneId.systemDefault()
}
