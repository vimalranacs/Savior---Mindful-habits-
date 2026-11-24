package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.saviorDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "savior_preferences"
)

data class UserSettings(
    val onboardingCompleted: Boolean = false,
    val accessibilityDisclosureAccepted: Boolean = false,
    val themePreference: ThemePreference = ThemePreference.LIGHT,
    val currencySymbol: String = "₹",
    val currencyCode: String = "INR",
    val cooldownSeconds: Int = 15,
    val globalPauseUntilEpochMs: Long = 0L,
    // Notification settings
    val eveningReminderEnabled: Boolean = true,
    val eveningReminderHour: Int = 19,
    val eveningReminderMinute: Int = 30,
    val eveningReminderTitle: String = "A small reminder for tomorrow's you.",
    val eveningReminderBody: String = "Take a moment to check your kitchen, plan tomorrow's essentials and prepare your grocery list. A little planning can save unnecessary orders later.",
    val runningLowReminderEnabled: Boolean = true,
    val runningLowReminderBody: String = "A few household essentials may be due for replenishment. Review your list before ordering.",
    val weeklySpendingReminderEnabled: Boolean = false,
    val weeklySpendingReminderBody: String = "Take a quiet minute to review what you recorded this week—no judgment, just clarity.",
    val weeklyReflectionReminderEnabled: Boolean = false,
    val weeklyReflectionReminderBody: String = "Look back on your mindful pauses and planned evenings from the past week.",
    // Quiet hours & frequency cap
    val quietHoursEnabled: Boolean = true,
    val quietStartHour: Int = 22,
    val quietEndHour: Int = 7,
    val dailyNotificationCap: Int = 2,
    val notificationsSentToday: Int = 0,
    val lastNotificationDateKey: String = ""
)

