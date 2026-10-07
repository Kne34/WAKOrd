package edu.bluejack26_1.wakord.ui.feature.home

import androidx.annotation.StringRes
import edu.bluejack26_1.wakord.domain.model.ChordProgression
import edu.bluejack26_1.wakord.domain.model.ChordVoicing
import edu.bluejack26_1.wakord.domain.model.FeedItem
import edu.bluejack26_1.wakord.domain.model.Song

data class SavedChord(val symbol: String, val voicing: ChordVoicing?)

data class HomeUiState(
    val userName: String = "",
    val photoUrl: String? = null,
    val recommended: List<Song> = emptyList(),
    val savedChords: List<SavedChord> = emptyList(),
    val feed: List<FeedItem> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val endReached: Boolean = false,
    @param:StringRes val errorMessage: Int? = null,
)

sealed interface HomeAction {
    data object LoadMore : HomeAction
    data object Refresh : HomeAction
    data class PlayProgression(val progression: ChordProgression) : HomeAction
    data object DismissError : HomeAction
}
