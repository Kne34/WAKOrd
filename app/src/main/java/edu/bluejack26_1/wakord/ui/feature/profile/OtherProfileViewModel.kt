package edu.bluejack26_1.wakord.ui.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bluejack26_1.wakord.WAKordApp
import edu.bluejack26_1.wakord.core.error.AppError
import edu.bluejack26_1.wakord.core.error.messageRes
import edu.bluejack26_1.wakord.domain.repository.FollowRepository
import edu.bluejack26_1.wakord.domain.repository.ProfileRepository
import edu.bluejack26_1.wakord.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OtherProfileViewModel(
    private val uid: String,
    userRepository: UserRepository,
    profileRepository: ProfileRepository,
    private val followRepository: FollowRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OtherProfileUiState())
    val uiState: StateFlow<OtherProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            profileRepository.user(uid).collect { u ->
                _uiState.update {
                    it.copy(isLoading = false, user = u, errorMessage = if (u == null) AppError.NotFound.messageRes() else it.errorMessage)
                }
            }
        }
        viewModelScope.launch { userRepository.currentUser().collect { me -> _uiState.update { it.copy(isMe = me?.uid == uid) } } }
        viewModelScope.launch { followRepository.isFollowing(uid).collect { v -> _uiState.update { it.copy(isFollowing = v) } } }
        viewModelScope.launch { followRepository.stats(uid).collect { v -> _uiState.update { it.copy(stats = v) } } }
        viewModelScope.launch {
            profileRepository.progressionsBy(uid).collect { list ->
                _uiState.update { it.copy(progressions = list.sortedByDescending { p -> p.likeCount }) }
            }
        }
    }

    fun onAction(action: OtherProfileAction) {
        when (action) {
            OtherProfileAction.ToggleFollow -> toggleFollow()
            OtherProfileAction.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun toggleFollow() {
        val s = _uiState.value
        if (s.isMe) return
        val follow = !s.isFollowing
        val delta = if (follow) 1 else -1
        _uiState.update { it.copy(isFollowing = follow, stats = it.stats.copy(followers = it.stats.followers + delta)) }
        viewModelScope.launch {
            followRepository.setFollowing(uid, follow).onFailure { e ->
                _uiState.update {
                    it.copy(isFollowing = !follow, stats = it.stats.copy(followers = it.stats.followers - delta), errorMessage = e.messageRes())
                }
            }
        }
    }

    companion object {
        const val USER_ID = "uid"

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as WAKordApp).container
                OtherProfileViewModel(
                    uid = requireNotNull(createSavedStateHandle()[USER_ID]),
                    userRepository = container.userRepository,
                    profileRepository = container.profileRepository,
                    followRepository = container.followRepository,
                )
            }
        }
    }
}
