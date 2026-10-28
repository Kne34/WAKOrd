package edu.bluejack26_1.wakord.data.repository

import com.google.firebase.auth.FirebaseAuth
import edu.bluejack26_1.wakord.data.remote.firebaseCall
import edu.bluejack26_1.wakord.data.remote.long
import edu.bluejack26_1.wakord.data.remote.requireUid
import edu.bluejack26_1.wakord.data.remote.string
import edu.bluejack26_1.wakord.data.sync.FirebaseBackup
import edu.bluejack26_1.wakord.domain.model.SavedType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirebaseSavedItemsRemote(
    private val auth: FirebaseAuth,
    private val backup: FirebaseBackup,
    private val saved: FirebaseSavedItemsRepository = FirebaseSavedItemsRepository(auth, backup),
) : SavedItemsRemote {
    override fun currentUid(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun push(type: SavedType, id: String, saved: Boolean): Result<Unit> = this.saved.setSaved(type, id, saved)

    override suspend fun fetch(type: SavedType): Result<List<RemoteSavedItem>> = firebaseCall {
        val uid = auth.requireUid()
        backup.get("saves/$uid/${type.path}").children
            .map { RemoteSavedItem(it.string("id"), it.long("savedAt")) }
            .filter { it.id.isNotEmpty() }
    }

    override suspend fun details(type: SavedType, id: String): ItemDetails = runCatching {
        when (type) {
            SavedType.CHORD -> ItemDetails(id, "")
            SavedType.SONG -> backup.get("songs/$id").let { ItemDetails(it.string("title"), it.string("artist")) }
            SavedType.PROGRESSION -> backup.get("progressions/$id").let {
                ItemDetails(it.string("title"), it.string("authorName"))
            }
        }
    }.getOrDefault(ItemDetails(id, ""))
}
