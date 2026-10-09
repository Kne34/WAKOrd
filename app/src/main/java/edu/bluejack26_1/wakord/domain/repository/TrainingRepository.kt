package edu.bluejack26_1.wakord.domain.repository

import edu.bluejack26_1.wakord.domain.model.TrainingSession
import kotlinx.coroutines.flow.Flow

interface TrainingRepository {
    val sessions: Flow<List<TrainingSession>>
    suspend fun save(session: TrainingSession): Result<Unit>
    suspend fun syncPending(): Result<Int>
    suspend fun refreshFromRemote(): Result<Unit>
}
