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
import edu.bluejack26_1.wakord.data.remote.GeminiCommentModerator
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

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database: AppDatabase by lazy { AppDatabase.build(appContext) }
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firebaseDatabase: FirebaseDatabase by lazy { FirebaseDatabase.getInstance().apply { setPersistenceEnabled(true) } }
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val connectivityObserver: ConnectivityObserver by lazy { AndroidConnectivityObserver(appContext) }
    val settingsRepository: SettingsRepository by lazy { RoomSettingsRepository(database.settingsDao()) }
    val tonePlayer: TonePlayer by lazy { SynthTonePlayer() }
    val audioInput: AudioInput by lazy { MicAudioInput() }
    val pitchDetector: PitchDetector by lazy { YinPitchDetector() }
    val pitchSource: PitchSource by lazy { MicPitchSource(appContext, audioInput, pitchDetector) }
    val authRepository: AuthRepository by lazy { FirebaseAuthRepository(firebaseAuth, firebaseDatabase) }
    val songRepository: SongRepository by lazy { FirebaseSongRepository(firebaseAuth, firebaseDatabase) }
    val commentRepository: CommentRepository by lazy { FirebaseCommentRepository(firebaseAuth, firebaseDatabase, GeminiCommentModerator()) }
    private val offlineSaved: OfflineSavedItemsRepository by lazy {
        OfflineSavedItemsRepository(database.savedItemDao(), FirebaseSavedItemsRemote(firebaseAuth, firebaseDatabase), syncScheduler)
    }
    val syncScheduler: SyncScheduler by lazy { WorkManagerSyncScheduler(appContext) }
    val savedItemsRepository: SavedItemsRepository get() = offlineSaved
    val savedLibraryRepository: SavedLibraryRepository get() = offlineSaved
    val progressionRepository: ProgressionRepository by lazy { FirebaseProgressionRepository(firebaseAuth, firebaseDatabase) }
    val userRepository: UserRepository by lazy { FirebaseUserRepository(firebaseAuth, firebaseDatabase) }
    val feedRepository: FeedRepository by lazy { FirebaseFeedRepository(firebaseAuth, firebaseDatabase) }
    val playbackController: PlaybackController by lazy { TonePlaybackController(tonePlayer, appScope) }
    val exploreRepository: ExploreRepository by lazy { FirebaseExploreRepository(firebaseDatabase) }
    val trainingRepository: TrainingRepository by lazy { RoomTrainingRepository(database.trainingDao(), firebaseAuth, firebaseDatabase) }
    val analysisRepository: AnalysisRepository by lazy { RoomAnalysisRepository(database.analysisDao(), firebaseAuth, firebaseDatabase) }
    val audioCapture: AudioCapture by lazy { AndroidAudioCapture(appContext) }
    val chordAnalyzer: ChordAnalyzer by lazy { FallbackChordAnalyzer(GeminiChordAnalyzer(), AlgorithmChordAnalyzer()) }
    val profileRepository: ProfileRepository by lazy { FirebaseProfileRepository(firebaseAuth, firebaseDatabase, appContext.contentResolver) }
    val userPhotoRepository: UserPhotoRepository by lazy { CachedUserPhotoRepository(profileRepository, appScope) }
    val draftRepository: DraftRepository by lazy { RoomDraftRepository(database.draftDao(), firebaseAuth) }
    val followRepository: FollowRepository by lazy { FirebaseFollowRepository(firebaseAuth, firebaseDatabase) }
    val notificationRepository: NotificationRepository by lazy { FirebaseNotificationRepository(firebaseAuth, firebaseDatabase) }
    val notificationWatcher: NotificationWatcher by lazy { NotificationWatcher(appContext, notificationRepository, settingsRepository, appScope) }
    val authValidator: AuthValidator = DefaultAuthValidator()
    val tuningRepository: TuningRepository by lazy { FirebaseTuningRepository(firebaseAuth, firebaseDatabase) }
    val syncRepository: SyncRepository = FirebaseSyncRepository(firebaseDatabase, authRepository, savedItemsRepository, settingsRepository, savedLibraryRepository, appScope)
}
