package com.example.presentation.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.blocking.BlockingDiagnosticsHelper
import com.example.domain.BlockingRuleEngine
import com.example.legal.LegalDocument
import com.example.legal.LegalDocuments
import com.example.presentation.SaviorUiState
import com.example.presentation.components.CalmCard
import com.example.presentation.components.CalmStatusBadge
import com.example.ui.theme.BrandConfig
import com.example.ui.theme.LocalSaviorColors
import com.example.ui.theme.ThemePreference
import kotlinx.coroutines.launch

private val currencyOptions = listOf(
    "INR" to "₹",
    "USD" to "$",
    "EUR" to "€",
    "GBP" to "£"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsPrivacyLegalScreen(
    uiState: SaviorUiState,
    onBack: () -> Unit,
    onSetTheme: (ThemePreference) -> Unit,
    onSetCurrency: (code: String, symbol: String) -> Unit,
    onSetCooldownSeconds: (Int) -> Unit,
    onSaveNotificationSettings: (
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
    ) -> Unit,
    onGenerateExportJson: suspend () -> String,
    onResetSettings: () -> Unit,
    onDeleteAllData: () -> Unit,
    onRequestEnableAccessibility: () -> Unit,
    onRefreshDiagnostics: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = uiState.settings
    val saviorColors = LocalSaviorColors.current

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var activeLegalDoc by remember { mutableStateOf<LegalDocument?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var exportStatusNote by remember { mutableStateOf("") }

    // Notification editable states
    var eveningEnabled by remember(settings) { mutableStateOf(settings.eveningReminderEnabled) }
    var eveningHour by remember(settings) { mutableIntStateOf(settings.eveningReminderHour) }
    var eveningMinute by remember(settings) { mutableIntStateOf(settings.eveningReminderMinute) }
    var eveningTitle by remember(settings) { mutableStateOf(settings.eveningReminderTitle) }
    var eveningBody by remember(settings) { mutableStateOf(settings.eveningReminderBody) }

    var runningLowEnabled by remember(settings) { mutableStateOf(settings.runningLowReminderEnabled) }
    var runningLowBody by remember(settings) { mutableStateOf(settings.runningLowReminderBody) }

    var weeklySpendingEnabled by remember(settings) { mutableStateOf(settings.weeklySpendingReminderEnabled) }
    var weeklySpendingBody by remember(settings) { mutableStateOf(settings.weeklySpendingReminderBody) }

    var weeklyReflectionEnabled by remember(settings) { mutableStateOf(settings.weeklyReflectionReminderEnabled) }
    var weeklyReflectionBody by remember(settings) { mutableStateOf(settings.weeklyReflectionReminderBody) }

    var quietEnabled by remember(settings) { mutableStateOf(settings.quietHoursEnabled) }
    var quietStart by remember(settings) { mutableIntStateOf(settings.quietStartHour) }
    var quietEnd by remember(settings) { mutableIntStateOf(settings.quietEndHour) }
    var dailyCap by remember(settings) { mutableIntStateOf(settings.dailyNotificationCap) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { onRefreshDiagnostics() }
    )

    val exportDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri ->
            if (uri != null) {
                scope.launch {
                    val json = onGenerateExportJson()
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(json.toByteArray(Charsets.UTF_8))
                        }
                    }.onSuccess {
                        exportStatusNote = "Local data exported cleanly to your chosen file."
                    }.onFailure {
                        exportStatusNote = "Could not write export file."
                    }
                }
            }
        }
    )

    if (activeLegalDoc != null) {
        val doc = activeLegalDoc!!
        BackHandler { activeLegalDoc = null }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = { activeLegalDoc = null }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back to Legal list"
                        )
                    }
                    Column {
                        Text(text = doc.title, style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = doc.lastUpdatedNote,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                CalmCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(
                        text = LegalDocuments.draftNotice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(doc.sections) { sec ->
                CalmCard {
                    Text(
                        text = sec.heading,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = sec.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen_list"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Return to previous screen"
                    )
                }
                Column {
                    Text(
                        text = "Preferences, Privacy & Legal",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "${BrandConfig.APP_NAME} • 100% local & offline",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            val tabs = listOf("General", "Reminders", "Privacy", "Legal")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                tabs.forEachIndexed { idx, label ->
                    SegmentedButton(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = tabs.size),
                        modifier = Modifier.testTag("settings_tab_$idx")
                    ) {
                        Text(label, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        when (selectedTab) {
            0 -> {
                // Appearance, Currency & Unlock Cooldown
                item {
                    CalmCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text("Appearance & Theme", style = MaterialTheme.typography.titleMedium)
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                ThemePreference.LIGHT to "Calm Light (Default)",
                                ThemePreference.DARK to "Midnight Slate Dark",
                                ThemePreference.SYSTEM to "Follow System"
                            ).forEach { (pref, label) ->
                                FilterChip(
                                    selected = settings.themePreference == pref,
                                    onClick = { onSetTheme(pref) },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("theme_${pref.name}")
                                )
                            }
                        }
                    }
                }

                item {
                    CalmCard {
                        Text("Currency Symbol", style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            currencyOptions.forEach { (code, symbol) ->
                                FilterChip(
                                    selected = settings.currencyCode == code,
                                    onClick = { onSetCurrency(code, symbol) },
                                    label = { Text("$code ($symbol)") }
                                )
                            }
                        }
                    }
                }

                item {
                    CalmCard {
                        Text(
                            text = "Unlock Reflection Cooldown: ${settings.cooldownSeconds} seconds",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "How long the 'Unlock Anyway' button waits so you can take one calm breath before bypassing a pause.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0, 5, 15, 30, 60).forEach { secs ->
                                FilterChip(
                                    selected = settings.cooldownSeconds == secs,
                                    onClick = { onSetCooldownSeconds(secs) },
                                    label = { Text(if (secs == 0) "No cooldown" else "${secs}s") }
                                )
                            }
                        }
                    }
                }
            }

            1 -> {
                // Reminders, Quiet Hours & Frequency Caps
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    uiState.diagnostics?.notificationPermissionGranted == false
                ) {
                    item {
                        CalmCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                            Text(
                                text = "Android 13+ Notification Permission",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Grant notification permission only if you want evening grocery planning or running-low reminders.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Button(
                                onClick = {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            ) {
                                Text("Allow Gentle Reminders")
                            }
                        }
                    }
                }

                item {
                    CalmCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("1. Evening Grocery Planning", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Scheduled for ${
                                        BlockingRuleEngine.formatMinutesOfDay(eveningHour * 60 + eveningMinute)
                                    }",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = eveningEnabled, onCheckedChange = { eveningEnabled = it })
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(18 to 30, 19 to 30, 20 to 30, 21 to 0).forEach { (h, m) ->
                                FilterChip(
                                    selected = eveningHour == h && eveningMinute == m,
                                    onClick = {
                                        eveningHour = h
                                        eveningMinute = m
                                    },
                                    label = { Text(BlockingRuleEngine.formatMinutesOfDay(h * 60 + m)) }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = eveningTitle,
                            onValueChange = { eveningTitle = it },
                            label = { Text("Editable reminder title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = eveningBody,
                            onValueChange = { eveningBody = it },
                            label = { Text("Editable reminder message") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                }

                item {
                    CalmCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("2. Running-Low Item Reminders", style = MaterialTheme.typography.titleMedium)
                            Switch(checked = runningLowEnabled, onCheckedChange = { runningLowEnabled = it })
                        }
                        OutlinedTextField(
                            value = runningLowBody,
                            onValueChange = { runningLowBody = it },
                            label = { Text("Editable message") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        HorizontalDivider(color = saviorColors.borderSubtle)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("3. Weekly Spending Review (Optional)", style = MaterialTheme.typography.titleMedium)
                            Switch(checked = weeklySpendingEnabled, onCheckedChange = { weeklySpendingEnabled = it })
                        }
                        OutlinedTextField(
                            value = weeklySpendingBody,
                            onValueChange = { weeklySpendingBody = it },
                            label = { Text("Editable message") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        HorizontalDivider(color = saviorColors.borderSubtle)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("4. Weekly Mindful Reflection (Optional)", style = MaterialTheme.typography.titleMedium)
                            Switch(checked = weeklyReflectionEnabled, onCheckedChange = { weeklyReflectionEnabled = it })
                        }
                        OutlinedTextField(
                            value = weeklyReflectionBody,
                            onValueChange = { weeklyReflectionBody = it },
                            label = { Text("Editable message") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    CalmCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Quiet & Sleep Hours", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Suppresses all notifications between ${quietStart}:00 and 0${quietEnd}:00",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = quietEnabled, onCheckedChange = { quietEnabled = it })
                        }
                        Text(
                            text = "Daily notification limit: max $dailyCap per day",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = dailyCap.toFloat(),
                            onValueChange = { dailyCap = it.toInt().coerceIn(1, 5) },
                            valueRange = 1f..5f,
                            steps = 3
                        )
                        Button(
                            onClick = {
                                onSaveNotificationSettings(
                                    eveningEnabled,
                                    eveningHour,
                                    eveningMinute,
                                    eveningTitle,
                                    eveningBody,
                                    runningLowEnabled,
                                    runningLowBody,
                                    weeklySpendingEnabled,
                                    weeklySpendingBody,
                                    weeklyReflectionEnabled,
                                    weeklyReflectionBody,
                                    quietEnabled,
                                    quietStart,
                                    quietEnd,
                                    dailyCap
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_reminder_settings_button")
                        ) {
                            Icon(imageVector = Icons.Outlined.NotificationsNone, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Save Reminder Preferences")
                        }
                    }
                }
            }

            2 -> {
                // Privacy Center & Data Controls
                item {
                    CalmCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text("Privacy Architecture Summary", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            text = "• Zero INTERNET permission declared in AndroidManifest.xml.\n" +
                                "• No account, no cloud backend, no ads, no third-party analytics SDKs.\n" +
                                "• Excluded from Android Auto Backup by default.\n" +
                                "• Keystore AES-256-GCM active: ${if (uiState.isKeystoreBacked) "Hardware/OS Keystore Ready" else "Local Fallback"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    CalmCard {
                        Text("Your Data Controls", style = MaterialTheme.typography.titleMedium)
                        if (exportStatusNote.isNotBlank()) {
                            Text(
                                text = exportStatusNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = saviorColors.positive
                            )
                        }

                        Button(
                            onClick = {
                                val fileName = "${BrandConfig.EXPORT_FILE_PREFIX}_${System.currentTimeMillis()}.json"
                                exportDocumentLauncher.launch(fileName)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("export_data_button")
                        ) {
                            Icon(imageVector = Icons.Outlined.FileDownload, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Export My Data (JSON)")
                        }

                        OutlinedButton(
                            onClick = { showResetConfirmDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reset_settings_button")
                        ) {
                            Icon(imageVector = Icons.Outlined.RestartAlt, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Reset Application Settings")
                        }

                        Button(
                            onClick = { showDeleteConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("delete_all_data_button")
                        ) {
                            Icon(imageVector = Icons.Outlined.DeleteForever, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Delete All My Data")
                        }
                    }
                }

                item {
                    CalmCard {
                        Text("Required Permissions Inspector", style = MaterialTheme.typography.titleMedium)
                        PermissionExplainRow(
                            name = "Accessibility Service (Optional)",
                            status = if (uiState.diagnostics?.accessibilityEnabledInSettings == true) "Enabled" else "Off",
                            purpose = "Inspects only foreground package names to trigger your pause screen. Never reads screen contents.",
                            onAction = onRequestEnableAccessibility
                        )
                        HorizontalDivider(color = saviorColors.borderSubtle)
                        PermissionExplainRow(
                            name = "Notifications (POST_NOTIFICATIONS)",
                            status = if (uiState.diagnostics?.notificationPermissionGranted == true) "Granted" else "Off",
                            purpose = "Delivers optional evening planning and running-low reminders.",
                            onAction = { BlockingDiagnosticsHelper.openAppNotificationSettings(context) }
                        )
                        HorizontalDivider(color = saviorColors.borderSubtle)
                        PermissionExplainRow(
                            name = "Boot Completed (Install-time)",
                            status = "Declared",
                            purpose = "Reschedules local WorkManager reminders after your phone restarts.",
                            onAction = null
                        )
                        HorizontalDivider(color = saviorColors.borderSubtle)
                        PermissionExplainRow(
                            name = "Internet Access",
                            status = "Not Requested",
                            purpose = "Intentionally omitted so your data can never leave your device.",
                            onAction = null
                        )
                    }
                }
            }

            else -> {
                // Legal Documents Hub
                item {
                    CalmCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Gavel,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text("Legal & Policy Center", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            text = LegalDocuments.draftNotice,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(LegalDocuments.all, key = { it.id }) { doc ->
                    CalmCard(
                        onClick = { activeLegalDoc = doc },
                        modifier = Modifier.testTag("legal_doc_${doc.id}")
                    ) {
                        Text(
                            text = doc.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = doc.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reset Application Settings?") },
            text = {
                Text("This restores default theme, cooldown, and reminder settings while keeping your grocery lists and recorded spending intact.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetSettings()
                        showResetConfirmDialog = false
                    }
                ) {
                    Text("Reset Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete All Local Data?") },
            text = {
                Text("This permanently erases all selected apps, custom schedules, pause history, grocery lists, evening plans, and spending records from your device.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAllData()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_all_button")
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PermissionExplainRow(
    name: String,
    status: String,
    purpose: String,
    onAction: (() -> Unit)?
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = name, style = MaterialTheme.typography.titleSmall)
            CalmStatusBadge(
                label = status,
                isPositive = status == "Enabled" || status == "Granted" || status == "Not Requested"
            )
        }
        Text(
            text = purpose,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (onAction != null) {
            TextButton(onClick = onAction) {
                Text("Configure in Android Settings")
            }
        }
    }
}
