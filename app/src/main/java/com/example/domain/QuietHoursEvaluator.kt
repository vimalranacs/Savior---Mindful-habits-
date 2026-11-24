package com.example.domain

/**
 * Evaluates quiet/sleep hours and daily notification frequency caps so Savior never interrupts sleep
 * or sends repetitive reminders.
 */
object QuietHoursEvaluator {

    fun isInQuietHours(
        currentHour: Int,
        quietHoursEnabled: Boolean,
        quietStartHour: Int,
        quietEndHour: Int
    ): Boolean {
        if (!quietHoursEnabled) return false
        val hour = currentHour.coerceIn(0, 23)
        val start = quietStartHour.coerceIn(0, 23)
        val end = quietEndHour.coerceIn(0, 23)
        if (start == end) return false
        return if (start < end) {
            hour in start until end
        } else {
            // Overnight quiet hours (e.g., 22:00 to 07:00)
            hour >= start || hour < end
        }
    }

    fun canSendNotification(
        currentHour: Int,
        quietHoursEnabled: Boolean,
        quietStartHour: Int,
        quietEndHour: Int,
        notificationsSentToday: Int,
        lastNotificationDateKey: String,
        todayDateKey: String,
        dailyCap: Int
    ): Boolean {
        if (isInQuietHours(currentHour, quietHoursEnabled, quietStartHour, quietEndHour)) {
            return false
        }
        val sentToday = if (lastNotificationDateKey == todayDateKey) notificationsSentToday else 0
        return sentToday < dailyCap.coerceAtLeast(1)
    }
}
