package com.example

import com.example.blocking.InstalledAppsProvider
import com.example.data.local.AppCategory
import com.example.data.local.BlockingScheduleEntity
import com.example.data.local.EveningPlanNoteEntity
import com.example.data.local.GroceryItemEntity
import com.example.data.local.PauseAction
import com.example.data.local.PauseEventEntity
import com.example.data.local.RestrictedAppEntity
import com.example.data.local.ScheduleMode
import com.example.data.local.SpendingEntryEntity
import com.example.domain.BlockingRuleEngine
import com.example.domain.GrocerySuggestionEngine
import com.example.domain.HabitSummaryEngine
import com.example.domain.QuietHoursEvaluator
import com.example.domain.SpendingAnalyticsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ExampleUnitTest {

    private val istZone = TimeZone.getTimeZone("GMT+05:30")

    @Test
    fun `blocking engine pauses selected app when schedule is active and Allows unselected apps`() {
        val restricted = listOf(
            RestrictedAppEntity(
                packageName = "com.grofers.customerapp",
                displayName = "Blinkit",
                category = AppCategory.QUICK_COMMERCE,
                isEnabled = true
            )
        )
        val schedules = listOf(
            BlockingScheduleEntity(
                id = 1,
                label = "Always Mindful",
                mode = ScheduleMode.ALWAYS,
                isEnabled = true
            )
        )
        val now = 1_750_000_000_000L

        val blockedResult = BlockingRuleEngine.evaluate(
            packageName = "com.grofers.customerapp",
            restrictedApps = restricted,
            schedules = schedules,
            globalPauseUntilEpochMs = 0L,
            nowEpochMs = now,
            timeZone = istZone
        )
        assertTrue(blockedResult.shouldInterrupt)

        val unselectedResult = BlockingRuleEngine.evaluate(
            packageName = "com.calculator.app",
            restrictedApps = restricted,
            schedules = schedules,
            globalPauseUntilEpochMs = 0L,
            nowEpochMs = now,
            timeZone = istZone
        )
        assertFalse(unselectedResult.shouldInterrupt)
    }

    @Test
    fun `blocking engine respects temporary unlock, end-of-day unlock, and global pause`() {
        val now = 1_750_000_000_000L
        val endOfDay = BlockingRuleEngine.computeEndOfDayEpochMs(now, istZone)
        assertTrue(endOfDay > now)

        val unlockedApp = RestrictedAppEntity(
            packageName = "in.swiggy.android",
            displayName = "Swiggy",
            unlockedUntilEpochMs = now + 15 * 60_000L
        )
        val alwaysSchedule = listOf(
            BlockingScheduleEntity(id = 1, label = "Always", mode = ScheduleMode.ALWAYS, isEnabled = true)
        )

        val whileUnlocked = BlockingRuleEngine.evaluate(
            packageName = "in.swiggy.android",
            restrictedApps = listOf(unlockedApp),
            schedules = alwaysSchedule,
            globalPauseUntilEpochMs = 0L,
            nowEpochMs = now,
            timeZone = istZone
        )
        assertFalse(whileUnlocked.shouldInterrupt)

        val afterUnlockExpired = BlockingRuleEngine.evaluate(
            packageName = "in.swiggy.android",
            restrictedApps = listOf(unlockedApp),
            schedules = alwaysSchedule,
            globalPauseUntilEpochMs = 0L,
            nowEpochMs = now + 20 * 60_000L,
            timeZone = istZone
        )
        assertTrue(afterUnlockExpired.shouldInterrupt)

        val whileGloballyPaused = BlockingRuleEngine.evaluate(
            packageName = "in.swiggy.android",
            restrictedApps = listOf(unlockedApp.copy(unlockedUntilEpochMs = 0L)),
            schedules = alwaysSchedule,
            globalPauseUntilEpochMs = now + 30 * 60_000L,
            nowEpochMs = now,
            timeZone = istZone
        )
        assertFalse(whileGloballyPaused.shouldInterrupt)
    }

    @Test
    fun `blocking engine evaluates work-hour and evening-cutoff schedules accurately`() {
        val monday10Am = Calendar.getInstance(istZone).apply {
            set(2026, Calendar.NOVEMBER, 23, 10, 15, 0) // Monday 10:15 AM
        }.timeInMillis

        val monday9Pm = Calendar.getInstance(istZone).apply {
            set(2026, Calendar.NOVEMBER, 23, 21, 30, 0) // Monday 9:30 PM
        }.timeInMillis

        val eveningSchedule = BlockingScheduleEntity(
            id = 2,
            label = "After 8 PM",
            mode = ScheduleMode.EVENING_CUTOFF,
            startMinuteOfDay = 20 * 60,
            endMinuteOfDay = 23 * 60 + 59,
            daysMask = 127,
            isEnabled = true
        )

        assertFalse(BlockingRuleEngine.isScheduleActiveAt(eveningSchedule, monday10Am, istZone))
        assertTrue(BlockingRuleEngine.isScheduleActiveAt(eveningSchedule, monday9Pm, istZone))
    }

    @Test
    fun `grocery suggestion engine prioritizes running-low and replenishment-due items and excludes at-home`() {
        val now = 1_750_000_000_000L
        val fiveDaysAgo = now - 5L * 24L * 60L * 60L * 1000L

        val items = listOf(
            GroceryItemEntity(
                id = 1,
                listId = 1,
                name = "Milk",
                frequencyDays = 2,
                lastPurchasedAt = fiveDaysAgo,
                isChecked = true,
                isAvailableAtHome = false
            ),
            GroceryItemEntity(
                id = 2,
                listId = 1,
                name = "Rice",
                frequencyDays = 14,
                lastPurchasedAt = fiveDaysAgo,
                isChecked = true,
                isAvailableAtHome = true
            )
        )

        assertTrue(GrocerySuggestionEngine.isDueForReplenishment(items[0], now))
        assertFalse(GrocerySuggestionEngine.isDueForReplenishment(items[1], now))

        val suggestions = GrocerySuggestionEngine.generateSuggestions(items, now)
        assertTrue(suggestions.any { it.name == "Milk" })
        assertFalse(suggestions.any { it.name == "Rice" })
    }

    @Test
    fun `spending analytics computes weekly, monthly, impulsive, and avoided totals honestly`() {
        val now = 1_750_000_000_000L
        val twoDaysAgo = now - 2L * 24L * 60L * 60L * 1000L
        val fifteenDaysAgo = now - 15L * 24L * 60L * 60L * 1000L

        val entries = listOf(
            SpendingEntryEntity(
                id = 1,
                amount = 450.0,
                dateEpochMs = twoDaysAgo,
                category = "Quick Commerce",
                appName = "Blinkit",
                isImpulsive = true
            ),
            SpendingEntryEntity(
                id = 2,
                amount = 800.0,
                dateEpochMs = fifteenDaysAgo,
                category = "Groceries",
                appName = "Planned Market",
                isImpulsive = false
            ),
            SpendingEntryEntity(
                id = 3,
                amount = 320.0,
                dateEpochMs = twoDaysAgo,
                category = "Food Delivery",
                appName = "Swiggy",
                isImpulsive = false,
                isAvoidedEstimate = true
            )
        )

        val summary = SpendingAnalyticsEngine.summarize(entries, "₹", now, istZone)
        assertEquals(450.0, summary.weeklyTotal, 0.01)
        assertEquals(1250.0, summary.monthlyTotal, 0.01)
        assertEquals(450.0, summary.monthlyImpulsiveTotal, 0.01)
        assertEquals(800.0, summary.monthlyNecessaryTotal, 0.01)
        assertEquals(320.0, summary.estimatedAvoidedTotal, 0.01)
    }

    @Test
    fun `quiet hours and daily frequency caps suppress sleep-time and excess notifications`() {
        // Overnight quiet window 22:00 to 07:00
        assertTrue(QuietHoursEvaluator.isInQuietHours(23, true, 22, 7))
        assertTrue(QuietHoursEvaluator.isInQuietHours(5, true, 22, 7))
        assertFalse(QuietHoursEvaluator.isInQuietHours(19, true, 22, 7))

        val canSendAt730Pm = QuietHoursEvaluator.canSendNotification(
            currentHour = 19,
            quietHoursEnabled = true,
            quietStartHour = 22,
            quietEndHour = 7,
            notificationsSentToday = 1,
            lastNotificationDateKey = "2026-11-23",
            todayDateKey = "2026-11-23",
            dailyCap = 2
        )
        assertTrue(canSendAt730Pm)

        val blockedWhenCapReached = QuietHoursEvaluator.canSendNotification(
            currentHour = 19,
            quietHoursEnabled = true,
            quietStartHour = 22,
            quietEndHour = 7,
            notificationsSentToday = 2,
            lastNotificationDateKey = "2026-11-23",
            todayDateKey = "2026-11-23",
            dailyCap = 2
        )
        assertFalse(blockedWhenCapReached)
    }

    @Test
    fun `habit summary engine computes mindful returns and planned evenings without streaks`() {
        val now = 1_750_000_000_000L
        val pauses = listOf(
            PauseEventEntity(packageName = "com.zeptoconsumerapp", appName = "Zepto", action = PauseAction.RETURNED, timestamp = now - 3600_000L),
            PauseEventEntity(packageName = "com.zeptoconsumerapp", appName = "Zepto", action = PauseAction.RETURNED, timestamp = now - 7200_000L),
            PauseEventEntity(packageName = "in.swiggy.android", appName = "Swiggy", action = PauseAction.WAITED_MINUTE, timestamp = now - 10_000_000L)
        )
        val plans = listOf(
            EveningPlanNoteEntity(dateKey = "2026-11-22", tomorrowMeals = "Dal & Rice", completedAt = now - 86_400_000L),
            EveningPlanNoteEntity(dateKey = "2026-11-23", tomorrowMeals = "Roti & Sabzi", completedAt = now - 3600_000L)
        )
        val summary = HabitSummaryEngine.summarizePeriod(
            windowLabel = "this week",
            daysWindow = 7,
            pauseEvents = pauses,
            eveningPlans = plans,
            spendingEntries = emptyList(),
            nowEpochMs = now,
            timeZone = istZone
        )
        assertEquals(2, summary.timesReturnedInsteadOfOrdering)
        assertEquals(1, summary.timesWaitedAMinute)
        assertEquals(2, summary.plannedShoppingSessions)
    }

    @Test
    fun `installed apps provider categorizes quick commerce, food delivery, and shopping brands`() {
        assertEquals(AppCategory.QUICK_COMMERCE, InstalledAppsProvider.categorize("com.grofers.customerapp", "Blinkit"))
        assertEquals(AppCategory.QUICK_COMMERCE, InstalledAppsProvider.categorize("com.zeptoconsumerapp", "Zepto"))
        assertEquals(AppCategory.FOOD_DELIVERY, InstalledAppsProvider.categorize("in.swiggy.android", "Swiggy"))
        assertEquals(AppCategory.FOOD_DELIVERY, InstalledAppsProvider.categorize("com.application.zomato", "Zomato"))
        assertEquals(AppCategory.SHOPPING, InstalledAppsProvider.categorize("com.flipkart.android", "Flipkart"))
    }
}
