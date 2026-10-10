package edu.bluejack26_1.wakord.ui.feature.profile

import edu.bluejack26_1.wakord.core.music.DiatonicChords
import edu.bluejack26_1.wakord.data.fake.FakeDraftRepository
import edu.bluejack26_1.wakord.data.fake.FakeFollowRepository
import edu.bluejack26_1.wakord.data.fake.FakeProfileRepository
import edu.bluejack26_1.wakord.data.fake.FakeProgressionRepository
import edu.bluejack26_1.wakord.data.fake.FakeSavedItemsRepository
import edu.bluejack26_1.wakord.data.fake.FakeSavedLibraryRepository
import edu.bluejack26_1.wakord.data.fake.FakeSyncRepository
import edu.bluejack26_1.wakord.data.fake.FakeUserRepository
import edu.bluejack26_1.wakord.data.fake.SampleSocial
import edu.bluejack26_1.wakord.domain.model.Difficulty
import edu.bluejack26_1.wakord.domain.model.Instrument
import edu.bluejack26_1.wakord.domain.model.ProgressionDraft
import edu.bluejack26_1.wakord.domain.model.SavedType
import edu.bluejack26_1.wakord.testutil.MainDispatcherRule
import edu.bluejack26_1.wakord.testutil.RecordingTonePlayer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelsTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val users = FakeUserRepository(SampleSocial.me)
    private val profiles = FakeProfileRepository()
    private val library = FakeSavedLibraryRepository()
    private val saved = FakeSavedItemsRepository()

    @Test
    fun `profile shows saved items and asks before removing`() = runTest(main.dispatcher) {
        val vm = ProfileViewModel(users, profiles, FakeFollowRepository(), library, saved, FakeSyncRepository(), main.dispatcher)
        advanceUntilIdle()
        val s = vm.uiState.value
        assertEquals("Keane Wilson", s.user?.fullName)
        assertEquals(listOf("Golden Hour Glow"), s.savedSongs.map { it.title })
        assertEquals("Cmaj7", s.savedChords.single().symbol)
        assertTrue(s.savedChords.single().voicing != null)
        assertEquals(1, library.syncCalls)

        saved.setSaved(SavedType.SONG, "golden-hour-glow", true)
        vm.onAction(ProfileAction.AskRemove(s.savedSongs.single()))
        assertEquals("golden-hour-glow", vm.uiState.value.pendingRemoval?.id)
        vm.onAction(ProfileAction.ConfirmRemove)
        advanceUntilIdle()
        assertNull(vm.uiState.value.pendingRemoval)
        assertFalse(saved.isSaved(SavedType.SONG, "golden-hour-glow").first())
    }

    @Test
    fun `my progressions come from my uid`() = runTest(main.dispatcher) {
        profiles.progressions.value = profiles.progressions.value.map { it.copy(authorId = "me") }
        val vm = ProfileViewModel(users, profiles, FakeFollowRepository(), library, saved, FakeSyncRepository(), main.dispatcher)
        advanceUntilIdle()
        assertEquals(3, vm.uiState.value.myProgressions.size)
    }

    @Test
    fun `edit profile validates then saves`() = runTest(main.dispatcher) {
        val vm = EditProfileViewModel(users, profiles)
        advanceUntilIdle()
        assertEquals("keane", vm.uiState.value.username)
        vm.onAction(EditProfileAction.UsernameChanged("Bad Name!"))
        vm.onAction(EditProfileAction.ToggleGenre("Jazz"))
        vm.onAction(EditProfileAction.Save)
        assertEquals(ProfileFieldError.USERNAME_INVALID, vm.uiState.value.usernameError)
        assertEquals(ProfileFieldError.TOO_FEW_GENRES, vm.uiState.value.genresError)
        assertNull(profiles.lastUpdate)

        vm.onAction(EditProfileAction.UsernameChanged("keane.w"))
        vm.onAction(EditProfileAction.ToggleGenre("Rock"))
        vm.onAction(EditProfileAction.ToggleInstrument(Instrument.PIANO))
        vm.onAction(EditProfileAction.Save)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isSaved)
        assertEquals(setOf("Pop", "Lo-fi", "Rock"), profiles.lastUpdate?.genres)
        assertEquals(setOf(Instrument.PIANO), profiles.lastUpdate?.instruments)
    }

    @Test
    fun `edit profile uploads a photo`() = runTest(main.dispatcher) {
        val vm = EditProfileViewModel(users, profiles)
        advanceUntilIdle()
        vm.onAction(EditProfileAction.PhotoPicked("content://photo/1"))
        advanceUntilIdle()
        assertTrue(vm.uiState.value.photoUrl!!.startsWith("data:image/jpeg"))
        assertFalse(vm.uiState.value.isUploadingPhoto)
    }

    @Test
    fun `new progression validates, keeps a draft and uploads`() = runTest(main.dispatcher) {
        val progressions = FakeProgressionRepository(null)
        val drafts = FakeDraftRepository()
        val player = RecordingTonePlayer()
        val vm = NewProgressionViewModel(progressions, drafts, player, main.dispatcher)
        advanceUntilIdle()
        vm.onAction(NewProgressionAction.Upload)
        assertEquals(setOf(DraftError.TITLE_REQUIRED, DraftError.TOO_FEW_CHORDS), vm.uiState.value.errors)

        vm.onAction(NewProgressionAction.TitleChanged("Sunday loop"))
        vm.onAction(NewProgressionAction.KeyChanged("G"))
        vm.onAction(NewProgressionAction.AddChord("G"))
        vm.onAction(NewProgressionAction.AddChord("E♭maj7"))
        vm.onAction(NewProgressionAction.AddChord("nope"))
        vm.onAction(NewProgressionAction.AddChord("C"))
        vm.onAction(NewProgressionAction.RemoveChord(1))
        vm.onAction(NewProgressionAction.DifficultyChanged(Difficulty.INTERMEDIATE))
        advanceUntilIdle()
        assertEquals(listOf("G", "C"), vm.uiState.value.chords)
        assertEquals("Sunday loop", drafts.draft.value?.title)
        assertEquals(DiatonicChords.of("G"), vm.uiState.value.suggestedChords)

        vm.onAction(NewProgressionAction.TogglePreview)
        advanceUntilIdle()
        assertEquals(2, player.chords.size)

        vm.onAction(NewProgressionAction.Upload)
        advanceUntilIdle()
        val id = vm.uiState.value.uploadedId!!
        assertEquals(listOf("G", "C"), progressions.progressions.value.getValue(id).chords)
        assertNull(drafts.draft.value)
        assertEquals("", vm.uiState.value.title)
    }

    @Test
    fun `new progression restores the saved draft`() = runTest(main.dispatcher) {
        val drafts = FakeDraftRepository()
        drafts.saveDraft(ProgressionDraft("Half done", "Am", 70, "Jazz", Difficulty.EASY, listOf("Am", "Dm")))
        val vm = NewProgressionViewModel(FakeProgressionRepository(null), drafts, RecordingTonePlayer(), main.dispatcher)
        advanceUntilIdle()
        assertEquals("Half done", vm.uiState.value.title)
        assertEquals("70", vm.uiState.value.bpm)
        assertEquals(listOf("Am", "Dm"), vm.uiState.value.chords)
    }

    @Test
    fun `diatonic chords follow the key`() {
        assertEquals(listOf("C", "Dm", "Em", "F", "G", "Am", "Bdim"), DiatonicChords.of("C", withSevenths = false))
        assertEquals(listOf("Am", "Bdim", "C", "Dm", "Em", "F", "G"), DiatonicChords.of("Am", withSevenths = false))
        assertEquals("G7", DiatonicChords.of("C")[11])
        assertEquals("Bbmaj7", DiatonicChords.of("F")[10])
    }
}
