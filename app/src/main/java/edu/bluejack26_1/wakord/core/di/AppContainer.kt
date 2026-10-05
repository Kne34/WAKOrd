package edu.bluejack26_1.wakord.core.di

import android.content.Context
import edu.bluejack26_1.wakord.core.network.AndroidConnectivityObserver
import edu.bluejack26_1.wakord.core.network.ConnectivityObserver
import edu.bluejack26_1.wakord.data.local.AppDatabase
import edu.bluejack26_1.wakord.data.repository.RoomSettingsRepository
import edu.bluejack26_1.wakord.domain.repository.SettingsRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database: AppDatabase by lazy { AppDatabase.build(appContext) }

    val connectivityObserver: ConnectivityObserver by lazy { AndroidConnectivityObserver(appContext) }
    val settingsRepository: SettingsRepository by lazy { RoomSettingsRepository(database.settingsDao()) }
}
