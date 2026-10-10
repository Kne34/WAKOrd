package edu.bluejack26_1.wakord.core.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import edu.bluejack26_1.wakord.core.error.AppError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

class AndroidAudioCapture(
    private val context: Context,
    override val sampleRate: Int = 16_000,
) : AudioCapture {
    override fun frames(frameSize: Int): Flow<FloatArray> = flow {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            throw AppError.MicrophoneOff
        }
        val minBuffer = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT)
        val recorder = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_FLOAT,
                maxOf(minBuffer, frameSize * 4),
            )
        } catch (e: SecurityException) {
            throw AppError.MicrophoneOff
        }
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            throw AppError.MicrophoneOff
        }
        try {
            recorder.startRecording()
            while (currentCoroutineContext().isActive) {
                val frame = FloatArray(frameSize)
                var filled = 0
                while (filled < frameSize) {
                    val read = recorder.read(frame, filled, frameSize - filled, AudioRecord.READ_BLOCKING)
                    if (read < 0) throw AppError.MicrophoneOff
                    filled += read
                }
                emit(frame)
            }
        } finally {
            if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) recorder.stop()
            recorder.release()
        }
    }.flowOn(Dispatchers.IO)
}
