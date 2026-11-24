package com.example

import android.app.Application
import com.example.data.SaviorContainer
import com.example.notifications.NotificationChannels
import com.example.notifications.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SaviorApplication : Application() {

    lateinit var container: SaviorContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = SaviorContainer(this)
        NotificationChannels.createAll(this)

        appScope.launch {
            container.database.ensureInitialDefaults()
            val settings = container.preferencesRepository.getSettingsOnce()
            ReminderScheduler.syncSchedules(this@SaviorApplication, settings)
        }
    }
}
