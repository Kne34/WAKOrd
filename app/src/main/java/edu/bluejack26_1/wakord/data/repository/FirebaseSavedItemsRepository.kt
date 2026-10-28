package edu.bluejack26_1.wakord.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ServerValue
import edu.bluejack26_1.wakord.data.remote.NodeOrder
import edu.bluejack26_1.wakord.data.remote.NodeQuery
import edu.bluejack26_1.wakord.data.remote.firebaseCall
import edu.bluejack26_1.wakord.data.remote.requireUid
import edu.bluejack26_1.wakord.data.remote.string
import edu.bluejack26_1.wakord.data.remote.toDatabaseKey
import edu.bluejack26_1.wakord.data.sync.FirebaseBackup
import edu.bluejack26_1.wakord.domain.model.SavedType
import edu.bluejack26_1.wakord.domain.repository.SavedItemsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class FirebaseSavedItemsRepository(
    private val auth: FirebaseAuth,
    private val backup: FirebaseBackup,
) : SavedItemsRepository {
    override fun isSaved(type: SavedType, id: String): Flow<Boolean> {
        val uid = auth.currentUser?.uid ?: return flowOf(false)
        return backup.observe("saves/$uid/${type.path}/${id.toDatabaseKey()}").map { it.exists() }
    }

    override fun saved(type: SavedType): Flow<List<String>> {
        val uid = auth.currentUser?.uid ?: return flowOf(emptyList())
        return backup.observe(NodeQuery("saves/$uid/${type.path}", NodeOrder.Child("savedAt"))).map { snapshot ->
            snapshot.children.map { it.string("id") }.filter { it.isNotEmpty() }.reversed()
        }
    }

    override suspend fun setSaved(type: SavedType, id: String, saved: Boolean): Result<Unit> = firebaseCall {
        val uid = auth.requireUid()
        val updates = mutableMapOf<String, Any?>(
            "saves/$uid/${type.path}/${id.toDatabaseKey()}" to if (saved) mapOf("id" to id, "savedAt" to ServerValue.TIMESTAMP) else null,
        )
        if (type != SavedType.CHORD) {
            updates["${type.path}/$id/saveCount"] = ServerValue.increment(if (saved) 1L else -1L)
        }
        backup.update(updates)
    }
}
