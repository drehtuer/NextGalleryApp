package net.drehtuer.nextgallery.core.data.edit

import net.drehtuer.nextgallery.core.model.edit.ImageSize

sealed interface MemoryCheck {
    data object Fits : MemoryCheck

    data class Refused(val requiredBytes: Long, val availableBytes: Long) : MemoryCheck
}

/** Decides whether an edit may run (D30). Bitmap pixels take 4 bytes; an edit holds the source and the output. */
object MemoryBudget {
    private const val BYTES_PER_PIXEL = 4L
    private const val BYTES_PER_MEGABYTE = 1_000_000L

    fun requiredBytes(source: ImageSize, output: ImageSize): Long = (source.pixels + output.pixels) * BYTES_PER_PIXEL

    fun check(source: ImageSize, output: ImageSize, availableBytes: Long): MemoryCheck {
        val required = requiredBytes(source, output)
        return if (required <= availableBytes) MemoryCheck.Fits else MemoryCheck.Refused(required, availableBytes)
    }

    /** Rounds up, and [availableMegabytes] rounds down, so a refusal never reads as if the memory were enough. */
    fun requiredMegabytes(bytes: Long): Long = (bytes + BYTES_PER_MEGABYTE - 1) / BYTES_PER_MEGABYTE

    fun availableMegabytes(bytes: Long): Long = bytes.coerceAtLeast(0) / BYTES_PER_MEGABYTE
}
