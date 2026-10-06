package edu.bluejack26_1.wakord.core.audio

import kotlin.math.sqrt

class YinPitchDetector(
    private val threshold: Float = 0.15f,
    private val silenceRms: Float = 0.01f,
) : PitchDetector {

    override fun detect(samples: FloatArray, sampleRate: Int): PitchResult? {
        if (samples.size < 4 || rms(samples) < silenceRms) return null
        val half = samples.size / 2
        val diff = FloatArray(half)

        for (tau in 1 until half) {
            var sum = 0f
            for (i in 0 until half) {
                val delta = samples[i] - samples[i + tau]
                sum += delta * delta
            }
            diff[tau] = sum
        }

        diff[0] = 1f
        var runningSum = 0f
        for (tau in 1 until half) {
            runningSum += diff[tau]
            diff[tau] = if (runningSum == 0f) 1f else diff[tau] * tau / runningSum
        }

        var tauEstimate = -1
        var tau = 2
        while (tau < half) {
            if (diff[tau] < threshold) {
                while (tau + 1 < half && diff[tau + 1] < diff[tau]) tau++
                tauEstimate = tau
                break
            }
            tau++
        }
        if (tauEstimate == -1) return null

        val betterTau = if (tauEstimate in 1 until half - 1) {
            val s0 = diff[tauEstimate - 1]
            val s1 = diff[tauEstimate]
            val s2 = diff[tauEstimate + 1]
            val denominator = 2f * (s0 - 2f * s1 + s2)
            if (denominator != 0f) tauEstimate + (s0 - s2) / denominator else tauEstimate.toFloat()
        } else {
            tauEstimate.toFloat()
        }

        return PitchResult(
            frequencyHz = sampleRate / betterTau,
            probability = (1f - diff[tauEstimate]).coerceIn(0f, 1f),
        )
    }

    private fun rms(samples: FloatArray): Float {
        var sum = 0f
        for (s in samples) sum += s * s
        return sqrt(sum / samples.size)
    }
}
