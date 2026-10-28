package edu.bluejack26_1.wakord.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ServerValue
import edu.bluejack26_1.wakord.data.remote.firebaseCall
import edu.bluejack26_1.wakord.data.remote.requireUid
import edu.bluejack26_1.wakord.data.sync.FirebaseBackup
import edu.bluejack26_1.wakord.domain.model.Instrument
import edu.bluejack26_1.wakord.domain.model.User
import edu.bluejack26_1.wakord.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val backup: FirebaseBackup,
) : AuthRepository {
    override val currentUserId: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override val isLoggedIn: Boolean get() = auth.currentUser != null

    override suspend fun login(email: String, password: String): Result<User> = firebaseCall {
        val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
        val uid = requireNotNull(result.user).uid
        backup.get("users/$uid").toUser(uid, email.trim())
    }

    override suspend fun register(fullName: String, email: String, password: String): Result<User> = firebaseCall {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val uid = requireNotNull(result.user).uid
        val user = User(
            uid = uid,
            fullName = fullName.trim(),
            email = email.trim(),
            username = email.trim().substringBefore('@').lowercase(),
        )
        backup.update(
            mapOf(
                "users/$uid" to mapOf(
                    "fullName" to user.fullName,
                    "email" to user.email,
                    "username" to user.username,
                    "bio" to "",
                    "createdAt" to ServerValue.TIMESTAMP,
                ),
            ),
        )
        user
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = firebaseCall {
        auth.sendPasswordResetEmail(email.trim()).await()
        Unit
    }

    override suspend fun savePreferences(instruments: Set<Instrument>, genres: Set<String>): Result<Unit> = firebaseCall {
        val uid = auth.requireUid()
        backup.update(
            mapOf(
                "users/$uid/instruments" to instruments.map { it.name },
                "users/$uid/genres" to genres.toList(),
            ),
        )
    }

    override suspend fun logout() {
        auth.signOut()
    }
}
