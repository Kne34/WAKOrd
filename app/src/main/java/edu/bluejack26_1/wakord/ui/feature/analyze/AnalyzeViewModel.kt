package edu.bluejack26_1.wakord.ui.feature.analyze

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bluejack26_1.wakord.WAKordApp
import edu.bluejack26_1.wakord.core.analysis.AnalysisInsights
import edu.bluejack26_1.wakord.core.analysis.AudioCleaner
import edu.bluejack26_1.wakord.core.analysis.Chromagram
import edu.bluejack26_1.wakord.core.analysis.RomanNumerals
import edu.bluejack26_1.wakord.core.audio.AudioCapture
import edu.bluejack26_1.wakord.core.audio.TonePlayer
import edu.bluejack26_1.wakord.core.error.AppError
import edu.bluejack26_1.wakord.core.error.messageRes
import edu.bluejack26_1.wakord.core.music.VoicingGenerator
import edu.bluejack26_1.wakord.domain.model.AnalysisResult
import edu.bluejack26_1.wakord.domain.model.AnalysisStage
import edu.bluejack26_1.wakord.domain.model.ChordTrackBar
import edu.bluejack26_1.wakord.domain.model.ProgressionDraft
import edu.bluejack26_1.wakord.domain.repository.AnalysisRepository
import edu.bluejack26_1.wakord.domain.repository.ChordAnalyzer
import edu.bluejack26_1.wakord.domain.repository.ExploreRepository
import edu.bluejack26_1.wakord.domain.repository.ProgressionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnalyzeViewModel(
    private val audioCapture: AudioCapture,
    private val chordAnalyzer: ChordAnalyzer,
    private val analysisRepository: AnalysisRepository,
    private val exploreRepository: ExploreRepository,
    private val progressionRepository: ProgressionRepository,
    private val tonePlayer: TonePlayer,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AnalyzeUiState())
    val uiState: StateFlow<AnalyzeUiState> = _uiState.asStateFlow()

    private var recordJob: Job? = null
    private var playJob: Job? = null
    @Volatile private var stopRequested = false

    init {
        viewModelScope.launch { analysisRepository.history.collect { h -> _uiState.update { it.copy(history = h) } } }
        viewModelScope.launch { analysisRepository.syncPending() }
    }

    fun onAction(action: AnalyzeAction) {
        when (action) {
            AnalyzeAction.ToggleRecording -> if (_uiState.value.isRecording) stopRequested = true else startRecording()
            AnalyzeAction.CancelAnalysis -> cancelAnalysis()
            is AnalyzeAction.OpenHistory -> viewModelScope.launch { show(action.result) }
            AnalyzeAction.PlayResult -> if (_uiState.value.isPlayingResult) stopPlayback() else playResult()
            is AnalyzeAction.AddBar -> addBar(action.symbol)
            is AnalyzeAction.RemoveBar -> _uiState.update { s ->
                s.copy(chordTrack = s.chordTrack.filterIndexed { i, _ -> i != action.index })
            }
            is AnalyzeAction.SaveAsProgression -> saveAsProgression(action.title)
            AnalyzeAction.CloseResult -> {
                stopPlayback()
                _uiState.update { it.copy(result = null, chordTrack = emptyList(), savedProgressionId = null) }
            }
            AnalyzeAction.MicrophoneDenied -> _uiState.update { it.copy(microphoneOff = true, isRecording = false) }
            AnalyzeAction.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun startRecording() {
        if (recordJob?.isActive == true || _uiState.value.isAnalyzing) return
        stopRequested = false
        stopPlayback()
        val rate = audioCapture.sampleRate
        val maxSamples = (rate * MAX_RECORDING_MS / 1000).toInt()
        val buffer = FloatArray(maxSamples)
        var recorded = 0
        _uiState.update { it.copy(isRecording = true, elapsedMs = 0, inputLevel = 0f, result = null, microphoneOff = false, savedProgressionId = null) }
        recordJob = viewModelScope.launch {
            var failure: Throwable? = null
            audioCapture.frames(FRAME_SIZE)
                .takeWhile { !stopRequested && recorded < maxSamples }
                .catch { failure = it }
                .collect { frame ->
                    val count = minOf(frame.size, maxSamples - recorded)
                    frame.copyInto(buffer, recorded, 0, count)
                    recorded += count
                    _uiState.update { it.copy(elapsedMs = recorded * 1000L / rate, inputLevel = Chromagram.rms(frame)) }
                }
            _uiState.update { it.copy(isRecording = false, inputLevel = 0f) }
            when {
                failure is AppError.MicrophoneOff -> _uiState.update { it.copy(microphoneOff = true) }
                failure != null -> _uiState.update { it.copy(errorMessage = failure?.messageRes()) }
                recorded < rate -> _uiState.update { it.copy(errorMessage = AppError.NoChordsHeard.messageRes()) }
                else -> analyze(buffer.copyOf(recorded), rate)
            }
        }
    }

    private fun cancelAnalysis() {
        recordJob?.cancel()
        recordJob = null
        _uiState.update { it.copy(isRecording = false, inputLevel = 0f, stage = null) }
    }

    private suspend fun analyze(samples: FloatArray, rate: Int) {
        _uiState.update { it.copy(stage = AnalysisStage.CLEANING_NOISE) }
        val cleaned = withContext(computeDispatcher) { AudioCleaner.clean(samples) }
        _uiState.update { it.copy(stage = AnalysisStage.DETECTING_CHORDS) }
        chordAnalyzer.analyze(cleaned, rate)
            .onSuccess { result ->
                _uiState.update { it.copy(stage = AnalysisStage.MATCHING_SONGS) }
                analysisRepository.save(result)
                show(result)
            }
            .onFailure { e -> _uiState.update { it.copy(errorMessage = e.messageRes()) } }
        _uiState.update { it.copy(stage = null) }
    }

    private suspend fun show(result: AnalysisResult) {
        _uiState.update { it.copy(result = result, chordTrack = AnalysisInsights.chordTrack(result), savedProgressionId = null) }
        exploreRepository.catalog().onSuccess { catalog ->
            val recommendations = withContext(computeDispatcher) { AnalysisInsights.recommend(result, catalog) }
            _uiState.update { it.copy(recommendations = recommendations) }
        }
    }

    private fun addBar(symbol: String) {
        val key = _uiState.value.result?.key ?: return
        _uiState.update { it.copy(chordTrack = it.chordTrack + ChordTrackBar(symbol, RomanNumerals.of(symbol, key))) }
    }

    private fun playResult() {
        val s = _uiState.value
        val bpm = s.result?.bpm ?: return
        if (s.chordTrack.isEmpty()) return
        playJob = viewModelScope.launch {
            _uiState.update { it.copy(isPlayingResult = true) }
            val barMs = 60_000L * 4 / bpm.coerceAtLeast(1)
            s.chordTrack.forEachIndexed { index, bar ->
                _uiState.update { it.copy(playingIndex = index) }
                val midis = withContext(computeDispatcher) {
                    VoicingGenerator.voicings(bar.symbol, limit = 1).firstOrNull()?.let { VoicingGenerator.midiNotes(it) }.orEmpty()
                }
                tonePlayer.playChord(midis, barMs)
            }
            _uiState.update { it.copy(isPlayingResult = false, playingIndex = null) }
        }
    }

    private fun stopPlayback() {
        playJob?.cancel()
        playJob = null
        tonePlayer.stop()
        _uiState.update { it.copy(isPlayingResult = false, playingIndex = null) }
    }

    private fun saveAsProgression(title: String) {
        val s = _uiState.value
        val result = s.result ?: return
        val chords = s.chordTrack.map { it.symbol }
        if (title.isBlank() || chords.isEmpty() || s.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val draft = ProgressionDraft(
                title = title,
                key = result.key,
                bpm = result.bpm,
                genre = s.recommendations.songs.groupingBy { it.genre }.eachCount().maxByOrNull { it.value }?.key ?: DEFAULT_GENRE,
                difficulty = AnalysisInsights.difficulty(chords),
                chords = chords,
            )
            progressionRepository.create(draft)
                .onSuccess { id -> _uiState.update { it.copy(isSaving = false, savedProgressionId = id) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, errorMessage = e.messageRes()) } }
        }
    }

    override fun onCleared() {
        tonePlayer.stop()
    }

    companion object {
        const val FRAME_SIZE = 4096
        private const val DEFAULT_GENRE = "Pop"

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as WAKordApp).container
                AnalyzeViewModel(
                    audioCapture = container.audioCapture,
                    chordAnalyzer = container.chordAnalyzer,
                    analysisRepository = container.analysisRepository,
                    exploreRepository = container.exploreRepository,
                    progressionRepository = container.progressionRepository,
                    tonePlayer = container.tonePlayer,
                )
            }
        }
    }
}
