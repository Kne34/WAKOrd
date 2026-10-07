package edu.bluejack26_1.wakord.core.music

import edu.bluejack26_1.wakord.domain.model.ChordVoicing

object VoicingGenerator {
    val GUITAR_STANDARD = listOf(40, 45, 50, 55, 59, 64)
    val UKULELE_STANDARD = listOf(67, 60, 64, 69)

    private const val WINDOW = 3
    private const val MAX_POSITION = 10

    fun voicings(symbol: String, tuning: List<Int> = GUITAR_STANDARD, limit: Int = 3): List<ChordVoicing> {
        val chord = Chord.parse(symbol) ?: return emptyList()
        val rootPc = NoteNames.pitchClass(chord.root)
        val bassPc = chord.bass?.let(NoteNames::pitchClass) ?: rootPc
        val intervals = ChordTheory.intervals(chord.suffix)
        val tones = ChordTheory.pitchClasses(chord)
        val required = (if (intervals.size >= 4) tones - ((rootPc + 7) % 12) else tones) + bassPc
        val minSounding = if (tuning.size >= 6) 4 else tuning.size - 1

        val found = (0..MAX_POSITION).mapNotNull { position ->
            bestAt(position, tuning, tones, required, bassPc, minSounding)
        }
        val unique = found.distinctBy { it.second.frets }
        val open = unique.firstOrNull { it.second.position == 0 }
        val others = unique.filter { it !== open }.sortedByDescending { it.first }
        return (listOfNotNull(open) + others)
            .take(limit)
            .map { it.second }
            .sortedBy { it.position }
    }

    fun midiNotes(voicing: ChordVoicing, tuning: List<Int> = GUITAR_STANDARD): List<Int> =
        voicing.frets.mapIndexedNotNull { string, fret -> if (fret < 0) null else tuning[string] + fret }

    private fun bestAt(
        position: Int,
        tuning: List<Int>,
        tones: Set<Int>,
        required: Set<Int>,
        bassPc: Int,
        minSounding: Int,
    ): Pair<Int, ChordVoicing>? {
        val options = tuning.map { open ->
            val frets = if (position == 0) 0..WINDOW else position..position + WINDOW
            listOf(-1) + frets.filter { (open + it) % 12 in tones }
        }
        var best: Pair<Int, ChordVoicing>? = null
        val current = IntArray(tuning.size)

        fun search(string: Int, sounding: Boolean) {
            if (string == tuning.size) {
                val candidate = evaluate(current.toList(), position, tuning, required, bassPc, minSounding) ?: return
                if (best == null || candidate.first > best!!.first) best = candidate
                return
            }
            for (fret in options[string]) {
                if (fret == -1 && sounding) continue
                current[string] = fret
                search(string + 1, sounding || fret >= 0)
            }
        }
        search(0, false)
        return best
    }

    private fun evaluate(
        frets: List<Int>,
        position: Int,
        tuning: List<Int>,
        required: Set<Int>,
        bassPc: Int,
        minSounding: Int,
    ): Pair<Int, ChordVoicing>? {
        val notes = frets.mapIndexedNotNull { s, f -> if (f < 0) null else tuning[s] + f }
        if (notes.size < minSounding) return null
        if (notes.min() % 12 != bassPc) return null
        if (!notes.map { it % 12 }.toSet().containsAll(required)) return null
        val fingers = fingersFor(frets) ?: return null

        val fretted = frets.filter { it > 0 }
        val span = if (fretted.isEmpty()) 0 else fretted.max() - fretted.min()
        val openStrings = frets.count { it == 0 }
        val fingerCount = fingers.maxOrNull() ?: 0
        val score = notes.size * 10 - fingerCount * 3 - span * 2 + openStrings
        val maxFret = frets.maxOrNull() ?: 0
        val baseFret = if (maxFret <= 4) 1 else fretted.min()
        return score to ChordVoicing(frets, fingers, baseFret, position)
    }

    private fun fingersFor(frets: List<Int>): List<Int>? {
        val fretted = frets.withIndex().filter { it.value > 0 }
        val fingers = MutableList(frets.size) { 0 }
        if (fretted.isEmpty()) return fingers
        val lowest = fretted.minOf { it.value }
        val barre = fretted.filter { it.value == lowest }
        var next = 1
        if (fretted.size > 4 && barre.size >= 2) {
            val from = barre.first().index
            val to = barre.last().index
            if ((from..to).any { frets[it] == 0 }) return null
            barre.forEach { fingers[it.index] = 1 }
            next = 2
        }
        fretted.filter { fingers[it.index] == 0 }
            .sortedWith(compareBy({ it.value }, { it.index }))
            .forEach { fingers[it.index] = next++ }
        return if (next - 1 > 4) null else fingers
    }
}
