package com.example.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.data.preferences.UserSettings
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Schedules battery-friendly WorkManager reminders without intrusive exact alarms.
 */
object ReminderScheduler {

    private const val WORK_EVENING = "savior_work_evening_plan"
    private const val WORK_RUNNING_LOW = "savior_work_running_low"
    private const val WORK_WEEKLY_SPENDING = "savior_work_weekly_spending"
    private const val WORK_WEEKLY_REFLECTION = "savior_work_weekly_reflection"

    fun syncSchedules(context: Context, settings: UserSettings) {
        val workManager = runCatching { WorkManager.getInstance(context) }.getOrNull() ?: return

        if (settings.eveningReminderEnabled) {
            val delayMs = computeInitialDelayMillis(
                targetHour = settings.eveningReminderHour,
                targetMinute = settings.eveningReminderMinute
            )
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(ReminderWorker.KEY_KIND to ReminderWorker.KIND_EVENING_PLAN))
                .build()
            workManager.enqueueUniquePeriodicWork(
                WORK_EVENING,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        } else {
            workManager.cancelUniqueWork(WORK_EVENING)
        }

        if (settings.runningLowReminderEnabled) {
            val delayMs = computeInitialDelayMillis(targetHour = 18, targetMinute = 0)
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(ReminderWorker.KEY_KIND to ReminderWorker.KIND_RUNNING_LOW))
                .build()
            workManager.enqueueUniquePeriodicWork(
                WORK_RUNNING_LOW,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        } else {
            workManager.cancelUniqueWork(WORK_RUNNING_LOW)
        }

        if (settings.weeklySpendingReminderEnabled) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(computeInitialDelayMillis(targetHour = 10, targetMinute = 30), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(ReminderWorker.KEY_KIND to ReminderWorker.KIND_WEEKLY_SPENDING))
                .build()
            workManager.enqueueUniquePeriodicWork(
                WORK_WEEKLY_SPENDING,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        } else {
            workManager.cancelUniqueWork(WORK_WEEKLY_SPENDING)
        }

        if (settings.weeklyReflectionReminderEnabled) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(computeInitialDelayMillis(targetHour = 19, targetMinute = 0), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(ReminderWorker.KEY_KIND to ReminderWorker.KIND_WEEKLY_REFLECTION))
                .build()
            workManager.enqueueUniquePeriodicWork(
                WORK_WEEKLY_REFLECTION,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        } else {
            workManager.cancelUniqueWork(WORK_WEEKLY_REFLECTION)
        }
    }

    fun computeInitialDelayMillis(
        targetHour: Int,
        targetMinute: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val next = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, targetHour.coerceIn(0, 23))
            set(Calendar.MINUTE, targetMinute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= nowMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return (next.timeInMillis - now.timeInMillis).coerceAtLeast(60_000L)
    }
}
