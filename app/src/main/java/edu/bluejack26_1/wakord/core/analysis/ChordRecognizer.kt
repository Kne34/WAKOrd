package edu.bluejack26_1.wakord.core.analysis

import edu.bluejack26_1.wakord.core.music.ChordTheory
import edu.bluejack26_1.wakord.core.music.NoteNames
import edu.bluejack26_1.wakord.domain.model.LiveChordReading
import kotlin.math.roundToInt
import kotlin.math.sqrt

object ChordRecognizer {
    private const val SILENCE_RMS = 0.01f
    private const val COMPLEXITY_PENALTY = 0.04f
    private const val TIE = 0.001f
    const val MIN_BUILT_NOTES = 3
    private val SUFFIXES = listOf("", "m", "7", "maj7", "m7", "dim", "aug", "sus2", "sus4", "m7b5", "6", "m6", "dim7", "add9")
    private val INTERVAL_NAMES = listOf("R", "m2", "M2", "m3", "M3", "P4", "d5", "P5", "A5", "M6", "m7", "M7")

    data class Match(val root: Int, val suffix: String, val score: Float) {
        val symbol: String get() = NoteNames.name(root, preferFlats = root in FLAT_ROOTS) + suffix
    }

    private val FLAT_ROOTS = setOf(1, 3, 8, 10)

    private val templates: List<Triple<Int, String, FloatArray>> = (0 until 12).flatMap { root ->
        SUFFIXES.map { suffix ->
            val t = FloatArray(12)
            ChordTheory.intervals(suffix).forEach { t[(root + it) % 12] = 1f }
            Triple(root, suffix, t)
        }
    }

    fun rank(chroma: FloatArray): List<Match> = templates
        .map { (root, suffix, template) ->
            val notes = template.count { it > 0f }
            Match(root, suffix, cosine(chroma, template) - COMPLEXITY_PENALTY * (notes - 3).coerceAtLeast(0))
        }
        .sortedByDescending { it.score }

    fun best(chroma: FloatArray): Match? = rank(chroma).firstOrNull()?.takeIf { it.score > 0f }

    fun recognize(frame: FloatArray, sampleRate: Int): LiveChordReading? {
        if (Chromagram.rms(frame) < SILENCE_RMS) return null
        val ranked = rank(Chromagram.chroma(frame, sampleRate))
        val best = ranked.first()
        val alternative = ranked.drop(1).firstOrNull { it.symbol != best.symbol }
        return reading(best, alternative)
    }

    fun fromNotes(midis: Collection<Int>): LiveChordReading? {
        val pitchClasses = midis.map { Math.floorMod(it, 12) }.toSet()
        if (pitchClasses.size < MIN_BUILT_NOTES) return null
        val chroma = FloatArray(12).also { c -> pitchClasses.forEach { c[it] = 1f } }
        val bass = Math.floorMod(midis.min(), 12)
        val ranked = rank(chroma)
        val top = ranked.first().score
        val best = ranked.firstOrNull { it.score >= top - TIE && it.root == bass } ?: ranked.first()
        return reading(best, ranked.firstOrNull { it.symbol != best.symbol })
    }

    fun reading(best: Match, alternative: Match?): LiveChordReading {
        val intervals = ChordTheory.intervals(best.suffix)
        return LiveChordReading(
            symbol = best.symbol,
            root = NoteNames.name(best.root, preferFlats = best.root in FLAT_ROOTS),
            quality = quality(best.suffix),
            extensions = extensions(best.suffix),
            notes = intervals.map { NoteNames.name(best.root + it, preferFlats = best.root in FLAT_ROOTS) },
            intervals = intervals.map { if (it == 2 && best.suffix.contains("9")) "9" else INTERVAL_NAMES[it] },
            matchPercent = (best.score.coerceIn(0f, 1f) * 100).roundToInt(),
            alternative = alternative?.symbol,
        )
    }

    private fun quality(suffix: String): String = when {
        suffix.startsWith("dim") || suffix == "m7b5" -> "diminished"
        suffix == "aug" -> "augmented"
        suffix.startsWith("sus") -> "suspended"
        suffix.startsWith("m") && !suffix.startsWith("maj") -> "minor"
        else -> "major"
    }

    private fun extensions(suffix: String): List<String> = when (suffix) {
        "7", "m7", "dim7" -> listOf("7")
        "maj7" -> listOf("maj7")
        "m7b5" -> listOf("7", "b5")
        "6", "m6" -> listOf("6")
        "add9" -> listOf("9")
        else -> emptyList()
    }

    private fun cosine(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        var na = 0f
        var nb = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            na += a[i] * a[i]
            nb += b[i] * b[i]
        }
        return if (na == 0f || nb == 0f) 0f else dot / (sqrt(na) * sqrt(nb))
    }
}
