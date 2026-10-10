package edu.bluejack26_1.wakord.core.analysis

import edu.bluejack26_1.wakord.core.music.NoteNames
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

object AudioCleaner {
    private const val GATE = 0.02f

    fun clean(samples: FloatArray): FloatArray {
        if (samples.isEmpty()) return samples
        val mean = samples.average().toFloat()
        val centered = FloatArray(samples.size) { samples[it] - mean }
        val peak = centered.maxOf { abs(it) }
        if (peak == 0f) return centered
        return FloatArray(centered.size) { i ->
            val v = centered[i] / peak
            if (abs(v) < GATE) 0f else v
        }
    }
}

object KeyDetector {
    private val MAJOR = floatArrayOf(6.35f, 2.23f, 3.48f, 2.33f, 4.38f, 4.09f, 2.52f, 5.19f, 2.39f, 3.66f, 2.29f, 2.88f)
    private val MINOR = floatArrayOf(6.33f, 2.68f, 3.52f, 5.38f, 2.60f, 3.53f, 2.54f, 4.75f, 3.98f, 2.69f, 3.34f, 3.17f)
    private val FLAT_MAJOR = setOf(1, 3, 5, 8, 10)
    private val FLAT_MINOR = setOf(0, 2, 3, 5, 7, 10)

    data class Key(val name: String, val confidence: Float)

    fun detect(chroma: FloatArray): Key {
        val candidates = (0 until 12).flatMap { tonic ->
            listOf(
                Triple(tonic, false, correlation(chroma, MAJOR, tonic)),
                Triple(tonic, true, correlation(chroma, MINOR, tonic)),
            )
        }.sortedByDescending { it.third }
        val (tonic, minor, score) = candidates.first()
        val name = NoteNames.name(tonic, preferFlats = tonic in (if (minor) FLAT_MINOR else FLAT_MAJOR)) + if (minor) "m" else ""
        return Key(name, ((score + 1f) / 2f).coerceIn(0f, 1f))
    }

    private fun correlation(chroma: FloatArray, profile: FloatArray, tonic: Int): Float {
        val shifted = FloatArray(12) { profile[Math.floorMod(it - tonic, 12)] }
        val ma = chroma.average().toFloat()
        val mb = shifted.average().toFloat()
        var num = 0f
        var da = 0f
        var db = 0f
        for (i in 0 until 12) {
            val a = chroma[i] - ma
            val b = shifted[i] - mb
            num += a * b
            da += a * a
            db += b * b
        }
        return if (da == 0f || db == 0f) 0f else num / sqrt(da * db)
    }
}

object TempoEstimator {
    private const val HOP = 256
    private const val MIN_BPM = 60
    private const val MAX_BPM = 180
    const val DEFAULT_BPM = 90

    fun estimate(samples: FloatArray, sampleRate: Int): Int {
        val envelope = FloatArray(samples.size / HOP) { i ->
            var sum = 0f
            for (j in i * HOP until (i + 1) * HOP) sum += samples[j] * samples[j]
            sum
        }
        if (envelope.size < 4) return DEFAULT_BPM
        val onset = FloatArray(envelope.size) { i -> if (i == 0) 0f else (envelope[i] - envelope[i - 1]).coerceAtLeast(0f) }
        val hopsPerSecond = sampleRate.toFloat() / HOP
        var bestLag = 0
        var bestScore = 0f
        val minLag = (hopsPerSecond * 60 / MAX_BPM).roundToInt().coerceAtLeast(1)
        val maxLag = (hopsPerSecond * 60 / MIN_BPM).roundToInt()
        for (lag in minLag..maxLag) {
            if (lag >= onset.size) break
            var score = 0f
            for (i in lag until onset.size) score += onset[i] * onset[i - lag]
            if (score > bestScore) {
                bestScore = score
                bestLag = lag
            }
        }
        return if (bestLag == 0) DEFAULT_BPM else (60f * hopsPerSecond / bestLag).roundToInt()
    }
}
