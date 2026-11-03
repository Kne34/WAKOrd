package edu.bluejack26_1.wakord.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "backup_nodes")
data class BackupNodeEntity(
    @PrimaryKey val path: String,
    val json: String,
    val updatedAt: Long,
)

@Entity(tableName = "pending_writes")
data class PendingWriteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUid: String,
    val updates: String,
    val createdAt: Long,
)

@Dao
interface BackupDao {
    @Query("SELECT * FROM backup_nodes")
    suspend fun nodes(): List<BackupNodeEntity>

    @Query("SELECT * FROM backup_nodes")
    fun observeNodes(): Flow<List<BackupNodeEntity>>

    @Upsert
    suspend fun upsertNodes(nodes: List<BackupNodeEntity>)

    @Query("DELETE FROM backup_nodes WHERE path IN (:paths)")
    suspend fun deleteNodes(paths: List<String>)

    @Transaction
    suspend fun replaceNodes(upserts: List<BackupNodeEntity>, deletes: List<String>) {
        if (deletes.isNotEmpty()) deleteNodes(deletes)
        if (upserts.isNotEmpty()) upsertNodes(upserts)
    }

    @Insert
    suspend fun enqueue(write: PendingWriteEntity)

    @Query("SELECT * FROM pending_writes WHERE ownerUid = :uid ORDER BY id")
    suspend fun pending(uid: String): List<PendingWriteEntity>

    @Query("SELECT COUNT(*) FROM pending_writes WHERE ownerUid = :uid")
    fun observePendingCount(uid: String): Flow<Int>

    @Query("DELETE FROM pending_writes WHERE id = :id")
    suspend fun deletePending(id: Long)
}
