package edu.bluejack26_1.wakord.domain.repository

import edu.bluejack26_1.wakord.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    val referencePitch: Flow<Float>
    val pushEnabled: Flow<Boolean>
    val keepOffline: Flow<Boolean>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setReferencePitch(hz: Float)
    suspend fun setPushEnabled(enabled: Boolean)
    suspend fun setKeepOffline(enabled: Boolean)
}
