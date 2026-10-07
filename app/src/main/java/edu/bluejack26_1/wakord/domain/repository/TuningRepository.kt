package edu.bluejack26_1.wakord.domain.repository

import edu.bluejack26_1.wakord.domain.model.Tuning
import kotlinx.coroutines.flow.Flow

interface TuningRepository {
    fun customTunings(): Flow<List<Tuning>>
    suspend fun saveCustomTuning(tuning: Tuning): Result<Unit>
    suspend fun deleteCustomTuning(id: String): Result<Unit>
}
