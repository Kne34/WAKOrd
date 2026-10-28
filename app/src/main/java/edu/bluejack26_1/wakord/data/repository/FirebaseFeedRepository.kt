package edu.bluejack26_1.wakord.data.repository

import com.google.firebase.auth.FirebaseAuth
import edu.bluejack26_1.wakord.data.remote.NodeOrder
import edu.bluejack26_1.wakord.data.remote.NodeQuery
import edu.bluejack26_1.wakord.data.remote.firebaseCall
import edu.bluejack26_1.wakord.data.remote.requireUid
import edu.bluejack26_1.wakord.data.remote.string
import edu.bluejack26_1.wakord.data.sync.FirebaseBackup
import edu.bluejack26_1.wakord.domain.model.FeedPage
import edu.bluejack26_1.wakord.domain.model.SavedType
import edu.bluejack26_1.wakord.domain.model.Song
import edu.bluejack26_1.wakord.domain.model.User
import edu.bluejack26_1.wakord.domain.model.UserActivity
import edu.bluejack26_1.wakord.domain.repository.FeedRepository
import edu.bluejack26_1.wakord.domain.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class FirebaseFeedRepository(
    private val auth: FirebaseAuth,
    private val backup: FirebaseBackup,
) : FeedRepository {
    override suspend fun songs(): Result<List<Song>> = firebaseCall {
        backup.get("songs").children.map { it.toSong() }
    }

    override suspend fun progressionsPage(before: Long?, pageSize: Int): Result<FeedPage> = firebaseCall {
        val query = NodeQuery("progressions", NodeOrder.Child("createdAt"), endBefore = before?.toDouble(), limitToLast = pageSize)
        val page = backup.get(query).children.map { it.toProgression() }.sortedByDescending { it.createdAt }
        FeedPage(page, if (page.size == pageSize) page.last().createdAt else null)
    }

    override suspend fun commentCounts(ids: List<String>): Result<Map<String, Int>> = firebaseCall {
        coroutineScope {
            ids.map { id ->
                async { id to (firebaseCall { backup.get("comments/$id").childrenCount }.getOrNull() ?: 0) }
            }.awaitAll().toMap()
        }
    }

    override suspend fun activity(): Result<UserActivity> = firebaseCall {
        val uid = auth.requireUid()
        coroutineScope {
            val liked = async { backup.get("userLikes/$uid").children.mapNotNull { it.key }.toSet() }
            val played = async { backup.get("plays/$uid").children.mapNotNull { it.key }.toSet() }
            val saved = async {
                val saves = backup.get("saves/$uid")
                listOf(SavedType.SONG, SavedType.PROGRESSION)
                    .flatMap { type -> saves.child(type.path).children.map { it.string("id") } }
                    .filter { it.isNotEmpty() }
                    .toSet()
            }
            UserActivity(likedIds = liked.await(), savedIds = saved.await(), playedIds = played.await())
        }
    }

    override fun likedIds(): Flow<Set<String>> {
        val uid = auth.currentUser?.uid ?: return flowOf(emptySet())
        return backup.observe("userLikes/$uid")
            .map { snapshot -> snapshot.children.mapNotNull { it.key }.toSet() }
            .catch { emit(emptySet()) }
    }
}

class FirebaseUserRepository(
    private val auth: FirebaseAuth,
    private val backup: FirebaseBackup,
) : UserRepository {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun currentUser(): Flow<User?> = uidFlow().flatMapLatest { uid ->
        if (uid == null) flowOf(null)
        else backup.observe("users/$uid").map { if (it.exists()) it.toUser(uid) else null }
    }

    private fun uidFlow(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }
}
