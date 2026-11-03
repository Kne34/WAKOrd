package edu.bluejack26_1.wakord.core.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import edu.bluejack26_1.wakord.core.analysis.AlgorithmChordAnalyzer
import edu.bluejack26_1.wakord.core.analysis.FallbackChordAnalyzer
import edu.bluejack26_1.wakord.core.audio.AndroidAudioCapture
import edu.bluejack26_1.wakord.core.audio.AudioCapture
import edu.bluejack26_1.wakord.core.audio.AudioInput
import edu.bluejack26_1.wakord.core.audio.MicAudioInput
import edu.bluejack26_1.wakord.core.audio.PitchDetector
import edu.bluejack26_1.wakord.core.audio.PlaybackController
import edu.bluejack26_1.wakord.core.audio.SynthTonePlayer
import edu.bluejack26_1.wakord.core.audio.TonePlaybackController
import edu.bluejack26_1.wakord.core.audio.TonePlayer
import edu.bluejack26_1.wakord.core.audio.YinPitchDetector
import edu.bluejack26_1.wakord.core.network.AndroidConnectivityObserver
import edu.bluejack26_1.wakord.core.network.ConnectivityObserver
import edu.bluejack26_1.wakord.core.notification.NotificationWatcher
import edu.bluejack26_1.wakord.core.training.MicPitchSource
import edu.bluejack26_1.wakord.core.training.PitchSource
import edu.bluejack26_1.wakord.data.local.AppDatabase
import edu.bluejack26_1.wakord.data.remote.GeminiChordAnalyzer
import edu.bluejack26_1.wakord.data.remote.FirebaseRemoteDatabase
import edu.bluejack26_1.wakord.data.remote.GeminiCommentModerator
import edu.bluejack26_1.wakord.data.remote.firebaseCall
import edu.bluejack26_1.wakord.data.remote.uidFlow
import edu.bluejack26_1.wakord.data.repository.CachedUserPhotoRepository
import edu.bluejack26_1.wakord.data.repository.FirebaseAuthRepository
import edu.bluejack26_1.wakord.data.repository.FirebaseCommentRepository
import edu.bluejack26_1.wakord.data.repository.FirebaseExploreRepository
import edu.bluejack26_1.wakord.data.repository.FirebaseFeedRepository
import edu.bluejack26_1.wakord.data.repository.FirebaseFollowRepository
import edu.bluejack26_1.wakord.data.repository.FirebaseNotificationRepository
import edu.bluejack26_1.wakord.data.repository.FirebaseProfileRepository
import edu.bluejack26_1.wakord.data.repository.FirebaseProgressionRepository
import edu.bluejack26_1.wakord.data.repository.FirebaseSavedItemsRemote
import edu.bluejack26_1.wakord.data.repository.FirebaseSongRepository
import edu.bluejack26_1.wakord.data.repository.FirebaseSyncRepository
import edu.bluejack26_1.wakord.data.repository.FirebaseTuningRepository
import edu.bluejack26_1.wakord.data.repository.FirebaseUserRepository
import edu.bluejack26_1.wakord.data.repository.OfflineSavedItemsRepository
import edu.bluejack26_1.wakord.data.repository.RoomAnalysisRepository
import edu.bluejack26_1.wakord.data.repository.RoomDraftRepository
import edu.bluejack26_1.wakord.data.repository.RoomSettingsRepository
import edu.bluejack26_1.wakord.data.repository.RoomTrainingRepository
import edu.bluejack26_1.wakord.data.repository.SyncScheduler
import edu.bluejack26_1.wakord.data.sync.FirebaseBackup
import edu.bluejack26_1.wakord.data.sync.UserDataBackup
import edu.bluejack26_1.wakord.data.sync.WorkManagerSyncScheduler
import edu.bluejack26_1.wakord.domain.repository.AnalysisRepository
import edu.bluejack26_1.wakord.domain.repository.AuthRepository
import edu.bluejack26_1.wakord.domain.repository.ChordAnalyzer
import edu.bluejack26_1.wakord.domain.repository.CommentRepository
import edu.bluejack26_1.wakord.domain.repository.DraftRepository
import edu.bluejack26_1.wakord.domain.repository.ExploreRepository
import edu.bluejack26_1.wakord.domain.repository.FeedRepository
import edu.bluejack26_1.wakord.domain.repository.FollowRepository
import edu.bluejack26_1.wakord.domain.repository.NotificationRepository
import edu.bluejack26_1.wakord.domain.repository.ProfileRepository
import edu.bluejack26_1.wakord.domain.repository.ProgressionRepository
import edu.bluejack26_1.wakord.domain.repository.SavedItemsRepository
import edu.bluejack26_1.wakord.domain.repository.SavedLibraryRepository
import edu.bluejack26_1.wakord.domain.repository.SettingsRepository
import edu.bluejack26_1.wakord.domain.repository.SongRepository
import edu.bluejack26_1.wakord.domain.repository.SyncRepository
import edu.bluejack26_1.wakord.domain.repository.TrainingRepository
import edu.bluejack26_1.wakord.domain.repository.TuningRepository
import edu.bluejack26_1.wakord.domain.repository.UserPhotoRepository
import edu.bluejack26_1.wakord.domain.repository.UserRepository
import edu.bluejack26_1.wakord.domain.validation.AuthValidator
import edu.bluejack26_1.wakord.domain.validation.DefaultAuthValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database: AppDatabase by lazy { AppDatabase.build(appContext) }
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firebaseDatabase: FirebaseDatabase by lazy { FirebaseDatabase.getInstance().apply { setPersistenceEnabled(true) } }
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val connectivityObserver: ConnectivityObserver by lazy { AndroidConnectivityObserver(appContext) }
    private val isOnline: StateFlow<Boolean> by lazy { connectivityObserver.isOnline.stateIn(appScope, SharingStarted.Eagerly, true) }
    val firebaseBackup: FirebaseBackup by lazy {
        FirebaseBackup(
            remote = FirebaseRemoteDatabase(firebaseDatabase),
            dao = database.backupDao(),
            isOnline = isOnline,
            userId = firebaseAuth.uidFlow(),
            scheduler = syncScheduler,
        )
    }
    val settingsRepository: SettingsRepository by lazy { RoomSettingsRepository(database.settingsDao()) }
    val tonePlayer: TonePlayer by lazy { SynthTonePlayer() }
    val audioInput: AudioInput by lazy { MicAudioInput() }
    val pitchDetector: PitchDetector by lazy { YinPitchDetector() }
    val pitchSource: PitchSource by lazy { MicPitchSource(appContext, audioInput, pitchDetector) }
    val authRepository: AuthRepository by lazy { FirebaseAuthRepository(firebaseAuth, firebaseBackup) }
    val songRepository: SongRepository by lazy { FirebaseSongRepository(firebaseAuth, firebaseBackup) }
    val commentRepository: CommentRepository by lazy { FirebaseCommentRepository(firebaseAuth, firebaseBackup, GeminiCommentModerator()) }
    private val offlineSaved: OfflineSavedItemsRepository by lazy {
        OfflineSavedItemsRepository(database.savedItemDao(), FirebaseSavedItemsRemote(firebaseAuth, firebaseBackup), syncScheduler)
    }
    val syncScheduler: SyncScheduler by lazy { WorkManagerSyncScheduler(appContext) }
    val savedItemsRepository: SavedItemsRepository get() = offlineSaved
    val savedLibraryRepository: SavedLibraryRepository get() = offlineSaved
    val progressionRepository: ProgressionRepository by lazy { FirebaseProgressionRepository(firebaseAuth, firebaseBackup) }
    val userRepository: UserRepository by lazy { FirebaseUserRepository(firebaseAuth, firebaseBackup) }
    val feedRepository: FeedRepository by lazy { FirebaseFeedRepository(firebaseAuth, firebaseBackup) }
    val playbackController: PlaybackController by lazy { TonePlaybackController(tonePlayer, appScope) }
    val exploreRepository: ExploreRepository by lazy { FirebaseExploreRepository(firebaseBackup) }
    val trainingRepository: TrainingRepository by lazy { RoomTrainingRepository(database.trainingDao(), firebaseAuth, firebaseBackup) }
    val analysisRepository: AnalysisRepository by lazy { RoomAnalysisRepository(database.analysisDao(), firebaseAuth, firebaseBackup) }
    val audioCapture: AudioCapture by lazy { AndroidAudioCapture(appContext) }
    val chordAnalyzer: ChordAnalyzer by lazy { FallbackChordAnalyzer(GeminiChordAnalyzer(), AlgorithmChordAnalyzer()) }
    val profileRepository: ProfileRepository by lazy { FirebaseProfileRepository(firebaseAuth, firebaseBackup, appContext.contentResolver) }
    val userPhotoRepository: UserPhotoRepository by lazy { CachedUserPhotoRepository(profileRepository, appScope) }
    val draftRepository: DraftRepository by lazy { RoomDraftRepository(database.draftDao(), firebaseAuth) }
    val followRepository: FollowRepository by lazy { FirebaseFollowRepository(firebaseAuth, firebaseBackup) }
    val notificationRepository: NotificationRepository by lazy { FirebaseNotificationRepository(firebaseAuth, firebaseBackup) }
    val notificationWatcher: NotificationWatcher by lazy { NotificationWatcher(appContext, notificationRepository, settingsRepository, appScope) }
    val authValidator: AuthValidator = DefaultAuthValidator()
    val tuningRepository: TuningRepository by lazy { FirebaseTuningRepository(firebaseAuth, firebaseBackup) }
    val syncRepository: SyncRepository = FirebaseSyncRepository(firebaseDatabase, authRepository, savedItemsRepository, settingsRepository, savedLibraryRepository, firebaseBackup, appScope)

    fun startSync() {
        appScope.launch {
            authRepository.currentUserId.filterNotNull().distinctUntilChanged().collect { syncScheduler.schedule() }
        }
        appScope.launch {
            isOnline.drop(1).filter { it }.collect {
                firebaseCall { firebaseBackup.flush() }
                savedLibraryRepository.syncNow()
            }
        }
    }

    suspend fun backUpUserData() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        UserDataBackup.run(firebaseBackup, uid)
    }
}
