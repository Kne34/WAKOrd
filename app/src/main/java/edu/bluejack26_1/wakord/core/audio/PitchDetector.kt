package edu.bluejack26_1.wakord.core.audio

import kotlinx.coroutines.flow.Flow

interface AudioInput {
    val sampleRate: Int
    fun frames(): Flow<FloatArray>
}

data class PitchResult(
    val frequencyHz: Float,
    val probability: Float,
)

interface PitchDetector {
    fun detect(samples: FloatArray, sampleRate: Int): PitchResult?
}
