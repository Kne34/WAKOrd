package edu.bluejack26_1.wakord.domain.repository

import edu.bluejack26_1.wakord.domain.model.ChordProgression
import edu.bluejack26_1.wakord.domain.model.Comment
import edu.bluejack26_1.wakord.domain.model.ProgressionDraft
import edu.bluejack26_1.wakord.domain.model.SavedType
import edu.bluejack26_1.wakord.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface SongRepository {
    fun song(id: String): Flow<Song?>
    fun isLiked(id: String): Flow<Boolean>
    suspend fun setLiked(id: String, liked: Boolean): Result<Unit>
}

interface ProgressionRepository {
    fun progression(id: String): Flow<ChordProgression?>
    fun isLiked(id: String): Flow<Boolean>
    suspend fun setLiked(id: String, liked: Boolean): Result<Unit>
    suspend fun recordPlay(id: String): Result<Unit>
    suspend fun create(draft: ProgressionDraft): Result<String>
}

interface CommentRepository {
    fun comments(targetId: String): Flow<List<Comment>>
    suspend fun post(targetId: String, text: String, parentId: String? = null): Result<Unit>
    suspend fun setLiked(targetId: String, commentId: String, liked: Boolean): Result<Unit>
}

interface SavedItemsRepository {
    fun isSaved(type: SavedType, id: String): Flow<Boolean>
    fun saved(type: SavedType): Flow<List<String>>
    suspend fun setSaved(type: SavedType, id: String, saved: Boolean): Result<Unit>
}
