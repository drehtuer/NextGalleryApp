package net.drehtuer.nextgallery.core.data.index

/** Port: schedules background work. Implemented with WorkManager in `:core:platform`. */
fun interface WorkScheduler {
    fun enqueueIndex(mode: IndexMode)
}
