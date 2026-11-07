package net.drehtuer.nextgallery.core.data.edit

import net.drehtuer.nextgallery.core.model.edit.ImageSize
import org.junit.Assert.assertEquals
import org.junit.Test

class MemoryBudgetTest {
    private val source = ImageSize(4000, 3000)

    @Test
    fun requiredBytes_countsSourceAndOutputAtFourBytesPerPixel() {
        assertEquals(480_000_000L, MemoryBudget.requiredBytes(source, ImageSize(12000, 9000)))
    }

    @Test
    fun requiredBytes_hugeImages_doNotOverflow() {
        val huge = ImageSize(32_000, 32_000)

        assertEquals(8_192_000_000L, MemoryBudget.requiredBytes(huge, huge))
    }

    @Test
    fun check_enoughMemory_fits() {
        assertEquals(MemoryCheck.Fits, MemoryBudget.check(source, source, 200_000_000))
    }

    @Test
    fun check_exactlyAtLimit_fits() {
        assertEquals(MemoryCheck.Fits, MemoryBudget.check(source, source, 96_000_000))
    }

    @Test
    fun check_oneByteShort_isRefused() {
        assertEquals(
            MemoryCheck.Refused(96_000_000, 95_999_999),
            MemoryBudget.check(source, source, 95_999_999),
        )
    }

    @Test
    fun check_outputMuchLarger_isRefusedWithBothNumbers() {
        assertEquals(
            MemoryCheck.Refused(480_000_000, 300_000_000),
            MemoryBudget.check(source, ImageSize(12000, 9000), 300_000_000),
        )
    }

    @Test
    fun check_nothingAvailable_isRefused() {
        assertEquals(MemoryCheck.Refused(96_000_000, 0), MemoryBudget.check(source, source, 0))
    }

    @Test
    fun requiredMegabytes_roundsUp() {
        assertEquals(301, MemoryBudget.requiredMegabytes(300_000_001))
    }

    @Test
    fun requiredMegabytes_exact_staysTheSame() {
        assertEquals(480, MemoryBudget.requiredMegabytes(480_000_000))
    }

    @Test
    fun availableMegabytes_roundsDown() {
        assertEquals(299, MemoryBudget.availableMegabytes(299_999_999))
    }

    @Test
    fun availableMegabytes_negative_isZero() {
        assertEquals(0, MemoryBudget.availableMegabytes(-5))
    }
}
