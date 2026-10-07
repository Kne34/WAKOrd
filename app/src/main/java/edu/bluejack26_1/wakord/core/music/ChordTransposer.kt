package edu.bluejack26_1.wakord.core.music

import edu.bluejack26_1.wakord.domain.model.Song

object ChordTransposer {
    fun transposeChord(symbol: String, semitones: Int, preferFlats: Boolean): String {
        val chord = Chord.parse(symbol) ?: return symbol
        fun shift(note: String) = NoteNames.name(NoteNames.pitchClass(note) + semitones, preferFlats)
        return Chord(shift(chord.root), chord.suffix, chord.bass?.let(::shift)).symbol
    }

    fun transposeKey(key: String, semitones: Int): String {
        if (Math.floorMod(semitones, 12) == 0) return key
        val normalized = NoteNames.normalize(key)
        val minor = normalized.endsWith("m")
        val pitchClass = NoteNames.pitchClass(normalized.removeSuffix("m")) + semitones
        val suffix = if (minor) "m" else ""
        val flatName = NoteNames.name(pitchClass, preferFlats = true) + suffix
        return if (NoteNames.prefersFlats(flatName)) flatName else NoteNames.name(pitchClass, preferFlats = false) + suffix
    }

    fun transposeSong(song: Song, semitones: Int): Song {
        if (Math.floorMod(semitones, 12) == 0) return song
        val newKey = transposeKey(song.key, semitones)
        val preferFlats = NoteNames.prefersFlats(newKey)
        return song.copy(
            key = newKey,
            sections = song.sections.map { section ->
                section.copy(
                    lines = section.lines.map { line ->
                        line.copy(segments = line.segments.map { it.copy(chord = it.chord?.let { c -> transposeChord(c, semitones, preferFlats) }) })
                    },
                )
            },
        )
    }
}
