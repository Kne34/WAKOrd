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
import edu.bluejack26_1.wakord.data.local.AppDatabase
import edu.bluejack26_1.wakord.data.remote.GeminiChordAnalyzer
import edu.bluejack26_1.wakord.data.repository.FirebaseAuthRepository
import edu.bluejack26_1.wakord.data.repository.FirebaseCommentRepository
import edu.bluejack26_1.wakord.data.repository.FirebaseExploreRepository
import edu.bluejack26_1.wakord.data.repository.FirebaseFeedRepository
import edu.bluejack26_1.wakord.data.repository.FirebaseProgressionRepository
import edu.bluejack26_1.wakord.data.repository.FirebaseSavedItemsRepository
import edu.bluejack26_1.wakord.data.repository.FirebaseSongRepository
import edu.bluejack26_1.wakord.data.repository.FirebaseSyncRepository
import edu.bluejack26_1.wakord.data.repository.FirebaseTuningRepository
import edu.bluejack26_1.wakord.data.repository.FirebaseUserRepository
import edu.bluejack26_1.wakord.data.repository.RoomAnalysisRepository
import edu.bluejack26_1.wakord.data.repository.RoomSettingsRepository
import edu.bluejack26_1.wakord.data.repository.RoomTrainingRepository
import edu.bluejack26_1.wakord.domain.repository.AnalysisRepository
import edu.bluejack26_1.wakord.domain.repository.AuthRepository
import edu.bluejack26_1.wakord.domain.repository.ChordAnalyzer
import edu.bluejack26_1.wakord.domain.repository.CommentRepository
import edu.bluejack26_1.wakord.domain.repository.ExploreRepository
import edu.bluejack26_1.wakord.domain.repository.FeedRepository
import edu.bluejack26_1.wakord.domain.repository.ProgressionRepository
import edu.bluejack26_1.wakord.domain.repository.SavedItemsRepository
import edu.bluejack26_1.wakord.domain.repository.SettingsRepository
import edu.bluejack26_1.wakord.domain.repository.SongRepository
import edu.bluejack26_1.wakord.domain.repository.SyncRepository
import edu.bluejack26_1.wakord.domain.repository.TrainingRepository
import edu.bluejack26_1.wakord.domain.repository.TuningRepository
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
    val authRepository: AuthRepository by lazy { FirebaseAuthRepository(firebaseAuth, firebaseDatabase) }
    val songRepository: SongRepository by lazy { FirebaseSongRepository(firebaseAuth, firebaseDatabase) }
    val commentRepository: CommentRepository by lazy { FirebaseCommentRepository(firebaseAuth, firebaseDatabase) }
    val savedItemsRepository: SavedItemsRepository by lazy { FirebaseSavedItemsRepository(firebaseAuth, firebaseDatabase) }
    val progressionRepository: ProgressionRepository by lazy { FirebaseProgressionRepository(firebaseAuth, firebaseDatabase) }
    val userRepository: UserRepository by lazy { FirebaseUserRepository(firebaseAuth, firebaseDatabase) }
    val feedRepository: FeedRepository by lazy { FirebaseFeedRepository(firebaseAuth, firebaseDatabase) }
    val playbackController: PlaybackController by lazy { TonePlaybackController(tonePlayer, appScope) }
    val exploreRepository: ExploreRepository by lazy { FirebaseExploreRepository(firebaseDatabase) }
    val trainingRepository: TrainingRepository by lazy { RoomTrainingRepository(database.trainingDao(), firebaseAuth, firebaseDatabase) }
    val analysisRepository: AnalysisRepository by lazy { RoomAnalysisRepository(database.analysisDao(), firebaseAuth, firebaseDatabase) }
    val audioCapture: AudioCapture by lazy { AndroidAudioCapture(appContext) }
    val chordAnalyzer: ChordAnalyzer by lazy { FallbackChordAnalyzer(GeminiChordAnalyzer(), AlgorithmChordAnalyzer()) }
    val authValidator: AuthValidator = DefaultAuthValidator()
    val tuningRepository: TuningRepository by lazy { FirebaseTuningRepository(firebaseAuth, firebaseDatabase) }
    val syncRepository: SyncRepository = FirebaseSyncRepository(firebaseDatabase, authRepository, savedItemsRepository, settingsRepository, appScope)
}
