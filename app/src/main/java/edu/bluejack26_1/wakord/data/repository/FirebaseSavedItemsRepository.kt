package edu.bluejack26_1.wakord.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import edu.bluejack26_1.wakord.data.remote.firebaseCall
import edu.bluejack26_1.wakord.data.remote.requireUid
import edu.bluejack26_1.wakord.data.remote.string
import edu.bluejack26_1.wakord.data.remote.toDatabaseKey
import edu.bluejack26_1.wakord.data.remote.valueFlow
import edu.bluejack26_1.wakord.domain.model.SavedType
import edu.bluejack26_1.wakord.domain.repository.SavedItemsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirebaseSavedItemsRepository(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase,
) : SavedItemsRepository {
    override fun isSaved(type: SavedType, id: String): Flow<Boolean> {
        val uid = auth.currentUser?.uid ?: return flowOf(false)
        return database.getReference("saves/$uid/${type.path}/${id.toDatabaseKey()}").valueFlow().map { it.exists() }
    }

    override fun saved(type: SavedType): Flow<List<String>> {
        val uid = auth.currentUser?.uid ?: return flowOf(emptyList())
        return database.getReference("saves/$uid/${type.path}").orderByChild("savedAt").valueFlow().map { snapshot ->
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
        database.reference.updateChildren(updates).await()
        Unit
    }
}
