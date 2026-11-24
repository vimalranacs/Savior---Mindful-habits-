package com.example.privacy

import com.example.data.local.SaviorDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.ui.theme.BrandConfig
import org.json.JSONArray
import org.json.JSONObject

class LocalDataExporter(
    private val database: SaviorDatabase,
    private val preferences: UserPreferencesRepository,
    private val cryptoManager: KeystoreCryptoManager
) {
    suspend fun exportToJsonString(exportedAtEpochMs: Long = System.currentTimeMillis()): String {
        val settings = preferences.getSettingsOnce()
        val apps = database.restrictedAppDao().getAllOnce()
        val schedules = database.blockingScheduleDao().getAllOnce()
        val events = database.pauseEventDao().getAllOnce()
        val lists = database.groceryDao().getListsOnce()
        val items = database.groceryDao().getAllItemsOnce()
        val eveningPlans = database.groceryDao().getEveningPlansOnce()
        val spending = database.spendingDao().getAllOnce()

        val root = JSONObject().apply {
            put("application", BrandConfig.APP_NAME)
            put("schemaVersion", 1)
            put("exportedAtEpochMs", exportedAtEpochMs)
            put("settings", JSONObject().apply {
                put("themePreference", settings.themePreference.name)
                put("currencyCode", settings.currencyCode)
                put("currencySymbol", settings.currencySymbol)
                put("cooldownSeconds", settings.cooldownSeconds)
                put("eveningReminderEnabled", settings.eveningReminderEnabled)
                put("eveningReminderTime", "%02d:%02d".format(settings.eveningReminderHour, settings.eveningReminderMinute))
                put("quietHoursEnabled", settings.quietHoursEnabled)
                put("quietStartHour", settings.quietStartHour)
                put("quietEndHour", settings.quietEndHour)
                put("dailyNotificationCap", settings.dailyNotificationCap)
            })
            put("restrictedApps", JSONArray().apply {
                apps.forEach { app ->
                    put(JSONObject().apply {
                        put("packageName", app.packageName)
                        put("displayName", app.displayName)
                        put("category", app.category)
                        put("isEnabled", app.isEnabled)
                    })
                }
            })
            put("blockingSchedules", JSONArray().apply {
                schedules.forEach { s ->
                    put(JSONObject().apply {
                        put("label", s.label)
                        put("mode", s.mode)
                        put("startMinuteOfDay", s.startMinuteOfDay)
                        put("endMinuteOfDay", s.endMinuteOfDay)
                        put("daysMask", s.daysMask)
                        put("isEnabled", s.isEnabled)
                    })
                }
            })
            put("pauseEvents", JSONArray().apply {
                events.forEach { e ->
                    put(JSONObject().apply {
                        put("appName", e.appName)
                        put("action", e.action)
                        put("unlockMinutes", e.unlockMinutes)
                        put("reason", e.reason)
                        put("timestamp", e.timestamp)
                    })
                }
            })
            put("groceryLists", JSONArray().apply {
                lists.forEach { l ->
                    put(JSONObject().apply {
                        put("id", l.id)
                        put("name", l.name)
                        put("isDefault", l.isDefault)
                    })
                }
            })
            put("groceryItems", JSONArray().apply {
                items.forEach { i ->
                    put(JSONObject().apply {
                        put("listId", i.listId)
                        put("name", i.name)
                        put("quantity", i.quantity)
                        put("unit", i.unit)
                        put("category", i.category)
                        put("approxPrice", i.approxPrice)
                        put("frequencyDays", i.frequencyDays)
                        put("lastPurchasedAt", i.lastPurchasedAt)
                        put("isChecked", i.isChecked)
                        put("isFavourite", i.isFavourite)
                        put("isRunningLow", i.isRunningLow)
                        put("isAvailableAtHome", i.isAvailableAtHome)
                    })
                }
            })
            put("eveningPlans", JSONArray().apply {
                eveningPlans.forEach { p ->
                    put(JSONObject().apply {
                        put("dateKey", p.dateKey)
                        put("tomorrowMeals", p.tomorrowMeals)
                        put("householdNeeds", p.householdNeeds)
                        put("completedAt", p.completedAt)
                    })
                }
            })
            put("spendingEntries", JSONArray().apply {
                spending.forEach { entry ->
                    put(JSONObject().apply {
                        put("amount", entry.amount)
                        put("dateEpochMs", entry.dateEpochMs)
                        put("category", entry.category)
                        put("appName", entry.appName)
                        put("isImpulsive", entry.isImpulsive)
                        put("isAvoidedEstimate", entry.isAvoidedEstimate)
                        put("note", cryptoManager.decrypt(entry.encryptedNote))
                    })
                }
            })
        }
        return root.toString(2)
    }

    suspend fun deleteAllLocalData() {
        database.spendingDao().deleteAll()
        database.pauseEventDao().deleteAll()
        database.groceryDao().deleteAllItems()
        database.groceryDao().deleteAllLists()
        database.groceryDao().deleteAllEveningPlans()
        database.restrictedAppDao().deleteAll()
        database.blockingScheduleDao().deleteAll()
        preferences.clearAll()
        database.ensureInitialDefaults()
    }
}
