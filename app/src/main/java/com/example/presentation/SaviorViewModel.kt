package com.example.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.SaviorApplication
import com.example.blocking.BlockingDiagnostics
import com.example.blocking.BlockingDiagnosticsHelper
import com.example.blocking.InstalledAppInfo
import com.example.blocking.SaviorAccessibilityService
import com.example.data.local.BlockingScheduleEntity
import com.example.data.local.EveningPlanNoteEntity
import com.example.data.local.GroceryItemEntity
import com.example.data.local.GroceryListEntity
import com.example.data.local.PauseAction
import com.example.data.local.PauseEventEntity
import com.example.data.local.RestrictedAppEntity
import com.example.data.local.SpendingEntryEntity
import com.example.data.preferences.UserSettings
import com.example.domain.BlockingRuleEngine
import com.example.domain.GrocerySuggestion
import com.example.domain.GrocerySuggestionEngine
import com.example.domain.HabitSummaryEngine
import com.example.domain.PeriodHabitSummary
import com.example.domain.SpendingAnalyticsEngine
import com.example.domain.SpendingSummary
import com.example.notifications.ReminderScheduler
import com.example.ui.theme.ThemePreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DecryptedSpendingItem(
    val entity: SpendingEntryEntity,
    val note: String
)

data class SaviorUiState(
    val isReady: Boolean = false,
    val settings: UserSettings = UserSettings(),
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val isLoadingApps: Boolean = true,
    val restrictedApps: List<RestrictedAppEntity> = emptyList(),
    val schedules: List<BlockingScheduleEntity> = emptyList(),
    val diagnostics: BlockingDiagnostics? = null,
    val groceryLists: List<GroceryListEntity> = emptyList(),
    val groceryItems: List<GroceryItemEntity> = emptyList(),
    val suggestions: List<GrocerySuggestion> = emptyList(),
    val eveningPlans: List<EveningPlanNoteEntity> = emptyList(),
    val spendingItems: List<DecryptedSpendingItem> = emptyList(),
    val spendingSummary: SpendingSummary = SpendingAnalyticsEngine.summarize(emptyList(), "₹"),
    val weeklyHabitSummary: PeriodHabitSummary = HabitSummaryEngine.summarizePeriod(
        "this week", 7, emptyList(), emptyList(), emptyList()
    ),
    val monthlyHabitSummary: PeriodHabitSummary = HabitSummaryEngine.summarizePeriod(
        "this month", 30, emptyList(), emptyList(), emptyList()
    ),
    val isKeystoreBacked: Boolean = false
)

class SaviorViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val container = (application as SaviorApplication).container
    private val db = container.database
    private val prefs = container.preferencesRepository

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    private val _isLoadingApps = MutableStateFlow(true)
    private val _diagnostics = MutableStateFlow(BlockingDiagnosticsHelper.collect(appContext))
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val isKeystoreBacked: Boolean = container.cryptoManager.isHardwareKeystoreAvailable()

    private val coreDataFlow = combine(
        prefs.settingsFlow,
        db.restrictedAppDao().observeAll(),
        db.blockingScheduleDao().observeAll(),
        db.pauseEventDao().observeAll(),
        db.groceryDao().observeLists()
    ) { settings, apps, schedules, pauses, lists ->
        CoreSnapshot(settings, apps, schedules, pauses, lists)
    }

    private val secondaryDataFlow = combine(
        db.groceryDao().observeAllItems(),
        db.groceryDao().observeEveningPlans(),
        db.spendingDao().observeAll(),
        _installedApps,
        _isLoadingApps
    ) { items, plans, spending, installed, loadingApps ->
        SecondarySnapshot(items, plans, spending, installed, loadingApps)
    }

    val uiState: StateFlow<SaviorUiState> = combine(
        coreDataFlow,
        secondaryDataFlow,
        _diagnostics,
        SaviorAccessibilityService.isServiceConnected
    ) { core, secondary, diagnostics, _ ->
        val refreshedDiagnostics = BlockingDiagnosticsHelper.collect(appContext)
        val decryptedSpending = secondary.spending.map { entry ->
            DecryptedSpendingItem(
                entity = entry,
                note = container.cryptoManager.decrypt(entry.encryptedNote)
            )
        }
        val spendingSummary = SpendingAnalyticsEngine.summarize(
            entries = secondary.spending,
            currencySymbol = core.settings.currencySymbol
        )
        val weeklyHabits = HabitSummaryEngine.summarizePeriod(
            windowLabel = "this week",
            daysWindow = 7,
            pauseEvents = core.pauses,
            eveningPlans = secondary.plans,
            spendingEntries = secondary.spending
        )
        val monthlyHabits = HabitSummaryEngine.summarizePeriod(
            windowLabel = "this month",
            daysWindow = 30,
            pauseEvents = core.pauses,
            eveningPlans = secondary.plans,
            spendingEntries = secondary.spending
        )
        val suggestions = GrocerySuggestionEngine.generateSuggestions(secondary.items)

        SaviorUiState(
            isReady = true,
            settings = core.settings,
            installedApps = secondary.installed,
            isLoadingApps = secondary.loadingApps,
            restrictedApps = core.apps,
            schedules = core.schedules,
            diagnostics = refreshedDiagnostics.takeIf { diagnostics != null } ?: refreshedDiagnostics,
            groceryLists = core.lists,
            groceryItems = secondary.items,
            suggestions = suggestions,
            eveningPlans = secondary.plans,
            spendingItems = decryptedSpending,
            spendingSummary = spendingSummary,
            weeklyHabitSummary = weeklyHabits,
            monthlyHabitSummary = monthlyHabits,
            isKeystoreBacked = isKeystoreBacked
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = SaviorUiState()
    )

    init {
        viewModelScope.launch {
            db.ensureInitialDefaults()
        }
        refreshInstalledApps()
        refreshDiagnostics()
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun refreshDiagnostics() {
        _diagnostics.value = BlockingDiagnosticsHelper.collect(appContext)
    }

    fun refreshInstalledApps() {
        viewModelScope.launch {
            _isLoadingApps.value = true
            _installedApps.value = container.installedAppsProvider.loadLaunchableApps()
            _isLoadingApps.value = false
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            prefs.setOnboardingCompleted(true)
        }
    }

    fun acceptAccessibilityDisclosureAndOpenSettings() {
        viewModelScope.launch {
            prefs.setAccessibilityDisclosureAccepted(true)
            BlockingDiagnosticsHelper.openAccessibilitySettings(appContext)
        }
    }

    // --- App Selection & Blocking ---

    fun toggleAppRestriction(appInfo: InstalledAppInfo, currentlySelected: Boolean) {
        viewModelScope.launch {
            if (currentlySelected) {
                db.restrictedAppDao().deleteByPackage(appInfo.packageName)
                _snackbarMessage.value = "Removed ${appInfo.displayName} from paused apps"
            } else {
                db.restrictedAppDao().upsert(
                    RestrictedAppEntity(
                        packageName = appInfo.packageName,
                        displayName = appInfo.displayName,
                        category = appInfo.suggestedCategory,
                        isEnabled = true
                    )
                )
                _snackbarMessage.value = "Added ${appInfo.displayName} to Mindful Pause"
            }
        }
    }

    fun toggleRestrictedAppEnabled(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            db.restrictedAppDao().setEnabled(packageName, enabled)
        }
    }

    fun unlockAppTemporarily(packageName: String, appName: String, minutes: Int, endOfDay: Boolean, reason: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val until = if (endOfDay) {
                BlockingRuleEngine.computeEndOfDayEpochMs(now)
            } else {
                now + minutes * 60_000L
            }
            db.restrictedAppDao().updateUnlock(packageName, until, reason)
            db.pauseEventDao().insert(
                PauseEventEntity(
                    packageName = packageName,
                    appName = appName,
                    action = PauseAction.UNLOCKED,
                    unlockMinutes = if (endOfDay) -1 else minutes,
                    reason = reason
                )
            )
            _snackbarMessage.value = if (endOfDay) {
                "$appName unlocked until end of day"
            } else {
                "$appName unlocked for $minutes minutes"
            }
        }
    }

    fun relockAppImmediately(packageName: String, appName: String) {
        viewModelScope.launch {
            db.restrictedAppDao().updateUnlock(packageName, 0L, "")
            _snackbarMessage.value = "Restored mindful pause for $appName"
        }
    }

    fun setGlobalPauseMinutes(minutes: Int) {
        viewModelScope.launch {
            val until = if (minutes <= 0) 0L else System.currentTimeMillis() + minutes * 60_000L
            prefs.setGlobalPauseUntil(until)
            _snackbarMessage.value = if (minutes <= 0) {
                "Mindful pauses resumed"
            } else {
                "All restrictions paused for $minutes minutes"
            }
        }
    }

    fun saveSchedule(schedule: BlockingScheduleEntity) {
        viewModelScope.launch {
            if (schedule.id == 0L) {
                db.blockingScheduleDao().insert(schedule)
            } else {
                db.blockingScheduleDao().update(schedule)
            }
            _snackbarMessage.value = "Saved schedule \"${schedule.label}\""
        }
    }

    fun toggleSchedule(schedule: BlockingScheduleEntity, enabled: Boolean) {
        viewModelScope.launch {
            db.blockingScheduleDao().update(schedule.copy(isEnabled = enabled))
        }
    }

    fun deleteSchedule(id: Long) {
        viewModelScope.launch {
            db.blockingScheduleDao().deleteById(id)
        }
    }

    // --- Groceries & Evening Routine ---

    fun createGroceryList(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            db.groceryDao().insertList(GroceryListEntity(name = clean))
            _snackbarMessage.value = "Created list \"$clean\""
        }
    }

    fun deleteGroceryList(list: GroceryListEntity) {
        if (list.isDefault) return
        viewModelScope.launch {
            db.groceryDao().deleteList(list.id)
            _snackbarMessage.value = "Deleted list \"${list.name}\""
        }
    }

    fun saveGroceryItem(item: GroceryItemEntity) {
        if (item.name.isBlank()) return
        viewModelScope.launch {
            db.groceryDao().upsertItem(item.copy(name = item.name.trim()))
        }
    }

    fun addSuggestedItemToList(suggestion: GrocerySuggestion, listId: Long) {
        viewModelScope.launch {
            val existing = db.groceryDao().getAllItemsOnce()
                .firstOrNull { it.name.equals(suggestion.name, ignoreCase = true) }
            if (existing != null) {
                db.groceryDao().updateItem(
                    existing.copy(
                        listId = listId,
                        isChecked = false,
                        isAvailableAtHome = false,
                        isRunningLow = true
                    )
                )
            } else {
                db.groceryDao().upsertItem(
                    GroceryItemEntity(
                        listId = listId,
                        name = suggestion.name,
                        quantity = suggestion.defaultQuantity,
                        unit = suggestion.defaultUnit,
                        category = suggestion.category,
                        frequencyDays = suggestion.suggestedFrequencyDays,
                        reminderEnabled = suggestion.suggestedFrequencyDays > 0
                    )
                )
            }
            _snackbarMessage.value = "Added ${suggestion.name} to your plan"
        }
    }

    fun toggleGroceryItemChecked(item: GroceryItemEntity) {
        viewModelScope.launch {
            val nowChecked = !item.isChecked
            db.groceryDao().updateItem(
                item.copy(
                    isChecked = nowChecked,
                    lastPurchasedAt = if (nowChecked) System.currentTimeMillis() else item.lastPurchasedAt,
                    purchaseCount = if (nowChecked) item.purchaseCount + 1 else item.purchaseCount,
                    isRunningLow = if (nowChecked) false else item.isRunningLow
                )
            )
        }
    }

    fun toggleGroceryItemFavourite(item: GroceryItemEntity) {
        viewModelScope.launch {
            db.groceryDao().updateItem(item.copy(isFavourite = !item.isFavourite))
        }
    }

    fun toggleGroceryItemRunningLow(item: GroceryItemEntity) {
        viewModelScope.launch {
            val nextLow = !item.isRunningLow
            db.groceryDao().updateItem(
                item.copy(
                    isRunningLow = nextLow,
                    isAvailableAtHome = if (nextLow) false else item.isAvailableAtHome
                )
            )
        }
    }

    fun toggleGroceryItemAvailableAtHome(item: GroceryItemEntity) {
        viewModelScope.launch {
            val nextHome = !item.isAvailableAtHome
            db.groceryDao().updateItem(
                item.copy(
                    isAvailableAtHome = nextHome,
                    isRunningLow = if (nextHome) false else item.isRunningLow
                )
            )
        }
    }

    fun deleteGroceryItem(id: Long) {
        viewModelScope.launch {
            db.groceryDao().deleteItem(id)
        }
    }

    fun saveEveningPlan(tomorrowMeals: String, householdNeeds: String) {
        viewModelScope.launch {
            val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            db.groceryDao().upsertEveningPlan(
                EveningPlanNoteEntity(
                    dateKey = todayKey,
                    tomorrowMeals = tomorrowMeals.trim(),
                    householdNeeds = householdNeeds.trim(),
                    completedAt = System.currentTimeMillis()
                )
            )
            _snackbarMessage.value = "Evening plan saved for tomorrow"
        }
    }

    // --- Spending Awareness ---

    fun addSpendingEntry(
        amount: Double,
        category: String,
        appName: String,
        isImpulsive: Boolean,
        isAvoidedEstimate: Boolean,
        note: String,
        dateEpochMs: Long = System.currentTimeMillis()
    ) {
        if (amount <= 0.0 || amount.isNaN() || amount.isInfinite()) return
        viewModelScope.launch {
            val encrypted = container.cryptoManager.encrypt(note.trim())
            db.spendingDao().insert(
                SpendingEntryEntity(
                    amount = amount,
                    dateEpochMs = dateEpochMs,
                    category = category.trim().ifEmpty { "Other" },
                    appName = appName.trim(),
                    isImpulsive = isImpulsive,
                    isAvoidedEstimate = isAvoidedEstimate,
                    encryptedNote = encrypted
                )
            )
            _snackbarMessage.value = if (isAvoidedEstimate) {
                "Recorded planned alternative"
            } else {
                "Recorded spending entry"
            }
        }
    }

    fun deleteSpendingEntry(id: Long) {
        viewModelScope.launch {
            db.spendingDao().deleteById(id)
        }
    }

    // --- Settings, Notifications & Privacy ---

    fun setThemePreference(theme: ThemePreference) {
        viewModelScope.launch { prefs.setThemePreference(theme) }
    }

    fun setCurrency(code: String, symbol: String) {
        viewModelScope.launch { prefs.setCurrency(code, symbol) }
    }

    fun setCooldownSeconds(seconds: Int) {
        viewModelScope.launch { prefs.setCooldownSeconds(seconds) }
    }

    fun saveNotificationSettings(
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
        viewModelScope.launch {
            prefs.updateNotificationSettings(
                eveningEnabled = eveningEnabled,
                eveningHour = eveningHour,
                eveningMinute = eveningMinute,
                eveningTitle = eveningTitle,
                eveningBody = eveningBody,
                runningLowEnabled = runningLowEnabled,
                runningLowBody = runningLowBody,
                weeklySpendingEnabled = weeklySpendingEnabled,
                weeklySpendingBody = weeklySpendingBody,
                weeklyReflectionEnabled = weeklyReflectionEnabled,
                weeklyReflectionBody = weeklyReflectionBody,
                quietHoursEnabled = quietHoursEnabled,
                quietStartHour = quietStartHour,
                quietEndHour = quietEndHour,
                dailyCap = dailyCap
            )
            val updated = prefs.getSettingsOnce()
            ReminderScheduler.syncSchedules(appContext, updated)
            _snackbarMessage.value = "Reminder preferences updated"
        }
    }

    suspend fun generateExportJson(): String = withContext(Dispatchers.IO) {
        container.dataExporter.exportToJsonString()
    }

    fun resetSettings() {
        viewModelScope.launch {
            prefs.resetSettingsPreservingOnboarding()
            val updated = prefs.getSettingsOnce()
            ReminderScheduler.syncSchedules(appContext, updated)
            _snackbarMessage.value = "Settings reset to calm defaults"
        }
    }

    fun deleteAllUserData() {
        viewModelScope.launch {
            container.dataExporter.deleteAllLocalData()
            val updated = prefs.getSettingsOnce()
            ReminderScheduler.syncSchedules(appContext, updated)
            _snackbarMessage.value = "All local data deleted"
        }
    }

    private data class CoreSnapshot(
        val settings: UserSettings,
        val apps: List<RestrictedAppEntity>,
        val schedules: List<BlockingScheduleEntity>,
        val pauses: List<PauseEventEntity>,
        val lists: List<GroceryListEntity>
    )

    private data class SecondarySnapshot(
        val items: List<GroceryItemEntity>,
        val plans: List<EveningPlanNoteEntity>,
        val spending: List<SpendingEntryEntity>,
        val installed: List<InstalledAppInfo>,
        val loadingApps: Boolean
    )
}
