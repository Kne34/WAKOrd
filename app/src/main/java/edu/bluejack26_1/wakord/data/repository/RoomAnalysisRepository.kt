package edu.bluejack26_1.wakord.data.repository

import com.google.firebase.auth.FirebaseAuth
import edu.bluejack26_1.wakord.data.local.AnalysisDao
import edu.bluejack26_1.wakord.data.local.AnalysisEntity
import edu.bluejack26_1.wakord.data.remote.firebaseCall
import edu.bluejack26_1.wakord.data.remote.requireUid
import edu.bluejack26_1.wakord.data.sync.FirebaseBackup
import edu.bluejack26_1.wakord.domain.model.AnalysisResult
import edu.bluejack26_1.wakord.domain.repository.AnalysisRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class RoomAnalysisRepository(
    private val dao: AnalysisDao,
    private val auth: FirebaseAuth,
    private val backup: FirebaseBackup,
) : AnalysisRepository {
    private val uid: String get() = auth.currentUser?.uid.orEmpty()

    override val history: Flow<List<AnalysisResult>> = flow {
        emitAll(dao.observe(uid).map { list -> list.map { it.toDomain() } })
    }

    override suspend fun save(result: AnalysisResult): Result<Unit> = runCatching {
        dao.upsert(listOf(AnalysisEntity.from(result, uid, synced = false)))
        syncPending()
        Unit
    }

    override suspend fun syncPending(): Result<Int> = firebaseCall {
        val owner = auth.requireUid()
        val pending = dao.unsynced(owner)
        if (pending.isNotEmpty()) {
            backup.update(
                pending.associate { e ->
                    "analyses/$owner/${e.id}" to mapOf(
                        "createdAt" to e.createdAt,
                        "durationMs" to e.durationMs,
                        "key" to e.key,
                        "confidence" to e.confidence.toDouble(),
                        "bpm" to e.bpm,
                        "chords" to e.chords,
                        "source" to e.source,
                    )
                },
            )
            dao.markSynced(pending.map { it.id })
        }
        pending.size
    }
}
