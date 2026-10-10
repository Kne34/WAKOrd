package edu.bluejack26_1.wakord.ui.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bluejack26_1.wakord.WAKordApp
import edu.bluejack26_1.wakord.core.error.messageRes
import edu.bluejack26_1.wakord.core.music.VoicingGenerator
import edu.bluejack26_1.wakord.domain.model.SavedType
import edu.bluejack26_1.wakord.domain.repository.FollowRepository
import edu.bluejack26_1.wakord.domain.repository.ProfileRepository
import edu.bluejack26_1.wakord.domain.repository.SavedItemsRepository
import edu.bluejack26_1.wakord.domain.repository.SavedLibraryRepository
import edu.bluejack26_1.wakord.domain.repository.SyncRepository
import edu.bluejack26_1.wakord.domain.repository.UserRepository
import edu.bluejack26_1.wakord.ui.feature.home.SavedChord
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(
    private val userRepository: UserRepository,
    private val profileRepository: ProfileRepository,
    private val followRepository: FollowRepository,
    private val savedLibraryRepository: SavedLibraryRepository,
    private val savedItemsRepository: SavedItemsRepository,
    private val syncRepository: SyncRepository,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val currentUser = userRepository.currentUser()

    init {
        viewModelScope.launch {
            currentUser.collect { u -> _uiState.update { it.copy(isLoading = false, user = u) } }
        }
        viewModelScope.launch {
            currentUser.filterNotNull().flatMapLatest { profileRepository.progressionsBy(it.uid) }
                .collect { list -> _uiState.update { it.copy(myProgressions = list) } }
        }
        viewModelScope.launch {
            currentUser.filterNotNull().flatMapLatest { followRepository.stats(it.uid) }
                .collect { stats -> _uiState.update { it.copy(stats = stats) } }
        }
        viewModelScope.launch {
            savedLibraryRepository.entries(SavedType.SONG).collect { list -> _uiState.update { it.copy(savedSongs = list) } }
        }
        viewModelScope.launch {
            savedLibraryRepository.entries(SavedType.CHORD).collect { list ->
                val chords = withContext(computeDispatcher) {
                    list.map { SavedChord(it.id, VoicingGenerator.voicings(it.id, limit = 1).firstOrNull()) }
                }
                _uiState.update { it.copy(savedChords = chords) }
            }
        }
        viewModelScope.launch {
            syncRepository.status.collect { status -> _uiState.update { it.copy(syncStatus = status) } }
        }
        syncNow()
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            is ProfileAction.SelectTab -> _uiState.update { it.copy(tab = action.tab) }
            is ProfileAction.AskRemove -> _uiState.update { it.copy(pendingRemoval = action.entry) }
            ProfileAction.CancelRemove -> _uiState.update { it.copy(pendingRemoval = null) }
            ProfileAction.ConfirmRemove -> confirmRemove()
            ProfileAction.SyncNow -> syncNow()
            ProfileAction.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun confirmRemove() {
        val entry = _uiState.value.pendingRemoval ?: return
        _uiState.update { it.copy(pendingRemoval = null) }
        viewModelScope.launch {
            savedItemsRepository.setSaved(entry.type, entry.id, false).onFailure { e ->
                _uiState.update { it.copy(errorMessage = e.messageRes()) }
            }
        }
    }

    private fun syncNow() {
        viewModelScope.launch { savedLibraryRepository.syncNow() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as WAKordApp).container
                ProfileViewModel(
                    userRepository = container.userRepository,
                    profileRepository = container.profileRepository,
                    followRepository = container.followRepository,
                    savedLibraryRepository = container.savedLibraryRepository,
                    savedItemsRepository = container.savedItemsRepository,
                    syncRepository = container.syncRepository,
                )
            }
        }
    }
}
