package edu.bluejack26_1.wakord.core.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

object PluckSynth {
    private const val MAX_HARMONICS = 10
    private const val BRIGHTNESS = 1.4
    private const val HARMONIC_DAMPING = 0.7
    private const val BASE_SUSTAIN_SECONDS = 1.8
    private const val ATTACK_SECONDS = 0.004f
    private const val RELEASE_SECONDS = 0.03f
    private const val PEAK = 0.8f

    fun frequencyOf(midi: Int, referencePitch: Float = 440f): Float =
        (referencePitch * 2.0.pow((midi - 69) / 12.0)).toFloat()

    fun render(
        frequencies: List<Float>,
        durationMs: Long,
        sampleRate: Int = 44_100,
        strumMs: Int = 25,
    ): FloatArray {
        val total = (sampleRate * durationMs / 1000).toInt()
        val out = FloatArray(total)
        if (total == 0 || frequencies.isEmpty()) return out
        val attack = (sampleRate * ATTACK_SECONDS).toInt().coerceAtLeast(1)

        frequencies.forEachIndexed { n, frequency ->
            val start = n * strumMs * sampleRate / 1000
            if (start >= total || frequency <= 0f) return@forEachIndexed
            val sustain = (BASE_SUSTAIN_SECONDS * (110.0 / frequency).pow(0.35)).coerceIn(0.6, 3.0)
            var k = 1
            while (k <= MAX_HARMONICS && k * frequency < sampleRate * 0.45f) {
                val omega = 2 * PI * k * frequency / sampleRate
                val coefficient = 2 * cos(omega)
                var previous = sin(-omega)
                var beforePrevious = sin(-2 * omega)
                val decay = exp(-(1 + HARMONIC_DAMPING * (k - 1)) / (sustain * sampleRate))
                var envelope = 1.0 / k.toDouble().pow(BRIGHTNESS)
                for (i in start until total) {
                    val value = coefficient * previous - beforePrevious
                    beforePrevious = previous
                    previous = value
                    val age = i - start
                    val ramp = if (age < attack) age.toDouble() / attack else 1.0
                    out[i] += (value * envelope * ramp).toFloat()
                    envelope *= decay
                }
                k++
            }
        }

        val peak = out.maxOf { abs(it) }.coerceAtLeast(1e-6f)
        val gain = PEAK / peak
        val release = (sampleRate * RELEASE_SECONDS).toInt().coerceIn(1, total)
        for (i in out.indices) {
            val fromEnd = total - 1 - i
            out[i] *= gain * if (fromEnd < release) fromEnd.toFloat() / release else 1f
        }
        return out
    }
}
