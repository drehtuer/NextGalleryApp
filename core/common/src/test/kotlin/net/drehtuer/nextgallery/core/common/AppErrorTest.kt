package net.drehtuer.nextgallery.core.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class AppErrorTest {
    @Test
    fun from_ioException_returnsIo() {
        assertEquals(AppError.Io("disk"), AppError.from(IOException("disk")))
    }

    @Test
    fun from_securityException_returnsPermissionDenied() {
        assertEquals(AppError.PermissionDenied, AppError.from(SecurityException()))
    }

    @Test
    fun from_otherException_returnsUnknownWithCause() {
        val cause = IllegalStateException()

        assertEquals(AppError.Unknown(cause), AppError.from(cause))
    }
}
