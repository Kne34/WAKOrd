package edu.bluejack26_1.wakord.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import edu.bluejack26_1.wakord.domain.model.AnalysisResult
import edu.bluejack26_1.wakord.domain.model.AnalysisSource
import edu.bluejack26_1.wakord.domain.model.DetectedChord
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "analyses")
data class AnalysisEntity(
    @PrimaryKey val id: String,
    val ownerUid: String,
    val createdAt: Long,
    val durationMs: Long,
    val key: String,
    val confidence: Float,
    val bpm: Int,
    val chords: String,
    val source: String,
    val synced: Boolean,
) {
    fun toDomain() = AnalysisResult(
        id = id,
        createdAt = createdAt,
        durationMs = durationMs,
        key = key,
        confidence = confidence,
        bpm = bpm,
        chords = decodeChords(chords),
        source = AnalysisSource.entries.firstOrNull { it.name == source } ?: AnalysisSource.ALGORITHM,
    )

    companion object {
        fun from(result: AnalysisResult, ownerUid: String, synced: Boolean) = AnalysisEntity(
            id = result.id,
            ownerUid = ownerUid,
            createdAt = result.createdAt,
            durationMs = result.durationMs,
            key = result.key,
            confidence = result.confidence,
            bpm = result.bpm,
            chords = encodeChords(result.chords),
            source = result.source.name,
            synced = synced,
        )

        fun encodeChords(chords: List<DetectedChord>): String =
            chords.joinToString(";") { "${it.symbol}|${it.startMs}|${it.endMs}" }

        fun decodeChords(text: String): List<DetectedChord> = text.split(';').mapNotNull { part ->
            val fields = part.split('|')
            if (fields.size != 3) return@mapNotNull null
            DetectedChord(fields[0], fields[1].toLongOrNull() ?: 0L, fields[2].toLongOrNull() ?: 0L)
        }
    }
}

@Dao
interface AnalysisDao {
    @Query("SELECT * FROM analyses WHERE ownerUid = :uid ORDER BY createdAt DESC")
    fun observe(uid: String): Flow<List<AnalysisEntity>>

    @Query("SELECT * FROM analyses WHERE ownerUid = :uid AND synced = 0")
    suspend fun unsynced(uid: String): List<AnalysisEntity>

    @Query("UPDATE analyses SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Upsert
    suspend fun upsert(entities: List<AnalysisEntity>)
}
