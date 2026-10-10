package edu.bluejack26_1.wakord

import android.app.Application
import edu.bluejack26_1.wakord.core.di.AppContainer

class WAKordApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.syncScheduler.schedule()
    }
}
