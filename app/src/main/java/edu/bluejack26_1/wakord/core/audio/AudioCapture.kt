package edu.bluejack26_1.wakord.core.audio

import kotlinx.coroutines.flow.Flow

interface AudioCapture {
    val sampleRate: Int
    fun frames(frameSize: Int): Flow<FloatArray>
}
