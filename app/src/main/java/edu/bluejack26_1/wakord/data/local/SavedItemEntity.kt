package edu.bluejack26_1.wakord.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

enum class SyncState { SYNCED, PENDING_SAVE, PENDING_REMOVE }

@Entity(tableName = "saved_items", primaryKeys = ["ownerUid", "type", "itemId"])
data class SavedItemEntity(
    val ownerUid: String,
    val type: String,
    val itemId: String,
    val title: String,
    val subtitle: String,
    val savedAt: Long,
    val state: String,
)

@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val ownerUid: String,
    val lastSyncedAt: Long,
)

@Dao
interface SavedItemDao {
    @Query("SELECT * FROM saved_items WHERE ownerUid = :uid ORDER BY savedAt DESC")
    fun observeAll(uid: String): Flow<List<SavedItemEntity>>

    @Query("SELECT * FROM saved_items WHERE ownerUid = :uid")
    suspend fun all(uid: String): List<SavedItemEntity>

    @Upsert
    suspend fun upsert(entities: List<SavedItemEntity>)

    @Query("DELETE FROM saved_items WHERE ownerUid = :uid AND type = :type AND itemId = :itemId")
    suspend fun delete(uid: String, type: String, itemId: String)

    @Query("SELECT * FROM sync_state WHERE ownerUid = :uid")
    fun observeSyncState(uid: String): Flow<SyncStateEntity?>

    @Upsert
    suspend fun upsertSyncState(entity: SyncStateEntity)
}
