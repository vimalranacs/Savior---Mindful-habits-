package com.example.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.blocking.BlockingDiagnosticsHelper
import com.example.blocking.InstalledAppInfo
import com.example.blocking.InstalledAppsProvider
import com.example.data.local.AppCategory
import com.example.data.local.BlockingScheduleEntity
import com.example.data.local.RestrictedAppEntity
import com.example.data.local.ScheduleMode
import com.example.domain.BlockingRuleEngine
import com.example.presentation.SaviorUiState
import com.example.presentation.components.AppIconAvatar
import com.example.presentation.components.CalmCard
import com.example.presentation.components.CalmSectionHeader
import com.example.presentation.components.CalmStatusBadge
import com.example.ui.theme.LocalSaviorColors
import java.util.concurrent.TimeUnit

private val quickPresetApps = listOf(
    InstalledAppInfo("com.grofers.customerapp", "Blinkit", AppCategory.QUICK_COMMERCE, null),
    InstalledAppInfo("com.zeptoconsumerapp", "Zepto", AppCategory.QUICK_COMMERCE, null),
    InstalledAppInfo("in.swiggy.android", "Swiggy (Food & Instamart)", AppCategory.FOOD_DELIVERY, null),
    InstalledAppInfo("com.application.zomato", "Zomato", AppCategory.FOOD_DELIVERY, null),
    InstalledAppInfo("com.bigbasket.mobileapp", "BigBasket", AppCategory.QUICK_COMMERCE, null),
    InstalledAppInfo("in.amazon.mShop.android.shopping", "Amazon Shopping", AppCategory.SHOPPING, null),
    InstalledAppInfo("com.flipkart.android", "Flipkart", AppCategory.SHOPPING, null)
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShieldScreen(
    uiState: SaviorUiState,
    onToggleAppRestriction: (InstalledAppInfo, Boolean) -> Unit,
    onToggleRestrictedAppEnabled: (String, Boolean) -> Unit,
    onUnlockAppTemporarily: (packageName: String, appName: String, minutes: Int, endOfDay: Boolean, reason: String) -> Unit,
    onRelockApp: (packageName: String, appName: String) -> Unit,
    onSetGlobalPauseMinutes: (Int) -> Unit,
    onSaveSchedule: (BlockingScheduleEntity) -> Unit,
    onToggleSchedule: (BlockingScheduleEntity, Boolean) -> Unit,
    onDeleteSchedule: (Long) -> Unit,
    onRequestEnableAccessibility: () -> Unit,
    onRefreshAppsAndDiagnostics: () -> Unit,
    onPreviewPauseScreen: (String) -> Unit
) {
    val context = LocalContext.current
    val saviorColors = LocalSaviorColors.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    var appToUnlock by remember { mutableStateOf<RestrictedAppEntity?>(null) }
    var showAddScheduleDialog by remember { mutableStateOf(false) }
    var showAddCustomPackageDialog by remember { mutableStateOf(false) }

    val restrictedMap = remember(uiState.restrictedApps) {
        uiState.restrictedApps.associateBy { it.packageName }
    }

    // Merge installed launchable apps with well-known quick-commerce / food-delivery presets
    // and any custom apps already saved by the user so the selector is comprehensive even in an emulator.
    val combinedApps = remember(uiState.installedApps, uiState.restrictedApps) {
        val map = LinkedHashMap<String, InstalledAppInfo>()
        quickPresetApps.forEach { preset ->
            map[preset.packageName] = preset
        }
        uiState.installedApps.forEach { installed ->
            map[installed.packageName] = installed
        }
        uiState.restrictedApps.forEach { saved ->
            if (!map.containsKey(saved.packageName)) {
                map[saved.packageName] = InstalledAppInfo(
                    packageName = saved.packageName,
                    displayName = saved.displayName,
                    suggestedCategory = saved.category,
                    icon = null
                )
            }
        }
        map.values.toList()
    }

    val filteredApps = remember(combinedApps, searchQuery, selectedCategory, restrictedMap) {
        combinedApps.filter { app ->
            val matchesQuery = searchQuery.isBlank() ||
                app.displayName.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)
            val matchesCategory = when (selectedCategory) {
                "All" -> true
                "Selected (${restrictedMap.size})" -> restrictedMap.containsKey(app.packageName)
                else -> app.suggestedCategory == selectedCategory
            }
            matchesQuery && matchesCategory
        }
    }

    val now = System.currentTimeMillis()
    val isGloballyPaused = uiState.settings.globalPauseUntilEpochMs > now

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("shield_screen_list"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CalmSectionHeader(
                    title = "Mindful App Pausing",
                    subtitle = "Choose which apps trigger a pause, set schedules, or unlock anytime",
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRefreshAppsAndDiagnostics) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Refresh apps and service status"
                    )
                }
            }
        }

        // Sub-navigation segmented buttons: Apps | Schedules | Diagnostics
        item {
            val tabs = listOf("Apps (${uiState.restrictedApps.size})", "Schedules", "Diagnostics")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                tabs.forEachIndexed { index, title ->
                    SegmentedButton(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                        modifier = Modifier.testTag("shield_tab_$index")
                    ) {
                        Text(title, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        when (selectedTab) {
            0 -> {
                // Service banner if accessibility is not enabled yet
                val diag = uiState.diagnostics
                if (diag != null && !diag.accessibilityEnabledInSettings) {
                    item {
                        CalmCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Activate Automatic Pausing",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Text(
                                text = "Select your apps below. When you are ready for automatic detection, review our transparent disclosure and enable the Mindful Pause Service.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = onRequestEnableAccessibility,
                                    modifier = Modifier.testTag("shield_enable_accessibility_button")
                                ) {
                                    Text("Review & Enable Service")
                                }
                                OutlinedButton(
                                    onClick = { onPreviewPauseScreen("Blinkit") }
                                ) {
                                    Text("Test Pause Screen")
                                }
                            }
                        }
                    }
                }

                // Search & Category Filters
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("app_search_input"),
                            placeholder = { Text("Search installed apps or brands…") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
                            },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium
                        )

                        val categories = listOf(
                            "All",
                            "Selected (${restrictedMap.size})",
                            AppCategory.QUICK_COMMERCE,
                            AppCategory.FOOD_DELIVERY,
                            AppCategory.SHOPPING,
                            AppCategory.ENTERTAINMENT,
                            AppCategory.OTHER
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categories.forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat, style = MaterialTheme.typography.labelMedium) }
                                )
                            }
                        }
                    }
                }

                if (uiState.isLoadingApps && combinedApps.isEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                items(filteredApps, key = { it.packageName }) { app ->
                    val savedEntity = restrictedMap[app.packageName]
                    val isSelected = savedEntity != null
                    val isTemporarilyUnlocked = (savedEntity?.unlockedUntilEpochMs ?: 0L) > now

                    CalmCard(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppIconAvatar(
                                    displayName = app.displayName,
                                    drawable = app.icon
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = app.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = app.suggestedCategory,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = isSelected && savedEntity.isEnabled,
                                onCheckedChange = { checked ->
                                    if (!isSelected && checked) {
                                        onToggleAppRestriction(app, false)
                                    } else if (isSelected && !checked) {
                                        onToggleAppRestriction(app, true)
                                    }
                                },
                                modifier = Modifier.testTag("toggle_app_${app.packageName}")
                            )
                        }

                        if (isSelected && savedEntity != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isTemporarilyUnlocked) {
                                    val minsLeft = TimeUnit.MILLISECONDS.toMinutes(
                                        (savedEntity.unlockedUntilEpochMs - now).coerceAtLeast(60_000L)
                                    )
                                    Text(
                                        text = "Unlocked for ~${minsLeft}m",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = saviorColors.positive
                                    )
                                    TextButton(
                                        onClick = { onRelockApp(savedEntity.packageName, savedEntity.displayName) }
                                    ) {
                                        Text("Re-lock Now")
                                    }
                                } else {
                                    TextButton(
                                        onClick = { appToUnlock = savedEntity },
                                        modifier = Modifier.testTag("manual_unlock_${app.packageName}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.LockOpen,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.size(6.dp))
                                        Text("Temporary Unlock")
                                    }
                                    TextButton(
                                        onClick = { onPreviewPauseScreen(app.displayName) }
                                    ) {
                                        Text("Preview Pause")
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedButton(
                        onClick = { showAddCustomPackageDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Add Any Custom Application by Name / Package")
                    }
                }
            }

            1 -> {
                // Global Pause Override & Schedules
                item {
                    CalmCard {
                        Text(
                            text = "Manual Override & Global Pause",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "You always retain complete control of your phone. Temporarily pause all restrictions whenever needed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isGloballyPaused) {
                                Button(onClick = { onSetGlobalPauseMinutes(0) }) {
                                    Text("Resume All Restrictions Now")
                                }
                            } else {
                                listOf(15 to "Pause 15m", 30 to "Pause 30m", 60 to "Pause 1h").forEach { (mins, label) ->
                                    AssistChip(
                                        onClick = { onSetGlobalPauseMinutes(mins) },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    }
                }

                items(uiState.schedules, key = { it.id }) { schedule ->
                    CalmCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = schedule.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = formatScheduleSummary(schedule),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = schedule.isEnabled,
                                onCheckedChange = { onToggleSchedule(schedule, it) }
                            )
                            if (uiState.schedules.size > 1) {
                                IconButton(onClick = { onDeleteSchedule(schedule.id) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = "Delete schedule"
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = { showAddScheduleDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_schedule_button"),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(imageVector = Icons.Outlined.Schedule, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Add Blocking Schedule")
                    }
                }
            }

            else -> {
                // Diagnostics & Honest Technical Limitations
                val diag = uiState.diagnostics
                item {
                    CalmCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (diag?.accessibilityEnabledInSettings == true) {
                                    Icons.Outlined.CheckCircle
                                } else {
                                    Icons.Outlined.WarningAmber
                                },
                                contentDescription = null,
                                tint = if (diag?.accessibilityEnabledInSettings == true) {
                                    saviorColors.positive
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                            Text(
                                text = diag?.statusHeadline ?: "Checking service health…",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Text(
                            text = diag?.statusDetail.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        DiagnosticRow(
                            label = "Accessibility Service enabled in Settings",
                            isOk = diag?.accessibilityEnabledInSettings == true
                        )
                        DiagnosticRow(
                            label = "Live window-change listener connected",
                            isOk = diag?.accessibilityServiceConnected == true
                        )
                        DiagnosticRow(
                            label = "Notification permission granted",
                            isOk = diag?.notificationPermissionGranted == true
                        )
                        DiagnosticRow(
                            label = "Unrestricted background battery status",
                            isOk = diag?.ignoringBatteryOptimizations == true
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(onClick = onRequestEnableAccessibility) {
                                Text("Accessibility Settings")
                            }
                            OutlinedButton(
                                onClick = { BlockingDiagnosticsHelper.openBatteryOptimizationSettings(context) }
                            ) {
                                Text("Battery Optimization")
                            }
                        }
                    }
                }

                item {
                    CalmCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Honest Android Technical Boundaries",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Text(
                            text = "• Only the foreground package identifier is inspected. Screen contents, passwords, messages, and keystrokes are never collected.\n" +
                                "• No Android app can or should claim to make another application impossible to open. Permissions can be revoked, services can be stopped by aggressive OEM battery savers, and restrictions can be bypassed by the device owner.\n" +
                                "• If pausing stops working after a system update or aggressive battery cleanup, re-toggle the service in Android Accessibility Settings and mark Savior as Unrestricted in Battery Settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }

    if (appToUnlock != null) {
        val target = appToUnlock!!
        ManualUnlockDialog(
            appName = target.displayName,
            onDismiss = { appToUnlock = null },
            onConfirmUnlock = { mins, eod, reason ->
                onUnlockAppTemporarily(target.packageName, target.displayName, mins, eod, reason)
                appToUnlock = null
            }
        )
    }

    if (showAddScheduleDialog) {
        AddScheduleDialog(
            onDismiss = { showAddScheduleDialog = false },
            onSave = { schedule ->
                onSaveSchedule(schedule)
                showAddScheduleDialog = false
            }
        )
    }

    if (showAddCustomPackageDialog) {
        AddCustomAppDialog(
            onDismiss = { showAddCustomPackageDialog = false },
            onAdd = { customApp ->
                onToggleAppRestriction(customApp, false)
                showAddCustomPackageDialog = false
            }
        )
    }
}

@Composable
private fun DiagnosticRow(label: String, isOk: Boolean) {
    val saviorColors = LocalSaviorColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        CalmStatusBadge(
            label = if (isOk) "Active" else "Off",
            isPositive = isOk
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ManualUnlockDialog(
    appName: String,
    onDismiss: () -> Unit,
    onConfirmUnlock: (minutes: Int, endOfDay: Boolean, reason: String) -> Unit
) {
    var minutes by remember { mutableIntStateOf(15) }
    var endOfDay by remember { mutableStateOf(false) }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unlock $appName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Choose how long you want to temporarily unlock $appName:",
                    style = MaterialTheme.typography.bodyMedium
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5, 15, 30).forEach { m ->
                        FilterChip(
                            selected = !endOfDay && minutes == m,
                            onClick = {
                                endOfDay = false
                                minutes = m
                            },
                            label = { Text("$m min") }
                        )
                    }
                    FilterChip(
                        selected = endOfDay,
                        onClick = { endOfDay = true },
                        label = { Text("Until end of day") }
                    )
                }
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Optional reason") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirmUnlock(minutes, endOfDay, reason.trim()) }) {
                Text("Unlock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddScheduleDialog(
    onDismiss: () -> Unit,
    onSave: (BlockingScheduleEntity) -> Unit
) {
    var presetIndex by remember { mutableIntStateOf(0) }
    var customLabel by remember { mutableStateOf("Late-Night Impulse Guard") }
    var startHour by remember { mutableIntStateOf(20) }
    var endHour by remember { mutableIntStateOf(23) }
    var weekdaysOnly by remember { mutableStateOf(false) }

    val presets = listOf(
        "Evening & Late Night (8:00 PM – 11:59 PM)" to ScheduleMode.EVENING_CUTOFF,
        "Work & Study Hours (9:00 AM – 6:00 PM)" to ScheduleMode.WORK_STUDY,
        "Custom Hours Window" to ScheduleMode.TIME_WINDOW,
        "Always Block" to ScheduleMode.ALWAYS
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Blocking Schedule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                presets.forEachIndexed { idx, (label, _) ->
                    FilterChip(
                        selected = presetIndex == idx,
                        onClick = {
                            presetIndex = idx
                            when (idx) {
                                0 -> {
                                    customLabel = "After 8:00 PM Evening Guard"
                                    startHour = 20
                                    endHour = 23
                                }
                                1 -> {
                                    customLabel = "Work & Study Focus"
                                    startHour = 9
                                    endHour = 18
                                    weekdaysOnly = true
                                }
                                2 -> {
                                    customLabel = "Selected Hours"
                                }
                                3 -> {
                                    customLabel = "Always Active"
                                }
                            }
                        },
                        label = { Text(label) }
                    )
                }

                OutlinedTextField(
                    value = customLabel,
                    onValueChange = { customLabel = it },
                    label = { Text("Schedule label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (presetIndex in 0..2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Start hour: $startHour:00", style = MaterialTheme.typography.bodySmall)
                        Row {
                            TextButton(onClick = { startHour = (startHour - 1).coerceIn(0, 23) }) { Text("-") }
                            TextButton(onClick = { startHour = (startHour + 1).coerceIn(0, 23) }) { Text("+") }
                        }
                    }
                }
                if (presetIndex in 1..2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("End hour: $endHour:00", style = MaterialTheme.typography.bodySmall)
                        Row {
                            TextButton(onClick = { endHour = (endHour - 1).coerceIn(0, 23) }) { Text("-") }
                            TextButton(onClick = { endHour = (endHour + 1).coerceIn(0, 23) }) { Text("+") }
                        }
                    }
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !weekdaysOnly,
                        onClick = { weekdaysOnly = false },
                        label = { Text("Every day") }
                    )
                    FilterChip(
                        selected = weekdaysOnly,
                        onClick = { weekdaysOnly = true },
                        label = { Text("Weekdays (Mon–Fri)") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val mode = presets[presetIndex].second
                    val mask = if (weekdaysOnly) 0b0011111 else 0b1111111
                    onSave(
                        BlockingScheduleEntity(
                            label = customLabel.trim().ifEmpty { "Mindful Schedule" },
                            mode = mode,
                            startMinuteOfDay = startHour * 60,
                            endMinuteOfDay = endHour * 60 + 59,
                            daysMask = mask,
                            isEnabled = true
                        )
                    )
                }
            ) {
                Text("Save Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddCustomAppDialog(
    onDismiss: () -> Unit,
    onAdd: (InstalledAppInfo) -> Unit
) {
    var displayName by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Any Application") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Enter the application name and Android package identifier (or leave package blank to generate one from the name).",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("App Name (e.g., Dunzo, Myntra)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("Package Name (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = displayName.isNotBlank(),
                onClick = {
                    val cleanName = displayName.trim()
                    val cleanPkg = packageName.trim().ifEmpty {
                        "custom.app." + cleanName.lowercase().replace(Regex("[^a-z0-9]+"), "")
                    }
                    onAdd(
                        InstalledAppInfo(
                            packageName = cleanPkg,
                            displayName = cleanName,
                            suggestedCategory = InstalledAppsProvider.categorize(cleanPkg, cleanName),
                            icon = null
                        )
                    )
                }
            ) {
                Text("Add to Paused Apps")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun formatScheduleSummary(schedule: BlockingScheduleEntity): String {
    val days = when (schedule.daysMask) {
        127 -> "Every day"
        31 -> "Mon–Fri"
        96 -> "Weekends"
        else -> "Selected days"
    }
    return when (schedule.mode) {
        ScheduleMode.ALWAYS -> "Always active • $days"
        ScheduleMode.EVENING_CUTOFF ->
            "After ${BlockingRuleEngine.formatMinutesOfDay(schedule.startMinuteOfDay)} • $days"
        else ->
            "${BlockingRuleEngine.formatMinutesOfDay(schedule.startMinuteOfDay)} – ${
                BlockingRuleEngine.formatMinutesOfDay(schedule.endMinuteOfDay)
            } • $days"
    }
}
