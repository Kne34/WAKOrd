package edu.bluejack26_1.wakord.data.remote

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import edu.bluejack26_1.wakord.core.error.AppError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

fun Query.valueFlow(): Flow<DataSnapshot> = callbackFlow {
    val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            trySend(snapshot)
        }

        override fun onCancelled(error: DatabaseError) {
            if (error.code == DatabaseError.PERMISSION_DENIED && FirebaseAuth.getInstance().currentUser == null) {
                close()
            } else {
                close(error.toException())
            }
        }
    }
    addValueEventListener(listener)
    awaitClose { removeEventListener(listener) }
}

fun DataSnapshot.string(key: String): String = child(key).getValue(String::class.java).orEmpty()

fun DataSnapshot.int(key: String): Int = child(key).getValue(Long::class.java)?.toInt() ?: 0

fun DataSnapshot.long(key: String): Long = child(key).getValue(Long::class.java) ?: 0L

fun DataSnapshot.strings(key: String): List<String> =
    child(key).children.mapNotNull { it.getValue(String::class.java) }

fun FirebaseAuth.requireUid(): String = currentUser?.uid ?: throw AppError.NotLoggedIn

fun String.toDatabaseKey(): String = this
    .replace("#", "s")
    .replace("/", "_")
    .replace(".", "-")
    .replace("$", "")
    .replace("[", "(")
    .replace("]", ")")

fun Throwable.toAppError(): AppError = when (this) {
    is AppError -> this
    is FirebaseAuthInvalidCredentialsException, is FirebaseAuthInvalidUserException -> AppError.InvalidCredentials
    is FirebaseAuthUserCollisionException -> AppError.EmailAlreadyUsed
    is FirebaseNetworkException -> AppError.Network
    else -> AppError.Unknown(message)
}

suspend fun <T> firebaseCall(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e.toAppError())
}
