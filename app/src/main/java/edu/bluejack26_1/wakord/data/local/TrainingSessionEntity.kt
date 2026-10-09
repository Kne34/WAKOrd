package edu.bluejack26_1.wakord.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import edu.bluejack26_1.wakord.domain.model.SkillType
import edu.bluejack26_1.wakord.domain.model.TrainingSession
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "training_sessions")
data class TrainingSessionEntity(
    @PrimaryKey val id: String,
    val ownerUid: String,
    val level: Int,
    val skill: String,
    val correct: Int,
    val total: Int,
    val durationMs: Long,
    val startedAt: Long,
    val synced: Boolean,
) {
    fun toDomain() = TrainingSession(
        id = id,
        level = level,
        skill = SkillType.entries.firstOrNull { it.name == skill } ?: SkillType.SINGLE_NOTE,
        correct = correct,
        total = total,
        durationMs = durationMs,
        startedAt = startedAt,
    )

    companion object {
        fun from(session: TrainingSession, ownerUid: String, synced: Boolean) = TrainingSessionEntity(
            id = session.id,
            ownerUid = ownerUid,
            level = session.level,
            skill = session.skill.name,
            correct = session.correct,
            total = session.total,
            durationMs = session.durationMs,
            startedAt = session.startedAt,
            synced = synced,
        )
    }
}

@Dao
interface TrainingDao {
    @Query("SELECT * FROM training_sessions WHERE ownerUid = :uid ORDER BY startedAt")
    fun observe(uid: String): Flow<List<TrainingSessionEntity>>

    @Query("SELECT * FROM training_sessions WHERE ownerUid = :uid AND synced = 0")
    suspend fun unsynced(uid: String): List<TrainingSessionEntity>

    @Query("UPDATE training_sessions SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Upsert
    suspend fun upsert(entities: List<TrainingSessionEntity>)
}
