package edu.bluejack26_1.wakord.core.discover

import edu.bluejack26_1.wakord.core.music.Chord
import edu.bluejack26_1.wakord.core.music.NoteNames
import edu.bluejack26_1.wakord.domain.model.ArtistResult
import edu.bluejack26_1.wakord.domain.model.Catalog
import edu.bluejack26_1.wakord.domain.model.ChordProgression
import edu.bluejack26_1.wakord.domain.model.ExploreFilter
import edu.bluejack26_1.wakord.domain.model.LyricMatch
import edu.bluejack26_1.wakord.domain.model.SearchResults
import edu.bluejack26_1.wakord.domain.model.Song
import java.text.Normalizer
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.IsoFields

object SearchEngine {
    private const val SNIPPET_RADIUS = 24

    fun search(query: String, catalog: Catalog, filter: ExploreFilter = ExploreFilter()): SearchResults {
        val q = fold(query)
        if (q.isEmpty()) return SearchResults()
        val songs = applyFilter(catalog.songs, filter)
        val progressions = applyFilter(catalog.progressions, filter)

        val titleOrArtist = songs.filter { fold(it.title).contains(q) || fold(it.artist).contains(q) }
        val lyricMatches = songs
            .filter { it !in titleOrArtist }
            .mapNotNull { song -> lyricSnippet(song, q)?.let { LyricMatch(song, it) } }

        return SearchResults(
            songs = titleOrArtist.sortedByDescending { it.likeCount },
            lyricMatches = lyricMatches,
            chords = matchingChords(query, songs, progressions),
            artists = songs
                .groupBy { it.artist }
                .filterKeys { fold(it).contains(q) }
                .map { (name, list) -> ArtistResult(name, list.size, list.map { it.genre }.distinct()) }
                .sortedByDescending { it.songCount },
            progressions = progressions.filter { p ->
                fold(p.title).contains(q) || p.chords.any { fold(it).startsWith(q) }
            },
        )
    }

    fun applyFilter(songs: List<Song>, filter: ExploreFilter): List<Song> = songs.filter {
        (filter.genre == null || it.genre.equals(filter.genre, ignoreCase = true)) &&
            (filter.difficulty == null || it.difficulty == filter.difficulty)
    }

    @JvmName("applyProgressionFilter")
    fun applyFilter(progressions: List<ChordProgression>, filter: ExploreFilter): List<ChordProgression> = progressions.filter {
        (filter.genre == null || it.genre.equals(filter.genre, ignoreCase = true)) &&
            (filter.difficulty == null || it.difficulty == filter.difficulty)
    }

    fun weekKey(epochMillis: Long): String {
        val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC).toLocalDate()
        val week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        return "%d-W%02d".format(date.get(IsoFields.WEEK_BASED_YEAR), week)
    }

    private fun matchingChords(query: String, songs: List<Song>, progressions: List<ChordProgression>): List<String> {
        val raw = NoteNames.normalize(query.trim()).replaceFirstChar { it.uppercaseChar() }
        val parsed = Chord.parse(raw) ?: return emptyList()
        val queryRoot = NoteNames.pitchClass(parsed.root)
        val all = songs.flatMap { song -> song.sections.flatMap { s -> s.lines.flatMap { l -> l.segments.mapNotNull { it.chord } } } } +
            progressions.flatMap { it.chords }
        return all
            .mapNotNull { Chord.parse(it) }
            .filter { NoteNames.pitchClass(it.root) == queryRoot && it.suffix.startsWith(parsed.suffix) }
            .map { it.symbol }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key.length })
            .map { it.key }
    }

    private fun lyricSnippet(song: Song, q: String): String? {
        val lyrics = song.sections.flatMap { s -> s.lines.map { line -> line.segments.joinToString("") { it.lyric }.trim() } }
        val line = lyrics.firstOrNull { fold(it).contains(q) } ?: return null
        val at = fold(line).indexOf(q)
        val start = (at - SNIPPET_RADIUS).coerceAtLeast(0)
        val end = (at + q.length + SNIPPET_RADIUS).coerceAtMost(line.length)
        return (if (start > 0) "…" else "") + line.substring(start, end) + (if (end < line.length) "…" else "")
    }

    private fun fold(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase().trim()
}
