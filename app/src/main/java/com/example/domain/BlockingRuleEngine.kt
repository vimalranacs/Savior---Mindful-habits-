package com.example.domain

import com.example.data.local.BlockingScheduleEntity
import com.example.data.local.RestrictedAppEntity
import com.example.data.local.ScheduleMode
import java.util.Calendar
import java.util.TimeZone

data class BlockingEvaluation(
    val shouldInterrupt: Boolean,
    val reason: String,
    val matchedScheduleLabel: String? = null
)

/**
 * Pure deterministic engine evaluating whether opening a given package should trigger
 * the calm Pause interruption screen at the given timestamp.
 */
object BlockingRuleEngine {

    /**
     * Converts java.util.Calendar day of week (SUNDAY=1..SATURDAY=7) to bit index (Monday=0..Sunday=6).
     */
    fun calendarDayToBitIndex(calendarDayOfWeek: Int): Int = when (calendarDayOfWeek) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        Calendar.SUNDAY -> 6
        else -> 0
    }

    fun isDayIncluded(daysMask: Int, dayBitIndex: Int): Boolean {
        return (daysMask and (1 shl dayBitIndex)) != 0
    }

    fun isMinuteWithinWindow(currentMinute: Int, startMinute: Int, endMinute: Int): Boolean {
        return if (startMinute <= endMinute) {
            currentMinute in startMinute..endMinute
        } else {
            // Overnight window (e.g., 21:00 to 07:00)
            currentMinute >= startMinute || currentMinute <= endMinute
        }
    }

    fun isScheduleActiveAt(
        schedule: BlockingScheduleEntity,
        nowEpochMs: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Boolean {
        if (!schedule.isEnabled) return false
        val cal = Calendar.getInstance(timeZone).apply { timeInMillis = nowEpochMs }
        val dayBit = calendarDayToBitIndex(cal.get(Calendar.DAY_OF_WEEK))
        if (!isDayIncluded(schedule.daysMask, dayBit)) return false

        val minuteOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        return when (schedule.mode) {
            ScheduleMode.ALWAYS -> true
            ScheduleMode.EVENING_CUTOFF -> minuteOfDay >= schedule.startMinuteOfDay
            ScheduleMode.TIME_WINDOW, ScheduleMode.WORK_STUDY ->
                isMinuteWithinWindow(minuteOfDay, schedule.startMinuteOfDay, schedule.endMinuteOfDay)
            else -> true
        }
    }

    fun evaluate(
        packageName: String,
        restrictedApps: List<RestrictedAppEntity>,
        schedules: List<BlockingScheduleEntity>,
        globalPauseUntilEpochMs: Long,
        nowEpochMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): BlockingEvaluation {
        if (globalPauseUntilEpochMs > nowEpochMs) {
            return BlockingEvaluation(
                shouldInterrupt = false,
                reason = "All restrictions are temporarily paused."
            )
        }

        val target = restrictedApps.firstOrNull { it.packageName == packageName }
            ?: return BlockingEvaluation(
                shouldInterrupt = false,
                reason = "Application is not selected for mindful pausing."
            )

        if (!target.isEnabled) {
            return BlockingEvaluation(
                shouldInterrupt = false,
                reason = "Restriction for ${target.displayName} is turned off."
            )
        }

        if (target.unlockedUntilEpochMs > nowEpochMs) {
            return BlockingEvaluation(
                shouldInterrupt = false,
                reason = "${target.displayName} is temporarily unlocked."
            )
        }

        val enabledSchedules = schedules.filter { it.isEnabled }
        if (enabledSchedules.isEmpty()) {
            return BlockingEvaluation(
                shouldInterrupt = false,
                reason = "No active blocking schedules."
            )
        }

        val activeSchedule = enabledSchedules.firstOrNull {
            isScheduleActiveAt(it, nowEpochMs, timeZone)
        }

        return if (activeSchedule != null) {
            BlockingEvaluation(
                shouldInterrupt = true,
                reason = "Active under schedule: ${activeSchedule.label}",
                matchedScheduleLabel = activeSchedule.label
            )
        } else {
            BlockingEvaluation(
                shouldInterrupt = false,
                reason = "Outside configured blocking schedule."
            )
        }
    }

    fun computeEndOfDayEpochMs(
        nowEpochMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowEpochMs
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    fun formatMinutesOfDay(minuteOfDay: Int): String {
        val clamped = minuteOfDay.coerceIn(0, 1439)
        val hour24 = clamped / 60
        val minute = clamped % 60
        val suffix = if (hour24 < 12) "AM" else "PM"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return "%d:%02d %s".format(hour12, minute, suffix)
    }
}
