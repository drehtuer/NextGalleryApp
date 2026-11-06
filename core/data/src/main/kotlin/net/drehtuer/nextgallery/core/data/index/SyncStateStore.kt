package net.drehtuer.nextgallery.core.data.index

/** Port: persisted [SyncState] per volume. Implemented with Room in `:core:database`. */
interface SyncStateStore {
    suspend fun get(volume: String): SyncState?

    suspend fun put(state: SyncState)

    suspend fun hasAny(): Boolean
}
