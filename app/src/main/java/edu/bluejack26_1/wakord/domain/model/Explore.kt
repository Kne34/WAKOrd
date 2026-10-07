package edu.bluejack26_1.wakord.domain.model

data class ExploreFilter(val genre: String? = null, val difficulty: Difficulty? = null)

data class LyricMatch(val song: Song, val snippet: String)

data class ArtistResult(val name: String, val songCount: Int, val genres: List<String>)

data class SearchResults(
    val songs: List<Song> = emptyList(),
    val lyricMatches: List<LyricMatch> = emptyList(),
    val chords: List<String> = emptyList(),
    val artists: List<ArtistResult> = emptyList(),
    val progressions: List<ChordProgression> = emptyList(),
) {
    val isEmpty: Boolean get() = songs.isEmpty() && lyricMatches.isEmpty() && chords.isEmpty() && artists.isEmpty() && progressions.isEmpty()
}

data class Catalog(val songs: List<Song>, val progressions: List<ChordProgression>)
