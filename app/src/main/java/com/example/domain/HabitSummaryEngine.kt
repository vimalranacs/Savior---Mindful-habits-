package com.example.domain

import com.example.data.local.EveningPlanNoteEntity
import com.example.data.local.PauseAction
import com.example.data.local.PauseEventEntity
import com.example.data.local.SpendingEntryEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class PeriodHabitSummary(
    val windowLabel: String,
    val timesReturnedInsteadOfOrdering: Int,
    val timesWaitedAMinute: Int,
    val plannedShoppingSessions: Int,
    val daysFollowingRestrictions: Int,
    val recordedImpulsiveOrders: Int,
    val estimatedSpendingAvoided: Double,
    val encouragingNote: String
)

/**
 * Calm, streak-free personal habit awareness summaries.
 * Deliberately avoids competitive scores or failure-inducing streaks.
 */
object HabitSummaryEngine {

    fun summarizePeriod(
        windowLabel: String,
        daysWindow: Int,
        pauseEvents: List<PauseEventEntity>,
        eveningPlans: List<EveningPlanNoteEntity>,
        spendingEntries: List<SpendingEntryEntity>,
        nowEpochMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): PeriodHabitSummary {
        val cutoff = nowEpochMs - daysWindow.toLong() * 24L * 60L * 60L * 1000L
        val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            this.timeZone = timeZone
        }

        val recentPauses = pauseEvents.filter { it.timestamp >= cutoff }
        val returnedCount = recentPauses.count { it.action == PauseAction.RETURNED }
        val waitedCount = recentPauses.count { it.action == PauseAction.WAITED_MINUTE }

        val recentPlans = eveningPlans.filter { it.completedAt >= cutoff }
        val plannedSessions = recentPlans.map { it.dateKey }.distinct().size

        // Days where the user either paused and returned OR completed an evening plan without an override unlock
        val unlockedDayKeys = recentPauses
            .filter { it.action == PauseAction.UNLOCKED }
            .map { dateKeyFormat.format(Date(it.timestamp)) }
            .toSet()

        val mindfulActivityDays = (
            recentPauses.filter { it.action == PauseAction.RETURNED || it.action == PauseAction.WAITED_MINUTE }
                .map { dateKeyFormat.format(Date(it.timestamp)) } +
                recentPlans.map { it.dateKey }
            ).toSet()

        val daysFollowing = mindfulActivityDays.count { it !in unlockedDayKeys }

        val recentSpending = spendingEntries.filter { it.dateEpochMs >= cutoff }
        val impulsiveCount = recentSpending.count { !it.isAvoidedEstimate && it.isImpulsive }
        val avoidedAmount = recentSpending.filter { it.isAvoidedEstimate }.sumOf { it.amount }

        val note = when {
            returnedCount > 0 && plannedSessions > 0 ->
                "You paused $returnedCount ${if (returnedCount == 1) "time" else "times"} and planned your groceries $plannedSessions ${if (plannedSessions == 1) "evening" else "evenings"} $windowLabel. Your choices are becoming more intentional."
            returnedCount > 0 ->
                "You paused $returnedCount ${if (returnedCount == 1) "time" else "times"} $windowLabel before opening a restricted application."
            plannedSessions > 0 ->
                "You planned your groceries $plannedSessions ${if (plannedSessions == 1) "evening" else "evenings"} $windowLabel."
            else ->
                "Every mindful pause counts. There are no streaks to lose here—simply return to intention whenever you choose."
        }

        return PeriodHabitSummary(
            windowLabel = windowLabel,
            timesReturnedInsteadOfOrdering = returnedCount,
            timesWaitedAMinute = waitedCount,
            plannedShoppingSessions = plannedSessions,
            daysFollowingRestrictions = daysFollowing,
            recordedImpulsiveOrders = impulsiveCount,
            estimatedSpendingAvoided = avoidedAmount,
            encouragingNote = note
        )
    }
}
