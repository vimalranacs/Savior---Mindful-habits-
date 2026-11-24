package com.example.blocking

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import androidx.core.content.ContextCompat

data class BlockingDiagnostics(
    val accessibilityEnabledInSettings: Boolean,
    val accessibilityServiceConnected: Boolean,
    val notificationPermissionGranted: Boolean,
    val ignoringBatteryOptimizations: Boolean,
    val statusHeadline: String,
    val statusDetail: String
)

object BlockingDiagnosticsHelper {

    fun isAccessibilityEnabledInSettings(context: Context): Boolean {
        val expectedComponent = ComponentName(context, SaviorAccessibilityService::class.java)
            .flattenToString()
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        while (splitter.hasNext()) {
            val component = splitter.next()
            if (component.equals(expectedComponent, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    fun isNotificationPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return false
        return runCatching { powerManager.isIgnoringBatteryOptimizations(context.packageName) }
            .getOrDefault(false)
    }

    fun collect(context: Context): BlockingDiagnostics {
        val enabledInSettings = isAccessibilityEnabledInSettings(context)
        val connected = SaviorAccessibilityService.isServiceConnected.value
        val notifGranted = isNotificationPermissionGranted(context)
        val batteryExempt = isIgnoringBatteryOptimizations(context)

        val (headline, detail) = when {
            enabledInSettings && connected ->
                "Mindful Pause Service is active" to
                    "Watching only the apps you selected. Never reads screen contents or text."
            enabledInSettings && !connected ->
                "Service enabled, awaiting system reconnect" to
                    "Android temporarily paused the service. Toggle it off and back on in Accessibility settings if pauses do not appear."
            else ->
                "Mindful Pause Service is not enabled yet" to
                    "Enable Savior in Android Accessibility settings when you are ready for automatic pauses."
        }

        return BlockingDiagnostics(
            accessibilityEnabledInSettings = enabledInSettings,
            accessibilityServiceConnected = connected,
            notificationPermissionGranted = notifGranted,
            ignoringBatteryOptimizations = batteryExempt,
            statusHeadline = headline,
            statusDetail = detail
        )
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openBatteryOptimizationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openAppNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}
