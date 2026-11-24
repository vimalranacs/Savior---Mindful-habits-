package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.SaviorApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Gracefully restores local WorkManager reminder schedules after device restart or app update.
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? SaviorApplication
                if (app != null) {
                    val settings = app.container.preferencesRepository.getSettingsOnce()
                    NotificationChannels.createAll(context)
                    ReminderScheduler.syncSchedules(context, settings)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
