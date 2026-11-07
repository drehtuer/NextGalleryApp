package net.drehtuer.nextgallery.core.data.edit

/** Port: how much memory an edit may use right now. Implemented in `:core:platform`. */
fun interface MemoryInfoSource {
    fun availableBytes(): Long
}
