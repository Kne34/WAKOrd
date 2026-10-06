package edu.bluejack26_1.wakord.data.fake

import edu.bluejack26_1.wakord.domain.model.Tuning
import edu.bluejack26_1.wakord.domain.repository.TuningRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeTuningRepository : TuningRepository {
    private val tunings = MutableStateFlow<List<Tuning>>(emptyList())

    override fun customTunings(): Flow<List<Tuning>> = tunings

    override suspend fun saveCustomTuning(tuning: Tuning): Result<Unit> {
        tunings.update { list -> list.filterNot { it.id == tuning.id } + tuning }
        return Result.success(Unit)
    }

    override suspend fun deleteCustomTuning(id: String): Result<Unit> {
        tunings.update { list -> list.filterNot { it.id == id } }
        return Result.success(Unit)
    }
}
