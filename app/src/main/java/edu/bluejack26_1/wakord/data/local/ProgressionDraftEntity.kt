package edu.bluejack26_1.wakord.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import edu.bluejack26_1.wakord.domain.model.Difficulty
import edu.bluejack26_1.wakord.domain.model.ProgressionDraft
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "progression_drafts")
data class ProgressionDraftEntity(
    @PrimaryKey val ownerUid: String,
    val title: String,
    val key: String,
    val bpm: Int,
    val genre: String,
    val difficulty: String,
    val chords: String,
    val description: String,
) {
    fun toDomain() = ProgressionDraft(
        title = title,
        key = key,
        bpm = bpm,
        genre = genre,
        difficulty = Difficulty.entries.firstOrNull { it.name == difficulty } ?: Difficulty.EASY,
        chords = chords.split('|').filter { it.isNotEmpty() },
        description = description,
    )

    companion object {
        fun from(draft: ProgressionDraft, ownerUid: String) = ProgressionDraftEntity(
            ownerUid = ownerUid,
            title = draft.title,
            key = draft.key,
            bpm = draft.bpm,
            genre = draft.genre,
            difficulty = draft.difficulty.name,
            chords = draft.chords.joinToString("|"),
            description = draft.description,
        )
    }
}

@Dao
interface DraftDao {
    @Query("SELECT * FROM progression_drafts WHERE ownerUid = :uid")
    fun observe(uid: String): Flow<ProgressionDraftEntity?>

    @Upsert
    suspend fun upsert(entity: ProgressionDraftEntity)

    @Query("DELETE FROM progression_drafts WHERE ownerUid = :uid")
    suspend fun delete(uid: String)
}
