package edu.bluejack26_1.wakord.data.repository

import com.google.firebase.auth.FirebaseAuth
import edu.bluejack26_1.wakord.data.local.DraftDao
import edu.bluejack26_1.wakord.data.local.ProgressionDraftEntity
import edu.bluejack26_1.wakord.domain.model.ProgressionDraft
import edu.bluejack26_1.wakord.domain.repository.DraftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class RoomDraftRepository(private val dao: DraftDao, private val auth: FirebaseAuth) : DraftRepository {
    private val uid: String get() = auth.currentUser?.uid.orEmpty()

    override val draft: Flow<ProgressionDraft?> = flow { emitAll(dao.observe(uid).map { it?.toDomain() }) }

    override suspend fun saveDraft(draft: ProgressionDraft) = dao.upsert(ProgressionDraftEntity.from(draft, uid))

    override suspend fun clearDraft() = dao.delete(uid)
}
