package edu.bluejack26_1.wakord.ui.feature.analyze

import androidx.annotation.StringRes
import edu.bluejack26_1.wakord.domain.model.AnalysisRecommendations
import edu.bluejack26_1.wakord.domain.model.AnalysisResult
import edu.bluejack26_1.wakord.domain.model.AnalysisSource
import edu.bluejack26_1.wakord.domain.model.AnalysisStage
import edu.bluejack26_1.wakord.domain.model.ChordTrackBar
import edu.bluejack26_1.wakord.domain.model.LiveChordReading

const val MAX_RECORDING_MS = 30_000L

data class AnalyzeUiState(
    val isRecording: Boolean = false,
    val elapsedMs: Long = 0,
    val maxMs: Long = MAX_RECORDING_MS,
    val inputLevel: Float = 0f,
    val stage: AnalysisStage? = null,
    val result: AnalysisResult? = null,
    val recommendations: AnalysisRecommendations = AnalysisRecommendations(),
    val chordTrack: List<ChordTrackBar> = emptyList(),
    val isPlayingResult: Boolean = false,
    val playingIndex: Int? = null,
    val history: List<AnalysisResult> = emptyList(),
    val microphoneOff: Boolean = false,
    val savedProgressionId: String? = null,
    val isSaving: Boolean = false,
    @param:StringRes val errorMessage: Int? = null,
) {
    val isAnalyzing: Boolean get() = stage != null
    val usedFallback: Boolean get() = result?.source == AnalysisSource.ALGORITHM
}

sealed interface AnalyzeAction {
    data object ToggleRecording : AnalyzeAction
    data object CancelAnalysis : AnalyzeAction
    data class OpenHistory(val result: AnalysisResult) : AnalyzeAction
    data object PlayResult : AnalyzeAction
    data class AddBar(val symbol: String) : AnalyzeAction
    data class RemoveBar(val index: Int) : AnalyzeAction
    data class SaveAsProgression(val title: String) : AnalyzeAction
    data object CloseResult : AnalyzeAction
    data object MicrophoneDenied : AnalyzeAction
    data object DismissError : AnalyzeAction
}

enum class AnalyzeMode { Record, Live }

const val MAX_TRACK_BARS = 8
const val DEFAULT_TRACK_BPM = 92
const val MIN_TRACK_BPM = 40
const val MAX_TRACK_BPM = 200

data class LiveChordUiState(
    val isListening: Boolean = false,
    val inputLevel: Float = 0f,
    val reading: LiveChordReading? = null,
    val lockedReading: LiveChordReading? = null,
    val isBuilding: Boolean = false,
    val builtNotes: Set<Int> = emptySet(),
    val builtReading: LiveChordReading? = null,
    val microphoneOff: Boolean = false,
    val bars: List<ChordTrackBar> = emptyList(),
    val trackKey: String? = null,
    val bpm: Int = DEFAULT_TRACK_BPM,
    val isPlayingTrack: Boolean = false,
    val isLooping: Boolean = false,
    val playingBar: Int? = null,
    val isSaving: Boolean = false,
    val savedProgressionId: String? = null,
    @param:StringRes val errorMessage: Int? = null,
) {
    val canAddBar: Boolean get() = bars.size < MAX_TRACK_BARS
    val isLocked: Boolean get() = lockedReading != null
    val shownReading: LiveChordReading? get() = if (isBuilding) builtReading else lockedReading ?: reading
}

sealed interface LiveChordAction {
    data object ToggleListening : LiveChordAction
    data object StartListening : LiveChordAction
    data object StopListening : LiveChordAction
    data object MicrophoneDenied : LiveChordAction
    data object ToggleLock : LiveChordAction
    data object StartBuilding : LiveChordAction
    data class ToggleKey(val midi: Int) : LiveChordAction
    data object ClearKeys : LiveChordAction
    data object FinishBuilding : LiveChordAction
    data class AddBar(val symbol: String) : LiveChordAction
    data class RemoveBar(val index: Int) : LiveChordAction
    data class SetBpm(val bpm: Int) : LiveChordAction
    data object PlayTrack : LiveChordAction
    data object ToggleLoop : LiveChordAction
    data class SaveAsProgression(val title: String) : LiveChordAction
    data object DismissError : LiveChordAction
}
