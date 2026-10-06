package edu.bluejack26_1.wakord.ui.feature.song

import androidx.annotation.StringRes
import edu.bluejack26_1.wakord.domain.model.Comment
import edu.bluejack26_1.wakord.domain.model.Song

data class ChordPosition(val section: Int, val line: Int, val segment: Int)

data class SongDetailUiState(
    val isLoading: Boolean = true,
    val song: Song? = null,
    val originalKey: String = "",
    val transposeSteps: Int = 0,
    val isPlaying: Boolean = false,
    val playbackSpeed: Float = 1f,
    val playingPosition: ChordPosition? = null,
    val nextPosition: ChordPosition? = null,
    val elapsedMs: Long = 0,
    val totalMs: Long = 0,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val comments: List<Comment> = emptyList(),
    val commentDraft: String = "",
    val replyingTo: Comment? = null,
    val isPosting: Boolean = false,
    val showCommentBlocked: Boolean = false,
    @param:StringRes val errorMessage: Int? = null,
)

sealed interface SongDetailAction {
    data class Transpose(val steps: Int) : SongDetailAction
    data object ResetTranspose : SongDetailAction
    data object TogglePlay : SongDetailAction
    data class SetSpeed(val speed: Float) : SongDetailAction
    data object ToggleLike : SongDetailAction
    data object ToggleSave : SongDetailAction
    data class CommentChanged(val text: String) : SongDetailAction
    data class ReplyTo(val comment: Comment?) : SongDetailAction
    data object PostComment : SongDetailAction
    data class ToggleCommentLike(val comment: Comment) : SongDetailAction
    data object DismissCommentBlocked : SongDetailAction
    data object DismissError : SongDetailAction
}

val PlaybackSpeeds = listOf(0.5f, 0.75f, 1f)
