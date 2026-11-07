package net.drehtuer.nextgallery.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface RemoteCacheDao {
    @Query("SELECT * FROM remote_cache WHERE parent_path = :parentPath")
    suspend fun getFolder(parentPath: String): List<RemoteCacheEntity>

    @Query("DELETE FROM remote_cache WHERE parent_path = :parentPath")
    suspend fun deleteFolder(parentPath: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(items: List<RemoteCacheEntity>)

    @Transaction
    suspend fun replaceFolder(parentPath: String, items: List<RemoteCacheEntity>) {
        deleteFolder(parentPath)
        insert(items)
    }

    @Query("DELETE FROM remote_cache")
    suspend fun clear()
}
