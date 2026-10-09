package edu.bluejack26_1.wakord.data.fake

import edu.bluejack26_1.wakord.domain.model.ThemeMode
import edu.bluejack26_1.wakord.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsRepository : SettingsRepository {
    override val themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    override val referencePitch = MutableStateFlow(440f)
    override val pushEnabled = MutableStateFlow(true)
    override val keepOffline = MutableStateFlow(true)

    override suspend fun setThemeMode(mode: ThemeMode) {
        themeMode.value = mode
    }

    override suspend fun setReferencePitch(hz: Float) {
        referencePitch.value = hz
    }

    override suspend fun setPushEnabled(enabled: Boolean) {
        pushEnabled.value = enabled
    }

    override suspend fun setKeepOffline(enabled: Boolean) {
        keepOffline.value = enabled
    }
}
