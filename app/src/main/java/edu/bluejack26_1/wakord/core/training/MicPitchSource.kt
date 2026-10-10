package edu.bluejack26_1.wakord.core.training

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import edu.bluejack26_1.wakord.core.audio.AudioInput
import edu.bluejack26_1.wakord.core.audio.PitchDetector
import edu.bluejack26_1.wakord.core.error.AppError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class MicPitchSource(
    private val context: Context,
    private val audioInput: AudioInput,
    private val pitchDetector: PitchDetector,
) : PitchSource {
    override fun frequencies(): Flow<Float?> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return flow { throw AppError.MicrophoneOff }
        }
        return audioInput.frames().map { pitchDetector.detect(it, audioInput.sampleRate)?.frequencyHz }
    }
}
