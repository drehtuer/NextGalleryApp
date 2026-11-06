package net.drehtuer.nextgallery.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface SyncStateDao {
    @Query("SELECT * FROM sync_state WHERE volume_name = :volume")
    suspend fun get(volume: String): SyncStateEntity?

    @Upsert
    suspend fun put(state: SyncStateEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM sync_state)")
    suspend fun hasAny(): Boolean
}
