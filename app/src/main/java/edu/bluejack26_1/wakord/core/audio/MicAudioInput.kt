package edu.bluejack26_1.wakord.core.audio

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

class MicAudioInput(
    override val sampleRate: Int = 44_100,
    private val frameSize: Int = 4096,
    private val hopSize: Int = 2048,
) : AudioInput {

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun frames(): Flow<FloatArray> = flow {
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_FLOAT,
        )
        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_FLOAT,
            maxOf(minBuffer, frameSize * Float.SIZE_BYTES * 2),
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            throw IllegalStateException("Microphone is not available")
        }
        val window = FloatArray(frameSize)
        val hop = FloatArray(hopSize)
        var filled = 0
        try {
            record.startRecording()
            while (currentCoroutineContext().isActive) {
                val read = record.read(hop, 0, hopSize, AudioRecord.READ_BLOCKING)
                if (read <= 0) continue
                System.arraycopy(window, read, window, 0, frameSize - read)
                System.arraycopy(hop, 0, window, frameSize - read, read)
                filled = minOf(frameSize, filled + read)
                if (filled == frameSize) emit(window.copyOf())
            }
        } finally {
            runCatching { record.stop() }
            record.release()
        }
    }.flowOn(Dispatchers.IO)
}
