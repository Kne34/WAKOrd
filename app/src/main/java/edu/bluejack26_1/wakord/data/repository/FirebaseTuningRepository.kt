package edu.bluejack26_1.wakord.data.repository

import com.google.firebase.auth.FirebaseAuth
import edu.bluejack26_1.wakord.data.remote.firebaseCall
import edu.bluejack26_1.wakord.data.remote.requireUid
import edu.bluejack26_1.wakord.data.remote.string
import edu.bluejack26_1.wakord.data.remote.strings
import edu.bluejack26_1.wakord.data.sync.FirebaseBackup
import edu.bluejack26_1.wakord.domain.model.TunerInstrument
import edu.bluejack26_1.wakord.domain.model.Tuning
import edu.bluejack26_1.wakord.domain.repository.TuningRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class FirebaseTuningRepository(
    private val auth: FirebaseAuth,
    private val backup: FirebaseBackup,
) : TuningRepository {
    override fun customTunings(): Flow<List<Tuning>> {
        val uid = auth.currentUser?.uid ?: return flowOf(emptyList())
        return backup.observe("tunings/$uid").map { snapshot ->
            snapshot.children.mapNotNull { t ->
                val instrument = TunerInstrument.entries.firstOrNull { it.name == t.string("instrument") }
                    ?: return@mapNotNull null
                Tuning(
                    id = t.key.orEmpty(),
                    name = t.string("name"),
                    instrument = instrument,
                    notes = t.strings("notes"),
                    isCustom = true,
                )
            }
        }
    }

    override suspend fun saveCustomTuning(tuning: Tuning): Result<Unit> = firebaseCall {
        val uid = auth.requireUid()
        backup.update(
            mapOf(
                "tunings/$uid/${tuning.id}" to mapOf(
                    "name" to tuning.name,
                    "instrument" to tuning.instrument.name,
                    "notes" to tuning.notes,
                ),
            ),
        )
    }

    override suspend fun deleteCustomTuning(id: String): Result<Unit> = firebaseCall {
        val uid = auth.requireUid()
        backup.update(mapOf("tunings/$uid/$id" to null))
    }
}
