package edu.bluejack26_1.wakord.ui.feature.explore

import androidx.annotation.StringRes
import edu.bluejack26_1.wakord.domain.model.ChordProgression
import edu.bluejack26_1.wakord.domain.model.Difficulty
import edu.bluejack26_1.wakord.domain.model.SearchResults
import edu.bluejack26_1.wakord.domain.model.Song
import edu.bluejack26_1.wakord.ui.feature.auth.DefaultGenres

enum class SearchTab { SONGS, CHORDS, ARTISTS }

data class ExploreUiState(
    val query: String = "",
    val genres: List<String> = DefaultGenres,
    val selectedGenre: String? = null,
    val selectedLevel: Difficulty? = null,
    val trendingSongs: List<Song> = emptyList(),
    val trendingProgressions: List<ChordProgression> = emptyList(),
    val topThisWeek: List<Song> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val endReached: Boolean = false,
    val isSearching: Boolean = false,
    val results: SearchResults = SearchResults(),
    val selectedTab: SearchTab = SearchTab.SONGS,
    @param:StringRes val loadError: Int? = null,
    @param:StringRes val errorMessage: Int? = null,
) {
    val showResults: Boolean get() = query.isNotBlank()
    val songCount: Int get() = results.songs.size + results.lyricMatches.size
    val chordCount: Int get() = results.chords.size
    val artistCount: Int get() = results.artists.size
}

sealed interface ExploreAction {
    data class QueryChanged(val query: String) : ExploreAction
    data object ClearQuery : ExploreAction
    data class SelectGenre(val genre: String?) : ExploreAction
    data class SelectLevel(val level: Difficulty?) : ExploreAction
    data class SelectTab(val tab: SearchTab) : ExploreAction
    data object LoadMore : ExploreAction
    data object Refresh : ExploreAction
    data object DismissError : ExploreAction
}
