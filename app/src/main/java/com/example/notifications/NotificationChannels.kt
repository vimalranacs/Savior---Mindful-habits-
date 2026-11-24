package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.R

object NotificationChannels {
    const val CHANNEL_EVENING_PLANNING = "savior_evening_planning"
    const val CHANNEL_RUNNING_LOW = "savior_running_low"
    const val CHANNEL_WEEKLY_SPENDING = "savior_weekly_spending"
    const val CHANNEL_WEEKLY_REFLECTION = "savior_weekly_reflection"

    const val EXTRA_OPEN_DESTINATION = "savior_open_destination"
    const val DEST_EVENING_PLAN = "evening_plan"
    const val DEST_INSIGHTS = "insights"

    fun createAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val channels = listOf(
            NotificationChannel(
                CHANNEL_EVENING_PLANNING,
                context.getString(R.string.channel_evening_planning_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.channel_evening_planning_desc)
            },
            NotificationChannel(
                CHANNEL_RUNNING_LOW,
                context.getString(R.string.channel_running_low_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.channel_running_low_desc)
            },
            NotificationChannel(
                CHANNEL_WEEKLY_SPENDING,
                context.getString(R.string.channel_weekly_spending_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.channel_weekly_spending_desc)
            },
            NotificationChannel(
                CHANNEL_WEEKLY_REFLECTION,
                context.getString(R.string.channel_weekly_reflection_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.channel_weekly_reflection_desc)
            }
        )
        manager.createNotificationChannels(channels)
    }
}
