package edu.bluejack26_1.wakord.data.repository

import edu.bluejack26_1.wakord.data.local.SettingsDao
import edu.bluejack26_1.wakord.data.local.SettingsEntity
import edu.bluejack26_1.wakord.domain.model.ThemeMode
import edu.bluejack26_1.wakord.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class RoomSettingsRepository(private val dao: SettingsDao) : SettingsRepository {

    private val settings: Flow<SettingsEntity> = dao.observe().map { it ?: SettingsEntity() }

    override val themeMode: Flow<ThemeMode> = settings
        .map { entity -> ThemeMode.entries.firstOrNull { it.name == entity.themeMode } ?: ThemeMode.SYSTEM }
        .distinctUntilChanged()

    override val referencePitch: Flow<Float> = settings.map { it.referencePitch }.distinctUntilChanged()

    override val pushEnabled: Flow<Boolean> = settings.map { it.pushEnabled }.distinctUntilChanged()

    override val keepOffline: Flow<Boolean> = settings.map { it.keepOffline }.distinctUntilChanged()

    override suspend fun setThemeMode(mode: ThemeMode) = update { it.copy(themeMode = mode.name) }

    override suspend fun setReferencePitch(hz: Float) = update { it.copy(referencePitch = hz.coerceIn(415f, 466f)) }

    override suspend fun setPushEnabled(enabled: Boolean) = update { it.copy(pushEnabled = enabled) }

    override suspend fun setKeepOffline(enabled: Boolean) = update { it.copy(keepOffline = enabled) }

    private suspend fun update(change: (SettingsEntity) -> SettingsEntity) {
        dao.upsert(change(dao.get() ?: SettingsEntity()))
    }
}
