package edu.bluejack26_1.wakord.core.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import edu.bluejack26_1.wakord.core.audio.AudioInput
import edu.bluejack26_1.wakord.core.audio.MicAudioInput
import edu.bluejack26_1.wakord.core.audio.PitchDetector
import edu.bluejack26_1.wakord.core.audio.SynthTonePlayer
import edu.bluejack26_1.wakord.core.audio.TonePlayer
import edu.bluejack26_1.wakord.core.audio.YinPitchDetector
import edu.bluejack26_1.wakord.core.network.AndroidConnectivityObserver
import edu.bluejack26_1.wakord.core.network.ConnectivityObserver
import edu.bluejack26_1.wakord.data.local.AppDatabase
import edu.bluejack26_1.wakord.data.repository.FirebaseAuthRepository
import edu.bluejack26_1.wakord.data.repository.FirebaseTuningRepository
import edu.bluejack26_1.wakord.data.repository.RoomSettingsRepository
import edu.bluejack26_1.wakord.domain.repository.AuthRepository
import edu.bluejack26_1.wakord.domain.repository.SettingsRepository
import edu.bluejack26_1.wakord.domain.repository.TuningRepository
import edu.bluejack26_1.wakord.domain.validation.AuthValidator
import edu.bluejack26_1.wakord.domain.validation.DefaultAuthValidator

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database: AppDatabase by lazy { AppDatabase.build(appContext) }
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firebaseDatabase: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }

    val connectivityObserver: ConnectivityObserver by lazy { AndroidConnectivityObserver(appContext) }
    val settingsRepository: SettingsRepository by lazy { RoomSettingsRepository(database.settingsDao()) }
    val tonePlayer: TonePlayer by lazy { SynthTonePlayer() }
    val audioInput: AudioInput by lazy { MicAudioInput() }
    val pitchDetector: PitchDetector by lazy { YinPitchDetector() }
    val authRepository: AuthRepository by lazy { FirebaseAuthRepository(firebaseAuth, firebaseDatabase) }
    val authValidator: AuthValidator = DefaultAuthValidator()
    val tuningRepository: TuningRepository by lazy { FirebaseTuningRepository(firebaseAuth, firebaseDatabase) }
}
