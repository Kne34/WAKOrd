package edu.bluejack26_1.wakord.core.training

import kotlinx.coroutines.flow.Flow

fun interface PitchSource {
    fun frequencies(): Flow<Float?>
}
