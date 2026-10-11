package edu.bluejack26_1.wakord.core.analysis

import edu.bluejack26_1.wakord.core.error.AppError
import edu.bluejack26_1.wakord.domain.model.AnalysisResult
import edu.bluejack26_1.wakord.domain.model.AnalysisSource
import edu.bluejack26_1.wakord.domain.model.DetectedChord
import edu.bluejack26_1.wakord.domain.repository.ChordAnalyzer
import java.util.UUID

class AlgorithmChordAnalyzer(private val clock: () -> Long = System::currentTimeMillis) : ChordAnalyzer {
    override suspend fun analyze(samples: FloatArray, sampleRate: Int): Result<AnalysisResult> {
        val frames = (0..samples.size - FRAME step HOP).map { start ->
            val frame = samples.copyOfRange(start, start + FRAME)
            val loud = Chromagram.rms(frame) >= SILENCE_RMS
            Frame(if (loud) Chromagram.chroma(frame, sampleRate) else null)
        }
        val labels = frames.map { f -> f.chroma?.let { ChordRecognizer.best(it) } }
        val smoothed = labels.indices.map { i ->
            labels.subList((i - 1).coerceAtLeast(0), (i + 2).coerceAtMost(labels.size))
                .filterNotNull()
                .groupingBy { it.symbol }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key
                ?.takeIf { labels[i] != null }
        }
        val hopMs = HOP * 1000L / sampleRate
        val frameMs = FRAME * 1000L / sampleRate
        val segments = mutableListOf<DetectedChord>()
        smoothed.forEachIndexed { i, symbol ->
            if (symbol == null) return@forEachIndexed
            val start = i * hopMs
            val end = start + frameMs
            val last = segments.lastOrNull()
            if (last != null && last.symbol == symbol && start <= last.endMs) segments[segments.lastIndex] = last.copy(endMs = end)
            else segments += DetectedChord(symbol, start, end)
        }
        val chords = mergeShort(segments)
        if (chords.isEmpty()) return Result.failure(AppError.NoChordsHeard)

        val total = FloatArray(12)
        frames.forEach { f -> f.chroma?.forEachIndexed { i, v -> total[i] += v } }
        val key = KeyDetector.detect(total)
        val scores = labels.filterNotNull().map { it.score }
        return Result.success(
            AnalysisResult(
                id = UUID.randomUUID().toString(),
                createdAt = clock(),
                durationMs = samples.size * 1000L / sampleRate,
                key = key.name,
                confidence = (scores.average().toFloat() * key.confidence).coerceIn(0f, 1f),
                bpm = TempoEstimator.estimate(samples, sampleRate),
                chords = chords,
                source = AnalysisSource.ALGORITHM,
            ),
        )
    }

    private fun mergeShort(segments: List<DetectedChord>): List<DetectedChord> {
        val result = mutableListOf<DetectedChord>()
        segments.forEach { s ->
            val last = result.lastOrNull()
            when {
                last != null && last.symbol == s.symbol -> result[result.lastIndex] = last.copy(endMs = s.endMs)
                s.endMs - s.startMs < MIN_SEGMENT_MS && last != null -> result[result.lastIndex] = last.copy(endMs = s.endMs)
                else -> result += s
            }
        }
        return result.filter { it.endMs - it.startMs >= MIN_SEGMENT_MS || result.size == 1 }
    }

    private class Frame(val chroma: FloatArray?)

    private companion object {
        const val FRAME = 4096
        const val HOP = 2048
        const val SILENCE_RMS = 0.01f
        const val MIN_SEGMENT_MS = 500L
    }
}

class FallbackChordAnalyzer(
    private val primary: ChordAnalyzer,
    private val fallback: ChordAnalyzer,
) : ChordAnalyzer {
    override suspend fun analyze(samples: FloatArray, sampleRate: Int): Result<AnalysisResult> {
        val ai = primary.analyze(samples, sampleRate).getOrNull() ?: return fallback.analyze(samples, sampleRate)
        return if (ai.chords.isEmpty()) Result.failure(AppError.NoChordsHeard) else Result.success(ai)
    }
}
