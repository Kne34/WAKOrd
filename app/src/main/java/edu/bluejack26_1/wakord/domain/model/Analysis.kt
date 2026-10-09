package edu.bluejack26_1.wakord.domain.model

enum class AnalysisSource { AI, ALGORITHM }

enum class AnalysisStage { CLEANING_NOISE, DETECTING_CHORDS, MATCHING_SONGS }

data class DetectedChord(val symbol: String, val startMs: Long, val endMs: Long)

data class AnalysisResult(
    val id: String,
    val createdAt: Long,
    val durationMs: Long,
    val key: String,
    val confidence: Float,
    val bpm: Int,
    val chords: List<DetectedChord>,
    val source: AnalysisSource,
) {
    val uniqueChords: List<String> get() = chords.map { it.symbol }.distinct()
}

data class ChordTrackBar(val symbol: String, val roman: String)

data class AnalysisRecommendations(
    val songs: List<Song> = emptyList(),
    val artists: List<String> = emptyList(),
    val progressions: List<ChordProgression> = emptyList(),
)

data class LiveChordReading(
    val symbol: String,
    val root: String,
    val quality: String,
    val extensions: List<String>,
    val notes: List<String>,
    val intervals: List<String>,
    val matchPercent: Int,
    val alternative: String?,
)

data class ProgressionDraft(
    val title: String,
    val key: String,
    val bpm: Int,
    val genre: String,
    val difficulty: Difficulty,
    val chords: List<String>,
    val description: String = "",
)
