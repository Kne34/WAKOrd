package edu.bluejack26_1.wakord.ui.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bluejack26_1.wakord.WAKordApp
import edu.bluejack26_1.wakord.core.error.messageRes
import edu.bluejack26_1.wakord.domain.model.ProfileUpdate
import edu.bluejack26_1.wakord.domain.repository.ProfileRepository
import edu.bluejack26_1.wakord.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditProfileViewModel(
    private val userRepository: UserRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val user = userRepository.currentUser().first()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    uid = user?.uid.orEmpty(),
                    fullName = user?.fullName.orEmpty(),
                    username = user?.username.orEmpty(),
                    bio = user?.bio.orEmpty(),
                    photoUrl = user?.photoUrl,
                    instruments = user?.instruments.orEmpty().toSet(),
                    genres = user?.genres.orEmpty().toSet(),
                    availableGenres = (it.availableGenres + user?.genres.orEmpty()).distinct(),
                )
            }
        }
    }

    fun onAction(action: EditProfileAction) {
        when (action) {
            is EditProfileAction.FullNameChanged -> _uiState.update { it.copy(fullName = action.value, fullNameError = null, isSaved = false) }
            is EditProfileAction.UsernameChanged -> _uiState.update { it.copy(username = action.value, usernameError = null, isSaved = false) }
            is EditProfileAction.BioChanged -> _uiState.update { it.copy(bio = action.value.take(MAX_BIO), isSaved = false) }
            is EditProfileAction.ToggleInstrument -> _uiState.update { it.copy(instruments = it.instruments.toggle(action.instrument), isSaved = false) }
            is EditProfileAction.ToggleGenre -> _uiState.update { it.copy(genres = it.genres.toggle(action.genre), genresError = null, isSaved = false) }
            is EditProfileAction.PhotoPicked -> uploadPhoto(action.uri)
            EditProfileAction.Save -> save()
            EditProfileAction.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun uploadPhoto(uri: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingPhoto = true) }
            profileRepository.uploadPhoto(uri)
                .onSuccess { url -> _uiState.update { it.copy(isUploadingPhoto = false, photoUrl = url) } }
                .onFailure { e -> _uiState.update { it.copy(isUploadingPhoto = false, errorMessage = e.messageRes()) } }
        }
    }

    private fun save() {
        val s = _uiState.value
        if (s.isSaving) return
        val fullNameError = if (s.fullName.isBlank()) ProfileFieldError.REQUIRED else null
        val usernameError = when {
            s.username.isBlank() -> ProfileFieldError.REQUIRED
            !USERNAME.matches(s.username.trim().lowercase()) -> ProfileFieldError.USERNAME_INVALID
            else -> null
        }
        val genresError = if (s.genres.size < MIN_GENRES) ProfileFieldError.TOO_FEW_GENRES else null
        _uiState.update { it.copy(fullNameError = fullNameError, usernameError = usernameError, genresError = genresError) }
        if (fullNameError != null || usernameError != null || genresError != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            profileRepository.updateProfile(ProfileUpdate(s.fullName, s.username, s.bio, s.instruments, s.genres))
                .onSuccess { _uiState.update { it.copy(isSaving = false, isSaved = true) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, errorMessage = e.messageRes()) } }
        }
    }

    private fun <T> Set<T>.toggle(item: T): Set<T> = if (item in this) this - item else this + item

    companion object {
        private const val MIN_GENRES = 3
        private const val MAX_BIO = 160
        private val USERNAME = Regex("^[a-z0-9_.]{3,20}$")

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as WAKordApp).container
                EditProfileViewModel(container.userRepository, container.profileRepository)
            }
        }
    }
}
