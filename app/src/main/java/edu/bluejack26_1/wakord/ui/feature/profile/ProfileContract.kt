package edu.bluejack26_1.wakord.ui.feature.profile

import androidx.annotation.StringRes
import edu.bluejack26_1.wakord.domain.model.ChordProgression
import edu.bluejack26_1.wakord.domain.model.Difficulty
import edu.bluejack26_1.wakord.domain.model.Instrument
import edu.bluejack26_1.wakord.domain.model.ProfileStats
import edu.bluejack26_1.wakord.domain.model.SavedEntry
import edu.bluejack26_1.wakord.domain.model.SyncStatus
import edu.bluejack26_1.wakord.domain.model.User
import edu.bluejack26_1.wakord.ui.feature.auth.DefaultGenres
import edu.bluejack26_1.wakord.ui.feature.home.SavedChord

enum class ProfileTab { SONGS, CHORDS, PROGRESSIONS }

data class ProfileUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val stats: ProfileStats = ProfileStats(),
    val tab: ProfileTab = ProfileTab.SONGS,
    val savedSongs: List<SavedEntry> = emptyList(),
    val savedChords: List<SavedChord> = emptyList(),
    val myProgressions: List<ChordProgression> = emptyList(),
    val syncStatus: SyncStatus = SyncStatus(),
    val pendingRemoval: SavedEntry? = null,
    @param:StringRes val errorMessage: Int? = null,
)

sealed interface ProfileAction {
    data class SelectTab(val tab: ProfileTab) : ProfileAction
    data class AskRemove(val entry: SavedEntry) : ProfileAction
    data object ConfirmRemove : ProfileAction
    data object CancelRemove : ProfileAction
    data object SyncNow : ProfileAction
    data object DismissError : ProfileAction
}

enum class ProfileFieldError { REQUIRED, USERNAME_INVALID, TOO_FEW_GENRES }

data class EditProfileUiState(
    val isLoading: Boolean = true,
    val fullName: String = "",
    val username: String = "",
    val bio: String = "",
    val photoUrl: String? = null,
    val instruments: Set<Instrument> = emptySet(),
    val availableGenres: List<String> = DefaultGenres,
    val genres: Set<String> = emptySet(),
    val fullNameError: ProfileFieldError? = null,
    val usernameError: ProfileFieldError? = null,
    val genresError: ProfileFieldError? = null,
    val isUploadingPhoto: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    @param:StringRes val errorMessage: Int? = null,
)

sealed interface EditProfileAction {
    data class FullNameChanged(val value: String) : EditProfileAction
    data class UsernameChanged(val value: String) : EditProfileAction
    data class BioChanged(val value: String) : EditProfileAction
    data class ToggleInstrument(val instrument: Instrument) : EditProfileAction
    data class ToggleGenre(val genre: String) : EditProfileAction
    data class PhotoPicked(val uri: String) : EditProfileAction
    data object Save : EditProfileAction
    data object DismissError : EditProfileAction
}

enum class DraftError { TITLE_REQUIRED, TOO_FEW_CHORDS, INVALID_BPM }

data class NewProgressionUiState(
    val title: String = "",
    val key: String = "C",
    val genre: String = DefaultGenres.first(),
    val difficulty: Difficulty = Difficulty.EASY,
    val bpm: String = "90",
    val chords: List<String> = emptyList(),
    val suggestedChords: List<String> = emptyList(),
    val description: String = "",
    val errors: Set<DraftError> = emptySet(),
    val isPreviewing: Boolean = false,
    val previewIndex: Int? = null,
    val isUploading: Boolean = false,
    val uploadedId: String? = null,
    @param:StringRes val errorMessage: Int? = null,
)

sealed interface NewProgressionAction {
    data class TitleChanged(val value: String) : NewProgressionAction
    data class KeyChanged(val value: String) : NewProgressionAction
    data class GenreChanged(val value: String) : NewProgressionAction
    data class DifficultyChanged(val value: Difficulty) : NewProgressionAction
    data class BpmChanged(val value: String) : NewProgressionAction
    data class AddChord(val symbol: String) : NewProgressionAction
    data class RemoveChord(val index: Int) : NewProgressionAction
    data class DescriptionChanged(val value: String) : NewProgressionAction
    data object TogglePreview : NewProgressionAction
    data object Upload : NewProgressionAction
    data object DismissUploaded : NewProgressionAction
    data object DismissError : NewProgressionAction
}

data class OtherProfileUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val stats: ProfileStats = ProfileStats(),
    val isFollowing: Boolean = false,
    val isMe: Boolean = false,
    val progressions: List<ChordProgression> = emptyList(),
    @param:StringRes val errorMessage: Int? = null,
)

sealed interface OtherProfileAction {
    data object ToggleFollow : OtherProfileAction
    data object DismissError : OtherProfileAction
}
