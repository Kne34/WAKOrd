package edu.bluejack26_1.wakord.core.analysis

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.log2
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object Chromagram {
    private const val MIN_HZ = 60.0
    private const val MAX_HZ = 2_000.0

    fun rms(frame: FloatArray): Float {
        if (frame.isEmpty()) return 0f
        var sum = 0.0
        frame.forEach { sum += it * it }
        return sqrt(sum / frame.size).toFloat()
    }

    fun chroma(frame: FloatArray, sampleRate: Int, referencePitch: Float = 440f): FloatArray {
        val size = Integer.highestOneBit(frame.size.coerceAtLeast(2))
        val re = DoubleArray(size) { i -> frame[i] * (0.5 - 0.5 * cos(2 * PI * i / (size - 1))) }
        val im = DoubleArray(size)
        fft(re, im)
        val chroma = FloatArray(12)
        for (bin in 1 until size / 2) {
            val hz = bin.toDouble() * sampleRate / size
            if (hz < MIN_HZ || hz > MAX_HZ) continue
            val magnitude = hypot(re[bin], im[bin])
            val midi = 69 + 12 * log2(hz / referencePitch)
            chroma[Math.floorMod(midi.roundToInt(), 12)] += magnitude.toFloat()
        }
        val max = chroma.max()
        if (max > 0f) for (i in chroma.indices) chroma[i] /= max
        return chroma
    }

    internal fun fft(re: DoubleArray, im: DoubleArray) {
        val n = re.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                re[i] = re[j].also { re[j] = re[i] }
                im[i] = im[j].also { im[j] = im[i] }
            }
        }
        var length = 2
        while (length <= n) {
            val angle = -2 * PI / length
            val wRe = cos(angle)
            val wIm = sin(angle)
            var start = 0
            while (start < n) {
                var curRe = 1.0
                var curIm = 0.0
                for (k in 0 until length / 2) {
                    val a = start + k
                    val b = a + length / 2
                    val tRe = re[b] * curRe - im[b] * curIm
                    val tIm = re[b] * curIm + im[b] * curRe
                    re[b] = re[a] - tRe
                    im[b] = im[a] - tIm
                    re[a] += tRe
                    im[a] += tIm
                    val next = curRe * wRe - curIm * wIm
                    curIm = curRe * wIm + curIm * wRe
                    curRe = next
                }
                start += length
            }
            length = length shl 1
        }
    }
}
