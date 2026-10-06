package edu.bluejack26_1.wakord.ui.feature.progression

import androidx.annotation.StringRes
import edu.bluejack26_1.wakord.domain.model.ChordProgression
import edu.bluejack26_1.wakord.domain.model.ChordVoicing
import edu.bluejack26_1.wakord.domain.model.Comment

data class ProgressionDetailUiState(
    val isLoading: Boolean = true,
    val progression: ChordProgression? = null,
    val chordDiagrams: List<ChordVoicing?> = emptyList(),
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val isPlaying: Boolean = false,
    val playingIndex: Int? = null,
    val selectedChord: String? = null,
    val voicings: List<ChordVoicing> = emptyList(),
    val selectedVoicing: Int = 0,
    val isChordSaved: Boolean = false,
    val comments: List<Comment> = emptyList(),
    val commentDraft: String = "",
    val isPosting: Boolean = false,
    val showCommentBlocked: Boolean = false,
    @param:StringRes val errorMessage: Int? = null,
)

sealed interface ProgressionDetailAction {
    data object TogglePlay : ProgressionDetailAction
    data class OpenChord(val symbol: String) : ProgressionDetailAction
    data object CloseChord : ProgressionDetailAction
    data class SelectVoicing(val index: Int) : ProgressionDetailAction
    data object PreviewChord : ProgressionDetailAction
    data object ToggleChordSaved : ProgressionDetailAction
    data object ToggleLike : ProgressionDetailAction
    data object ToggleSave : ProgressionDetailAction
    data class CommentChanged(val text: String) : ProgressionDetailAction
    data object PostComment : ProgressionDetailAction
    data class ToggleCommentLike(val comment: Comment) : ProgressionDetailAction
    data object DismissCommentBlocked : ProgressionDetailAction
    data object DismissError : ProgressionDetailAction
}
