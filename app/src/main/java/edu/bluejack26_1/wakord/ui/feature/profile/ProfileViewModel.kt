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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
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

    private var loadJob: Job? = null

    init {
        load()
        viewModelScope.launch {
            currentUser.filterNotNull().flatMapLatest { profileRepository.progressionsBy(it.uid) }
                .catch { }
                .collect { list -> _uiState.update { it.copy(myProgressions = list) } }
        }
        viewModelScope.launch {
            currentUser.filterNotNull().flatMapLatest { followRepository.stats(it.uid) }
                .catch { }
                .collect { stats -> _uiState.update { it.copy(stats = stats) } }
        }
        viewModelScope.launch {
            savedLibraryRepository.entries(SavedType.SONG).catch { }.collect { list -> _uiState.update { it.copy(savedSongs = list) } }
        }
        viewModelScope.launch {
            savedLibraryRepository.entries(SavedType.CHORD).catch { }.collect { list ->
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

    private fun load() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        loadJob = viewModelScope.launch {
            currentUser
                .catch { e -> _uiState.update { it.copy(isLoading = false, loadError = e.messageRes()) } }
                .collect { u -> _uiState.update { it.copy(isLoading = false, user = u) } }
        }
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            is ProfileAction.SelectTab -> _uiState.update { it.copy(tab = action.tab) }
            is ProfileAction.AskRemove -> _uiState.update { it.copy(pendingRemoval = action.entry) }
            ProfileAction.CancelRemove -> _uiState.update { it.copy(pendingRemoval = null) }
            ProfileAction.ConfirmRemove -> confirmRemove()
            ProfileAction.SyncNow -> syncNow()
            ProfileAction.Retry -> load()
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
