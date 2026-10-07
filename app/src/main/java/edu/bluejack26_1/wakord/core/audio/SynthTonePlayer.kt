package edu.bluejack26_1.wakord.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class SynthTonePlayer(private val sampleRate: Int = 44_100) : TonePlayer {
    @Volatile
    private var track: AudioTrack? = null

    override suspend fun playNote(midi: Int, durationMs: Long, referencePitch: Float) =
        playChord(listOf(midi), durationMs, referencePitch)

    override suspend fun playChord(midis: List<Int>, durationMs: Long, referencePitch: Float) {
        if (midis.isEmpty() || durationMs <= 0) return
        val pcm = withContext(Dispatchers.Default) {
            val samples = PluckSynth.render(midis.map { PluckSynth.frequencyOf(it, referencePitch) }, durationMs, sampleRate)
            ShortArray(samples.size) { (samples[it] * Short.MAX_VALUE).toInt().toShort() }
        }
        play(pcm, durationMs)
    }

    override fun stop() {
        track?.let {
            runCatching { it.stop() }
            it.release()
        }
        track = null
    }

    private suspend fun play(pcm: ShortArray, durationMs: Long) {
        stop()
        val audioTrack = AudioTrack.Builder()
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
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * Short.SIZE_BYTES)
            .build()
        audioTrack.write(pcm, 0, pcm.size)
        track = audioTrack
        audioTrack.play()
        try {
            delay(durationMs)
        } finally {
            if (track === audioTrack) stop()
        }
    }
}
