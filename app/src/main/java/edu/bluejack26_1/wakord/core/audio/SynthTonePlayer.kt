package edu.bluejack26_1.wakord.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

class SynthTonePlayer(private val sampleRate: Int = 44_100) : TonePlayer {
    private val audioDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val generation = AtomicInteger()

    @Volatile
    private var track: AudioTrack? = null

    override suspend fun playNote(midi: Int, durationMs: Long, referencePitch: Float) =
        playChord(listOf(midi), durationMs, referencePitch)

    override suspend fun playChord(midis: List<Int>, durationMs: Long, referencePitch: Float) {
        if (midis.isEmpty() || durationMs <= 0) return
        val ticket = generation.incrementAndGet()
        val pcm = withContext(Dispatchers.Default) {
            val samples = PluckSynth.render(midis.map { PluckSynth.frequencyOf(it, referencePitch) }, durationMs, sampleRate)
            ShortArray(samples.size) { (samples[it] * Short.MAX_VALUE).toInt().toShort() }
        }
        val startedAt = System.nanoTime()
        val finished = withContext(audioDispatcher) { stream(pcm, ticket) }
        if (!finished) return
        val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000
        delay((durationMs - elapsedMs).coerceAtLeast(0))
    }

    override fun stop() {
        generation.incrementAndGet()
        track?.let { runCatching { it.pause() } }
    }

    private fun stream(pcm: ShortArray, ticket: Int): Boolean {
        if (ticket != generation.get()) return false
        val output = track ?: createTrack().also { track = it }
        runCatching {
            output.pause()
            output.flush()
        }
        var offset = 0
        var started = false
        while (offset < pcm.size) {
            if (ticket != generation.get()) {
                runCatching { output.pause() }
                return false
            }
            val written = output.write(pcm, offset, minOf(CHUNK_SAMPLES, pcm.size - offset))
            if (written <= 0) return false
            offset += written
            if (!started) {
                output.play()
                started = true
            }
        }
        return true
    }

    private fun createTrack(): AudioTrack {
        val minBuffer = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(maxOf(minBuffer, CHUNK_SAMPLES * Short.SIZE_BYTES * 2))
            .build()
    }

    private companion object {
        const val CHUNK_SAMPLES = 2048
    }
}
