package net.drehtuer.nextgallery.core.data.index

/** Port: schedules background work. Implemented with WorkManager in `:core:platform`. */
interface WorkScheduler {
    fun enqueueIndex(mode: IndexMode)

    /** The periodic upload-state check. Safe to call on every app start: it stays one unique work. */
    fun scheduleUploadStateSync()

    /** One upload-state check as soon as there is a network. */
    fun requestUploadStateSync()
}
