package edu.bluejack26_1.wakord.domain.repository

import edu.bluejack26_1.wakord.domain.model.Instrument
import edu.bluejack26_1.wakord.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUserId: Flow<String?>
    val isLoggedIn: Boolean

    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(fullName: String, email: String, password: String): Result<User>
    suspend fun sendPasswordReset(email: String): Result<Unit>
    suspend fun savePreferences(instruments: Set<Instrument>, genres: Set<String>): Result<Unit>
    suspend fun logout()
}
