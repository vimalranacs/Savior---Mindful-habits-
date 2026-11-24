package com.example.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.SaviorApplication
import com.example.domain.GrocerySuggestionEngine
import com.example.domain.QuietHoursEvaluator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val kind = inputData.getString(KEY_KIND) ?: return Result.success()
        val app = applicationContext as? SaviorApplication ?: return Result.success()
        val container = app.container
        val settings = container.preferencesRepository.getSettingsOnce()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        val now = Calendar.getInstance()
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now.timeInMillis))
        val allowedNow = QuietHoursEvaluator.canSendNotification(
            currentHour = now.get(Calendar.HOUR_OF_DAY),
            quietHoursEnabled = settings.quietHoursEnabled,
            quietStartHour = settings.quietStartHour,
            quietEndHour = settings.quietEndHour,
            notificationsSentToday = settings.notificationsSentToday,
            lastNotificationDateKey = settings.lastNotificationDateKey,
            todayDateKey = todayKey,
            dailyCap = settings.dailyNotificationCap
        )
        if (!allowedNow) return Result.success()

        NotificationChannels.createAll(applicationContext)

        when (kind) {
            KIND_EVENING_PLAN -> {
                if (!settings.eveningReminderEnabled) return Result.success()
                postNotification(
                    id = 1001,
                    channelId = NotificationChannels.CHANNEL_EVENING_PLANNING,
                    title = settings.eveningReminderTitle,
                    body = settings.eveningReminderBody,
                    destination = NotificationChannels.DEST_EVENING_PLAN
                )
                container.preferencesRepository.recordNotificationSent(todayKey)
            }

            KIND_RUNNING_LOW -> {
                if (!settings.runningLowReminderEnabled) return Result.success()
                val items = container.database.groceryDao().getAllItemsOnce()
                val dueItems = items.filter { item ->
                    item.reminderEnabled && GrocerySuggestionEngine.isDueForReplenishment(item)
                }
                if (dueItems.isEmpty()) return Result.success()
                val names = dueItems.take(3).joinToString(", ") { it.name }
                postNotification(
                    id = 1002,
                    channelId = NotificationChannels.CHANNEL_RUNNING_LOW,
                    title = "Items to check in your kitchen",
                    body = "${settings.runningLowReminderBody} ($names)",
                    destination = NotificationChannels.DEST_EVENING_PLAN
                )
                container.preferencesRepository.recordNotificationSent(todayKey)
            }

            KIND_WEEKLY_SPENDING -> {
                if (!settings.weeklySpendingReminderEnabled) return Result.success()
                postNotification(
                    id = 1003,
                    channelId = NotificationChannels.CHANNEL_WEEKLY_SPENDING,
                    title = "Weekly spending awareness",
                    body = settings.weeklySpendingReminderBody,
                    destination = NotificationChannels.DEST_INSIGHTS
                )
                container.preferencesRepository.recordNotificationSent(todayKey)
            }

            KIND_WEEKLY_REFLECTION -> {
                if (!settings.weeklyReflectionReminderEnabled) return Result.success()
                postNotification(
                    id = 1004,
                    channelId = NotificationChannels.CHANNEL_WEEKLY_REFLECTION,
                    title = "A quiet weekly reflection",
                    body = settings.weeklyReflectionReminderBody,
                    destination = NotificationChannels.DEST_INSIGHTS
                )
                container.preferencesRepository.recordNotificationSent(todayKey)
            }
        }

        return Result.success()
    }

    private fun postNotification(
        id: Int,
        channelId: String,
        title: String,
        body: String,
        destination: String
    ) {
        val openIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NotificationChannels.EXTRA_OPEN_DESTINATION, destination)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            id,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        runCatching {
            NotificationManagerCompat.from(applicationContext).notify(id, notification)
        }
    }

    companion object {
        const val KEY_KIND = "reminder_kind"
        const val KIND_EVENING_PLAN = "evening_plan"
        const val KIND_RUNNING_LOW = "running_low"
        const val KIND_WEEKLY_SPENDING = "weekly_spending"
        const val KIND_WEEKLY_REFLECTION = "weekly_reflection"
    }
}
