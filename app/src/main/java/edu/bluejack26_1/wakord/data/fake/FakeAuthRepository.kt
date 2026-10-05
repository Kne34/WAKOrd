package edu.bluejack26_1.wakord.data.fake

import edu.bluejack26_1.wakord.domain.model.Instrument
import edu.bluejack26_1.wakord.domain.model.User
import edu.bluejack26_1.wakord.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAuthRepository : AuthRepository {
    private val demoUser = User(uid = "demo", fullName = "Keane Wilson", email = "keane.wilson@mail.com")

    var loginResult: Result<User> = Result.success(demoUser)
    var registerResult: Result<User> = Result.success(demoUser)
    var resetResult: Result<Unit> = Result.success(Unit)
    var preferencesResult: Result<Unit> = Result.success(Unit)

    var savedInstruments: Set<Instrument>? = null
    var savedGenres: Set<String>? = null
    var resetEmail: String? = null

    override val currentUserId = MutableStateFlow<String?>(null)
    override val isLoggedIn: Boolean get() = currentUserId.value != null

    override suspend fun login(email: String, password: String): Result<User> =
        loginResult.onSuccess { currentUserId.value = it.uid }

    override suspend fun register(fullName: String, email: String, password: String): Result<User> =
        registerResult.onSuccess { currentUserId.value = it.uid }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        resetEmail = email
        return resetResult
    }

    override suspend fun savePreferences(instruments: Set<Instrument>, genres: Set<String>): Result<Unit> {
        savedInstruments = instruments
        savedGenres = genres
        return preferencesResult
    }

    override suspend fun logout() {
        currentUserId.value = null
    }
}
