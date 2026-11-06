package net.drehtuer.nextgallery.core.common

import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertSame
import org.junit.Test

class DefaultDispatcherProviderTest {
    @Test
    fun io_returnsDispatchersIo() {
        assertSame(Dispatchers.IO, DefaultDispatcherProvider().io)
    }

    @Test
    fun default_returnsDispatchersDefault() {
        assertSame(Dispatchers.Default, DefaultDispatcherProvider().default)
    }
}
