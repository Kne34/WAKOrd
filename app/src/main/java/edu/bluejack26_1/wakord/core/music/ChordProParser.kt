package edu.bluejack26_1.wakord.core.music

import edu.bluejack26_1.wakord.domain.model.ChordSegment
import edu.bluejack26_1.wakord.domain.model.SongLine

object ChordProParser {
    private val TOKEN = Regex("\\[([^\\]]+)]")

    fun parseLine(line: String): SongLine {
        val segments = mutableListOf<ChordSegment>()
        var chord: String? = null
        var cursor = 0
        TOKEN.findAll(line).forEach { match ->
            val lyric = line.substring(cursor, match.range.first)
            if (lyric.isNotEmpty() || chord != null) segments += ChordSegment(chord, lyric)
            chord = match.groupValues[1].trim()
            cursor = match.range.last + 1
        }
        val rest = line.substring(cursor)
        if (rest.isNotEmpty() || chord != null) segments += ChordSegment(chord, rest)
        return SongLine(segments)
    }

    fun toChordPro(line: SongLine): String =
        line.segments.joinToString("") { (it.chord?.let { c -> "[$c]" } ?: "") + it.lyric }
}