class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.applicationContext.saviorDataStore)

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val ACCESSIBILITY_DISCLOSURE_ACCEPTED = booleanPreferencesKey("accessibility_disclosure_accepted")
        val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
        val CURRENCY_CODE = stringPreferencesKey("currency_code")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val COOLDOWN_SECONDS = intPreferencesKey("cooldown_seconds")
        val GLOBAL_PAUSE_UNTIL = longPreferencesKey("global_pause_until")

        val EVENING_ENABLED = booleanPreferencesKey("evening_enabled")
        val EVENING_HOUR = intPreferencesKey("evening_hour")
        val EVENING_MINUTE = intPreferencesKey("evening_minute")
        val EVENING_TITLE = stringPreferencesKey("evening_title")
        val EVENING_BODY = stringPreferencesKey("evening_body")

        val RUNNING_LOW_ENABLED = booleanPreferencesKey("running_low_enabled")
        val RUNNING_LOW_BODY = stringPreferencesKey("running_low_body")
        val WEEKLY_SPENDING_ENABLED = booleanPreferencesKey("weekly_spending_enabled")
        val WEEKLY_SPENDING_BODY = stringPreferencesKey("weekly_spending_body")
        val WEEKLY_REFLECTION_ENABLED = booleanPreferencesKey("weekly_reflection_enabled")
        val WEEKLY_REFLECTION_BODY = stringPreferencesKey("weekly_reflection_body")

        val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        val QUIET_START_HOUR = intPreferencesKey("quiet_start_hour")
        val QUIET_END_HOUR = intPreferencesKey("quiet_end_hour")
        val DAILY_CAP = intPreferencesKey("daily_notification_cap")
        val SENT_TODAY = intPreferencesKey("notifications_sent_today")
        val LAST_NOTIF_DATE = stringPreferencesKey("last_notification_date_key")
    }

    val settingsFlow: Flow<UserSettings> = dataStore.data.map { prefs ->
        val themeStr = prefs[Keys.THEME_PREFERENCE] ?: ThemePreference.LIGHT.name
        val theme = runCatching { ThemePreference.valueOf(themeStr) }.getOrDefault(ThemePreference.LIGHT)
        val defaults = UserSettings()
        UserSettings(
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: defaults.onboardingCompleted,
            accessibilityDisclosureAccepted = prefs[Keys.ACCESSIBILITY_DISCLOSURE_ACCEPTED] ?: defaults.accessibilityDisclosureAccepted,
            themePreference = theme,
            currencySymbol = prefs[Keys.CURRENCY_SYMBOL] ?: defaults.currencySymbol,
            currencyCode = prefs[Keys.CURRENCY_CODE] ?: defaults.currencyCode,
            cooldownSeconds = (prefs[Keys.COOLDOWN_SECONDS] ?: defaults.cooldownSeconds).coerceIn(0, 60),
            globalPauseUntilEpochMs = prefs[Keys.GLOBAL_PAUSE_UNTIL] ?: 0L,
            eveningReminderEnabled = prefs[Keys.EVENING_ENABLED] ?: defaults.eveningReminderEnabled,
            eveningReminderHour = prefs[Keys.EVENING_HOUR] ?: defaults.eveningReminderHour,
            eveningReminderMinute = prefs[Keys.EVENING_MINUTE] ?: defaults.eveningReminderMinute,
            eveningReminderTitle = prefs[Keys.EVENING_TITLE] ?: defaults.eveningReminderTitle,
            eveningReminderBody = prefs[Keys.EVENING_BODY] ?: defaults.eveningReminderBody,
            runningLowReminderEnabled = prefs[Keys.RUNNING_LOW_ENABLED] ?: defaults.runningLowReminderEnabled,
            runningLowReminderBody = prefs[Keys.RUNNING_LOW_BODY] ?: defaults.runningLowReminderBody,
            weeklySpendingReminderEnabled = prefs[Keys.WEEKLY_SPENDING_ENABLED] ?: defaults.weeklySpendingReminderEnabled,
            weeklySpendingReminderBody = prefs[Keys.WEEKLY_SPENDING_BODY] ?: defaults.weeklySpendingReminderBody,
            weeklyReflectionReminderEnabled = prefs[Keys.WEEKLY_REFLECTION_ENABLED] ?: defaults.weeklyReflectionReminderEnabled,
            weeklyReflectionReminderBody = prefs[Keys.WEEKLY_REFLECTION_BODY] ?: defaults.weeklyReflectionReminderBody,
            quietHoursEnabled = prefs[Keys.QUIET_HOURS_ENABLED] ?: defaults.quietHoursEnabled,
            quietStartHour = prefs[Keys.QUIET_START_HOUR] ?: defaults.quietStartHour,
            quietEndHour = prefs[Keys.QUIET_END_HOUR] ?: defaults.quietEndHour,
            dailyNotificationCap = (prefs[Keys.DAILY_CAP] ?: defaults.dailyNotificationCap).coerceIn(1, 5),
            notificationsSentToday = prefs[Keys.SENT_TODAY] ?: 0,
            lastNotificationDateKey = prefs[Keys.LAST_NOTIF_DATE] ?: ""
        )
    }

    suspend fun getSettingsOnce(): UserSettings = settingsFlow.first()

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setAccessibilityDisclosureAccepted(accepted: Boolean) {
        dataStore.edit { it[Keys.ACCESSIBILITY_DISCLOSURE_ACCEPTED] = accepted }
    }

    suspend fun setThemePreference(theme: ThemePreference) {
        dataStore.edit { it[Keys.THEME_PREFERENCE] = theme.name }
    }

    suspend fun setCurrency(code: String, symbol: String) {
        dataStore.edit {
            it[Keys.CURRENCY_CODE] = code
            it[Keys.CURRENCY_SYMBOL] = symbol
        }
    }

    suspend fun setCooldownSeconds(seconds: Int) {
        dataStore.edit { it[Keys.COOLDOWN_SECONDS] = seconds.coerceIn(0, 60) }
    }

    suspend fun setGlobalPauseUntil(epochMs: Long) {
        dataStore.edit { it[Keys.GLOBAL_PAUSE_UNTIL] = epochMs.coerceAtLeast(0L) }
    }

    suspend fun updateNotificationSettings(
        eveningEnabled: Boolean,
        eveningHour: Int,
        eveningMinute: Int,
        eveningTitle: String,
        eveningBody: String,
        runningLowEnabled: Boolean,
        runningLowBody: String,
        weeklySpendingEnabled: Boolean,
        weeklySpendingBody: String,
        weeklyReflectionEnabled: Boolean,
        weeklyReflectionBody: String,
        quietHoursEnabled: Boolean,
        quietStartHour: Int,
        quietEndHour: Int,
        dailyCap: Int
    ) {
        dataStore.edit {
            it[Keys.EVENING_ENABLED] = eveningEnabled
            it[Keys.EVENING_HOUR] = eveningHour.coerceIn(0, 23)
            it[Keys.EVENING_MINUTE] = eveningMinute.coerceIn(0, 59)
            it[Keys.EVENING_TITLE] = eveningTitle.trim().ifEmpty { UserSettings().eveningReminderTitle }
            it[Keys.EVENING_BODY] = eveningBody.trim().ifEmpty { UserSettings().eveningReminderBody }
            it[Keys.RUNNING_LOW_ENABLED] = runningLowEnabled
            it[Keys.RUNNING_LOW_BODY] = runningLowBody.trim().ifEmpty { UserSettings().runningLowReminderBody }
            it[Keys.WEEKLY_SPENDING_ENABLED] = weeklySpendingEnabled
            it[Keys.WEEKLY_SPENDING_BODY] = weeklySpendingBody.trim().ifEmpty { UserSettings().weeklySpendingReminderBody }
            it[Keys.WEEKLY_REFLECTION_ENABLED] = weeklyReflectionEnabled
            it[Keys.WEEKLY_REFLECTION_BODY] = weeklyReflectionBody.trim().ifEmpty { UserSettings().weeklyReflectionReminderBody }
            it[Keys.QUIET_HOURS_ENABLED] = quietHoursEnabled
            it[Keys.QUIET_START_HOUR] = quietStartHour.coerceIn(0, 23)
            it[Keys.QUIET_END_HOUR] = quietEndHour.coerceIn(0, 23)
            it[Keys.DAILY_CAP] = dailyCap.coerceIn(1, 5)
        }
    }

    suspend fun recordNotificationSent(dateKey: String) {
        dataStore.edit { prefs ->
            val currentKey = prefs[Keys.LAST_NOTIF_DATE] ?: ""
            val nextCount = if (currentKey == dateKey) (prefs[Keys.SENT_TODAY] ?: 0) + 1 else 1
            prefs[Keys.LAST_NOTIF_DATE] = dateKey
            prefs[Keys.SENT_TODAY] = nextCount
        }
    }

    suspend fun resetSettingsPreservingOnboarding() {
        val wasOnboarded = getSettingsOnce().onboardingCompleted
        val wasDisclosed = getSettingsOnce().accessibilityDisclosureAccepted
        dataStore.edit {
            it.clear()
            it[Keys.ONBOARDING_COMPLETED] = wasOnboarded
            it[Keys.ACCESSIBILITY_DISCLOSURE_ACCEPTED] = wasDisclosed
        }
    }

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}
