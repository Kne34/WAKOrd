package edu.bluejack26_1.wakord.data.fake

import edu.bluejack26_1.wakord.core.audio.AudioCapture
import edu.bluejack26_1.wakord.domain.model.AnalysisResult
import edu.bluejack26_1.wakord.domain.model.AnalysisSource
import edu.bluejack26_1.wakord.domain.model.DetectedChord
import edu.bluejack26_1.wakord.domain.repository.AnalysisRepository
import edu.bluejack26_1.wakord.domain.repository.ChordAnalyzer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update

object SampleAnalysis {
    val result = AnalysisResult(
        id = "a1",
        createdAt = 1_790_000_000_000L,
        durationMs = 12_000,
        key = "C",
        confidence = 0.86f,
        bpm = 92,
        chords = listOf(
            DetectedChord("C", 0, 3_000),
            DetectedChord("Am", 3_000, 6_000),
            DetectedChord("F", 6_000, 9_000),
            DetectedChord("G", 9_000, 12_000),
        ),
        source = AnalysisSource.AI,
    )
}

class FakeAnalysisRepository(initial: List<AnalysisResult> = emptyList()) : AnalysisRepository {
    override val history = MutableStateFlow(initial)

    override suspend fun save(result: AnalysisResult): Result<Unit> {
        history.update { listOf(result) + it.filter { r -> r.id != result.id } }
        return Result.success(Unit)
    }

    override suspend fun syncPending(): Result<Int> = Result.success(0)
}

class FakeChordAnalyzer(var result: Result<AnalysisResult> = Result.success(SampleAnalysis.result)) : ChordAnalyzer {
    var analyzedSamples = 0

    override suspend fun analyze(samples: FloatArray, sampleRate: Int): Result<AnalysisResult> {
        analyzedSamples = samples.size
        return result
    }
}

class FakeAudioCapture(
    override val sampleRate: Int = 16_000,
    private val frameValue: Float = 0.2f,
    private val frameCount: Int = Int.MAX_VALUE,
    var error: Throwable? = null,
) : AudioCapture {
    override fun frames(frameSize: Int): Flow<FloatArray> = flow {
        error?.let { throw it }
        repeat(frameCount) {
            delay(frameSize * 1000L / sampleRate)
            emit(FloatArray(frameSize) { frameValue })
        }
    }
}
