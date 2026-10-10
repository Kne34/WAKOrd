package edu.bluejack26_1.wakord.core.analysis

import edu.bluejack26_1.wakord.core.music.Chord
import edu.bluejack26_1.wakord.core.music.ChordTheory
import edu.bluejack26_1.wakord.core.music.NoteNames
import edu.bluejack26_1.wakord.domain.model.AnalysisRecommendations
import edu.bluejack26_1.wakord.domain.model.AnalysisResult
import edu.bluejack26_1.wakord.domain.model.Catalog
import edu.bluejack26_1.wakord.domain.model.ChordTrackBar
import edu.bluejack26_1.wakord.domain.model.Difficulty
import edu.bluejack26_1.wakord.domain.model.Song
import kotlin.math.roundToInt

object RomanNumerals {
    private val MAJOR = listOf("I", "bII", "II", "bIII", "III", "IV", "#IV", "V", "bVI", "VI", "bVII", "VII")
    private val MINOR = listOf("I", "bII", "II", "III", "#III", "IV", "#IV", "V", "VI", "#VI", "VII", "#VII")

    fun of(symbol: String, key: String): String {
        val chord = Chord.parse(symbol) ?: return symbol
        val keyChord = Chord.parse(key) ?: return symbol
        val minorKey = keyChord.suffix.startsWith("m")
        val degree = Math.floorMod(NoteNames.pitchClass(chord.root) - NoteNames.pitchClass(keyChord.root), 12)
        val numeral = (if (minorKey) MINOR else MAJOR)[degree]
        val intervals = ChordTheory.intervals(chord.suffix)
        val isMinor = 3 in intervals && 4 !in intervals
        val isDiminished = isMinor && 6 in intervals && 7 !in intervals
        val base = if (isMinor) numeral.lowercase() else numeral
        val extension = when {
            isDiminished && 10 in intervals -> "ø7"
            isDiminished -> "°"
            chord.suffix.contains("maj7") -> "maj7"
            chord.suffix.contains("7") -> "7"
            chord.suffix.contains("9") -> "9"
            else -> ""
        }
        return base + extension
    }
}

object AnalysisInsights {
    private const val BEATS_PER_BAR = 4

    fun chordTrack(result: AnalysisResult): List<ChordTrackBar> {
        val barMs = 60_000.0 * BEATS_PER_BAR / result.bpm.coerceAtLeast(1)
        return result.chords.flatMap { chord ->
            val bars = ((chord.endMs - chord.startMs) / barMs).roundToInt().coerceAtLeast(1)
            List(bars) { ChordTrackBar(chord.symbol, RomanNumerals.of(chord.symbol, result.key)) }
        }
    }

    fun difficulty(chords: List<String>): Difficulty {
        val sizes = chords.mapNotNull { Chord.parse(it) }.map { ChordTheory.intervals(it.suffix).size }
        return when {
            sizes.any { it >= 5 } || chords.distinct().size > 6 -> Difficulty.ADVANCED
            sizes.any { it == 4 } -> Difficulty.INTERMEDIATE
            else -> Difficulty.EASY
        }
    }

    fun recommend(result: AnalysisResult, catalog: Catalog, limit: Int = 5): AnalysisRecommendations {
        val detected = identities(result.uniqueChords)
        if (detected.isEmpty()) return AnalysisRecommendations()
        val songs = catalog.songs
            .map { it to overlap(detected, identities(songChords(it))) + if (it.key == result.key) 0.5 else 0.0 }
            .filter { it.second > 0.5 }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(limit)
        val progressions = catalog.progressions
            .map { it to overlap(detected, identities(it.chords)) + if (it.key == result.key) 0.5 else 0.0 }
            .filter { it.second > 0.5 }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(limit)
        return AnalysisRecommendations(
            songs = songs,
            artists = songs.map { it.artist }.distinct().take(limit),
            progressions = progressions,
        )
    }

    fun songChords(song: Song): List<String> =
        song.sections.flatMap { s -> s.lines.flatMap { l -> l.segments.mapNotNull { it.chord } } }

    private fun identities(symbols: List<String>): Set<Pair<Int, Set<Int>>> = symbols.mapNotNull { Chord.parse(it) }
        .map { NoteNames.pitchClass(it.root) to ChordTheory.intervals(it.suffix).take(3).toSet() }
        .toSet()

    private fun overlap(a: Set<Pair<Int, Set<Int>>>, b: Set<Pair<Int, Set<Int>>>): Double =
        if (a.isEmpty() || b.isEmpty()) 0.0 else (a intersect b).size.toDouble() / a.size * 3
}
