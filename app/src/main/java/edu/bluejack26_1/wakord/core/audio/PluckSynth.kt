package edu.bluejack26_1.wakord.core.audio

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

object PluckSynth {
    fun frequencyOf(midi: Int, referencePitch: Float = 440f): Float =
        (referencePitch * 2.0.pow((midi - 69) / 12.0)).toFloat()

    fun render(
        frequencies: List<Float>,
        durationMs: Long,
        sampleRate: Int = 44_100,
        strumMs: Int = 25,
        decay: Float = 0.996f,
        seed: Int = 7,
    ): FloatArray {
        val total = (sampleRate * durationMs / 1000).toInt()
        val out = FloatArray(total)
        if (total == 0) return out
        val random = Random(seed)

        frequencies.forEachIndexed { n, frequency ->
            val period = (sampleRate / frequency).roundToInt().coerceAtLeast(2)
            val buffer = FloatArray(period) { random.nextFloat() * 2f - 1f }
            val start = n * strumMs * sampleRate / 1000
            var index = 0
            for (i in start until total) {
                val current = buffer[index]
                val next = buffer[(index + 1) % period]
                buffer[index] = decay * 0.5f * (current + next)
                out[i] += current
                index = (index + 1) % period
            }
        }

        val peak = out.maxOf { abs(it) }.coerceAtLeast(1e-6f)
        val gain = 0.8f / peak
        val fade = (sampleRate * 0.02f).toInt().coerceAtMost(total)
        for (i in out.indices) {
            val fromEnd = total - i
            out[i] *= gain * if (fromEnd < fade) fromEnd.toFloat() / fade else 1f
        }
        return out
    }
}
