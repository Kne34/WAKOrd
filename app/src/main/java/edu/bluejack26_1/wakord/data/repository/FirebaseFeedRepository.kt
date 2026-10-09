package edu.bluejack26_1.wakord.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import edu.bluejack26_1.wakord.data.remote.firebaseCall
import edu.bluejack26_1.wakord.data.remote.requireUid
import edu.bluejack26_1.wakord.data.remote.string
import edu.bluejack26_1.wakord.data.remote.valueFlow
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
import kotlinx.coroutines.tasks.await

class FirebaseFeedRepository(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase,
) : FeedRepository {
    override suspend fun songs(): Result<List<Song>> = firebaseCall {
        database.getReference("songs").get().await().children.map { it.toSong() }
    }

    override suspend fun progressionsPage(before: Long?, pageSize: Int): Result<FeedPage> = firebaseCall {
        val ordered = database.getReference("progressions").orderByChild("createdAt")
        val query = (if (before != null) ordered.endBefore(before.toDouble()) else ordered).limitToLast(pageSize)
        val page = query.get().await().children.map { it.toProgression() }.sortedByDescending { it.createdAt }
        FeedPage(page, if (page.size == pageSize) page.last().createdAt else null)
    }

    override suspend fun commentCounts(ids: List<String>): Result<Map<String, Int>> = firebaseCall {
        coroutineScope {
            ids.map { id ->
                async { id to database.getReference("comments/$id").get().await().childrenCount.toInt() }
            }.awaitAll().toMap()
        }
    }

    override suspend fun activity(): Result<UserActivity> = firebaseCall {
        val uid = auth.requireUid()
        coroutineScope {
            val liked = async { database.getReference("userLikes/$uid").get().await().children.mapNotNull { it.key }.toSet() }
            val played = async { database.getReference("plays/$uid").get().await().children.mapNotNull { it.key }.toSet() }
            val saved = async {
                val saves = database.getReference("saves/$uid").get().await()
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
        return database.getReference("userLikes/$uid").valueFlow()
            .map { snapshot -> snapshot.children.mapNotNull { it.key }.toSet() }
            .catch { emit(emptySet()) }
    }
}

class FirebaseUserRepository(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase,
) : UserRepository {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun currentUser(): Flow<User?> = uidFlow().flatMapLatest { uid ->
        if (uid == null) flowOf(null)
        else database.getReference("users/$uid").valueFlow().map { if (it.exists()) it.toUser(uid) else null }
    }

    private fun uidFlow(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }
}
