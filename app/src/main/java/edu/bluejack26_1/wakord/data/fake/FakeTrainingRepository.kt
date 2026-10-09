package edu.bluejack26_1.wakord.data.fake

import edu.bluejack26_1.wakord.domain.model.TrainingSession
import edu.bluejack26_1.wakord.domain.repository.TrainingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeTrainingRepository(initial: List<TrainingSession> = emptyList()) : TrainingRepository {
    override val sessions = MutableStateFlow(initial)
    var syncCalls = 0

    override suspend fun save(session: TrainingSession): Result<Unit> {
        sessions.update { it + session }
        return Result.success(Unit)
    }

    override suspend fun syncPending(): Result<Int> {
        syncCalls++
        return Result.success(0)
    }

    override suspend fun refreshFromRemote(): Result<Unit> = Result.success(Unit)
}
