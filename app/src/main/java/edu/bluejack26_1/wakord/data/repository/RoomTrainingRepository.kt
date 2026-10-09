package edu.bluejack26_1.wakord.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import edu.bluejack26_1.wakord.data.local.TrainingDao
import edu.bluejack26_1.wakord.data.local.TrainingSessionEntity
import edu.bluejack26_1.wakord.data.remote.firebaseCall
import edu.bluejack26_1.wakord.data.remote.int
import edu.bluejack26_1.wakord.data.remote.long
import edu.bluejack26_1.wakord.data.remote.requireUid
import edu.bluejack26_1.wakord.data.remote.string
import edu.bluejack26_1.wakord.domain.model.TrainingSession
import edu.bluejack26_1.wakord.domain.repository.TrainingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class RoomTrainingRepository(
    private val dao: TrainingDao,
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase,
) : TrainingRepository {
    private val uid: String get() = auth.currentUser?.uid.orEmpty()

    override val sessions: Flow<List<TrainingSession>> = flow {
        emitAll(dao.observe(uid).map { list -> list.map { it.toDomain() } })
    }

    override suspend fun save(session: TrainingSession): Result<Unit> = runCatching {
        dao.upsert(listOf(TrainingSessionEntity.from(session, uid, synced = false)))
        syncPending()
        Unit
    }

    override suspend fun syncPending(): Result<Int> = firebaseCall {
        val owner = auth.requireUid()
        val pending = dao.unsynced(owner)
        if (pending.isNotEmpty()) {
            database.reference.updateChildren(
                pending.associate { e ->
                    "training/$owner/sessions/${e.id}" to mapOf(
                        "level" to e.level,
                        "skill" to e.skill,
                        "correct" to e.correct,
                        "total" to e.total,
                        "durationMs" to e.durationMs,
                        "startedAt" to e.startedAt,
                    )
                },
            ).await()
            dao.markSynced(pending.map { it.id })
        }
        pending.size
    }

    override suspend fun refreshFromRemote(): Result<Unit> = firebaseCall {
        val owner = auth.requireUid()
        val remote = database.getReference("training/$owner/sessions").get().await().children.map { c ->
            TrainingSessionEntity(
                id = c.key.orEmpty(),
                ownerUid = owner,
                level = c.int("level"),
                skill = c.string("skill"),
                correct = c.int("correct"),
                total = c.int("total"),
                durationMs = c.long("durationMs"),
                startedAt = c.long("startedAt"),
                synced = true,
            )
        }
        val pendingIds = dao.unsynced(owner).map { it.id }.toSet()
        dao.upsert(remote.filter { it.id !in pendingIds })
    }
}
