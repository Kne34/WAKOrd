package edu.bluejack26_1.wakord.core.music

object DiatonicChords {
    private val MAJOR_STEPS = listOf(0, 2, 4, 5, 7, 9, 11)
    private val MAJOR_QUALITIES = listOf("", "m", "m", "", "", "m", "dim")
    private val MINOR_STEPS = listOf(0, 2, 3, 5, 7, 8, 10)
    private val MINOR_QUALITIES = listOf("m", "dim", "", "m", "m", "", "")
    private val SEVENTHS = mapOf("" to "maj7", "m" to "m7", "dim" to "m7b5")

    val KEYS = listOf("C", "Db", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B") +
        listOf("Am", "Bbm", "Bm", "Cm", "C#m", "Dm", "Ebm", "Em", "Fm", "F#m", "Gm", "G#m")

    fun of(key: String, withSevenths: Boolean = true): List<String> {
        val chord = Chord.parse(key) ?: return emptyList()
        val minor = chord.suffix.startsWith("m")
        val tonic = NoteNames.pitchClass(chord.root)
        val flats = NoteNames.prefersFlats(key) || chord.root.contains('b')
        val steps = if (minor) MINOR_STEPS else MAJOR_STEPS
        val qualities = if (minor) MINOR_QUALITIES else MAJOR_QUALITIES
        val triads = steps.mapIndexed { i, step -> NoteNames.name(tonic + step, flats) + qualities[i] }
        if (!withSevenths) return triads
        val sevenths = steps.mapIndexed { i, step ->
            val root = NoteNames.name(tonic + step, flats)
            val isDominant = (!minor && i == 4) || (minor && i == 6)
            root + if (isDominant) "7" else SEVENTHS.getValue(qualities[i])
        }
        return triads + sevenths
    }
}
