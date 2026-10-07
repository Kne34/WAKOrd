package edu.bluejack26_1.wakord.core.music

object NoteNames {
    private val SHARPS = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    private val FLATS = listOf("C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B")
    private val BASE = mapOf('C' to 0, 'D' to 2, 'E' to 4, 'F' to 5, 'G' to 7, 'A' to 9, 'B' to 11)
    private val FLAT_KEYS = setOf("F", "Bb", "Eb", "Ab", "Db", "Gb", "Dm", "Gm", "Cm", "Fm", "Bbm", "Ebm")

    fun normalize(text: String): String = text.replace('♯', '#').replace('♭', 'b')

    fun pitchClass(note: String): Int {
        val n = normalize(note)
        val base = BASE[n.first().uppercaseChar()] ?: error("Bukan nama nada: $note")
        val accidental = n.drop(1).sumOf {
            when (it) {
                '#' -> 1
                'b' -> -1
                else -> 0
            }
        }
        return Math.floorMod(base + accidental, 12)
    }

    fun name(pitchClass: Int, preferFlats: Boolean): String =
        (if (preferFlats) FLATS else SHARPS)[Math.floorMod(pitchClass, 12)]

    fun prefersFlats(key: String): Boolean = normalize(key) in FLAT_KEYS

    fun pretty(symbol: String): String =
        Regex("([A-G])([#b])").replace(symbol) { m -> m.groupValues[1] + if (m.groupValues[2] == "#") "♯" else "♭" }
}

data class Chord(val root: String, val suffix: String = "", val bass: String? = null) {
    val symbol: String get() = root + suffix + (bass?.let { "/$it" } ?: "")

    companion object {
        private val REGEX = Regex("^([A-G][#b]?)([^/]*)(?:/([A-G][#b]?))?$")

        fun parse(symbol: String): Chord? {
            val match = REGEX.matchEntire(NoteNames.normalize(symbol.trim())) ?: return null
            val (root, suffix, bass) = match.destructured
            return Chord(root, suffix, bass.ifEmpty { null })
        }
    }
}

object ChordTheory {
    private val MAJOR = listOf(0, 4, 7)
    private val MINOR = listOf(0, 3, 7)
    private val TABLE: Map<String, List<Int>> = mapOf(
        "" to MAJOR, "maj" to MAJOR, "M" to MAJOR,
        "m" to MINOR, "min" to MINOR, "-" to MINOR,
        "5" to listOf(0, 7),
        "6" to listOf(0, 4, 7, 9), "m6" to listOf(0, 3, 7, 9),
        "7" to listOf(0, 4, 7, 10), "maj7" to listOf(0, 4, 7, 11), "M7" to listOf(0, 4, 7, 11),
        "m7" to listOf(0, 3, 7, 10), "m(maj7)" to listOf(0, 3, 7, 11),
        "dim" to listOf(0, 3, 6), "dim7" to listOf(0, 3, 6, 9), "m7b5" to listOf(0, 3, 6, 10),
        "aug" to listOf(0, 4, 8), "+" to listOf(0, 4, 8),
        "sus2" to listOf(0, 2, 7), "sus4" to listOf(0, 5, 7), "sus" to listOf(0, 5, 7),
        "7sus4" to listOf(0, 5, 7, 10),
        "add9" to listOf(0, 4, 7, 2), "madd9" to listOf(0, 3, 7, 2),
        "9" to listOf(0, 4, 7, 10, 2), "maj9" to listOf(0, 4, 7, 11, 2),
        "m9" to listOf(0, 3, 7, 10, 2), "m7(9)" to listOf(0, 3, 7, 10, 2),
        "11" to listOf(0, 4, 7, 10, 5), "m11" to listOf(0, 3, 7, 10, 5),
        "13" to listOf(0, 4, 10, 9), "maj13" to listOf(0, 4, 11, 9), "m13" to listOf(0, 3, 10, 9),
    )

    fun intervals(suffix: String): List<Int> = TABLE[suffix]
        ?: if (suffix.startsWith("m") && !suffix.startsWith("maj")) MINOR else MAJOR

    fun pitchClasses(chord: Chord): Set<Int> {
        val root = NoteNames.pitchClass(chord.root)
        val tones = intervals(chord.suffix).map { (root + it) % 12 }.toMutableSet()
        chord.bass?.let { tones += NoteNames.pitchClass(it) }
        return tones
    }
}
