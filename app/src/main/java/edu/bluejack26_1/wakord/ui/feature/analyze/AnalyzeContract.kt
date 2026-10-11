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

data class LiveChordUiState(
    val isListening: Boolean = false,
    val reading: LiveChordReading? = null,
    val microphoneOff: Boolean = false,
)

sealed interface LiveChordAction {
    data object ToggleListening : LiveChordAction
    data object MicrophoneDenied : LiveChordAction
}
