package com.example.blocking

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.SaviorApplication
import com.example.data.local.BlockingScheduleEntity
import com.example.data.local.RestrictedAppEntity
import com.example.domain.BlockingRuleEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Strictly scoped AccessibilityService for mindful app pausing.
 *
 * Privacy guarantee:
 * - Configured with android:canRetrieveWindowContent="false" and typeWindowStateChanged only.
 * - Inspects ONLY event.packageName against the user's locally selected app list.
 * - Never reads screen text, views, keystrokes, passwords, messages, or notifications.
 * - Never transmits anything off the device.
 */
class SaviorAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var cachedApps: List<RestrictedAppEntity> = emptyList()

    @Volatile
    private var cachedSchedules: List<BlockingScheduleEntity> = emptyList()

    @Volatile
    private var cachedGlobalPauseUntil: Long = 0L

    @Volatile
    private var lastInterruptedPackage: String = ""

    @Volatile
    private var lastInterruptTimestamp: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        _isServiceConnected.value = true

        val container = (applicationContext as? SaviorApplication)?.container ?: return

        serviceScope.launch {
            container.database.ensureInitialDefaults()
        }
        serviceScope.launch {
            container.database.restrictedAppDao().observeAll().collectLatest { apps ->
                cachedApps = apps
            }
        }
        serviceScope.launch {
            container.database.blockingScheduleDao().observeAll().collectLatest { schedules ->
                cachedSchedules = schedules
            }
        }
        serviceScope.launch {
            container.preferencesRepository.settingsFlow.collectLatest { settings ->
                cachedGlobalPauseUntil = settings.globalPauseUntilEpochMs
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg.isBlank() || pkg == packageName) return

        // Reset debounce once the user navigates away to a different app or launcher
        if (pkg != lastInterruptedPackage && !isSystemOverlayPackage(pkg)) {
            lastInterruptedPackage = ""
        }

        val now = System.currentTimeMillis()
        val evaluation = BlockingRuleEngine.evaluate(
            packageName = pkg,
            restrictedApps = cachedApps,
            schedules = cachedSchedules,
            globalPauseUntilEpochMs = cachedGlobalPauseUntil,
            nowEpochMs = now
        )

        if (!evaluation.shouldInterrupt) return

        // Prevent rapid duplicate launches from consecutive window events inside the same app
        if (pkg == lastInterruptedPackage && (now - lastInterruptTimestamp) < DEBOUNCE_MS) {
            return
        }

        lastInterruptedPackage = pkg
        lastInterruptTimestamp = now

        val matchedApp = cachedApps.firstOrNull { it.packageName == pkg }
        val appName = matchedApp?.displayName ?: pkg

        val intent = PauseInterruptionActivity.createIntent(
            context = this,
            targetPackage = pkg,
            targetAppName = appName,
            scheduleLabel = evaluation.matchedScheduleLabel.orEmpty(),
            isPreview = false
        )
        runCatching { startActivity(intent) }
    }

    override fun onInterrupt() {
        // Service interrupted by system; status will reflect cleanly if unbound
    }

    override fun onUnbind(intent: Intent?): Boolean {
        _isServiceConnected.value = false
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        _isServiceConnected.value = false
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun isSystemOverlayPackage(pkg: String): Boolean {
        return pkg == "com.android.systemui" || pkg == "android"
    }

    companion object {
        private const val DEBOUNCE_MS = 1800L

        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()
    }
}
