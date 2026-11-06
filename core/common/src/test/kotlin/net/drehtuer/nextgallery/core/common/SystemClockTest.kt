package net.drehtuer.nextgallery.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class SystemClockTest {
    @Test
    fun nowMillis_returnsCurrentTime() {
        val before = System.currentTimeMillis()

        val now = SystemClock().nowMillis()

        assertTrue(now in before..System.currentTimeMillis())
    }

    @Test
    fun zone_returnsSystemDefaultZone() {
        assertEquals(ZoneId.systemDefault(), SystemClock().zone())
    }
}
