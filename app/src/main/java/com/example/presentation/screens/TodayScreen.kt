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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.BlockingRuleEngine
import com.example.presentation.SaviorUiState
import com.example.presentation.components.CalmCard
import com.example.presentation.components.CalmStatusBadge
import com.example.ui.theme.BrandConfig
import com.example.ui.theme.LocalSaviorColors
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodayScreen(
    uiState: SaviorUiState,
    onOpenShield: () -> Unit,
    onOpenEveningPlan: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenManifesto: () -> Unit,
    onOpenSettings: () -> Unit,
    onRequestEnableAccessibility: () -> Unit,
    onPreviewPauseScreen: (appName: String) -> Unit,
    onSetGlobalPauseMinutes: (Int) -> Unit,
    onRelockApp: (packageName: String, appName: String) -> Unit
) {
    val saviorColors = LocalSaviorColors.current
    val now = System.currentTimeMillis()
    val isGloballyPaused = uiState.settings.globalPauseUntilEpochMs > now
    val activeRestrictedApps = uiState.restrictedApps.filter { it.isEnabled }
    val runningLowItems = uiState.groceryItems.filter { !it.isChecked && !it.isAvailableAtHome && it.isRunningLow }
    val uncheckedItems = uiState.groceryItems.filter { !it.isChecked && !it.isAvailableAtHome }
    val serviceActive = uiState.diagnostics?.accessibilityEnabledInSettings == true

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("today_screen_list"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top header bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Spa,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = BrandConfig.APP_NAME,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = BrandConfig.TAGLINE,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("open_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings, Privacy & Legal",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Calm Status & Blocking Sanctuary Card
        item {
            CalmCard(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CalmStatusBadge(
                        label = when {
                            isGloballyPaused -> "Restrictions paused temporarily"
                            serviceActive && activeRestrictedApps.isNotEmpty() ->
                                "${activeRestrictedApps.size} ${if (activeRestrictedApps.size == 1) "app" else "apps"} guarded"
                            activeRestrictedApps.isNotEmpty() ->
                                "${activeRestrictedApps.size} selected • Service off"
                            else -> "No apps selected yet"
                        },
                        icon = Icons.Outlined.Shield,
                        isPositive = serviceActive && !isGloballyPaused && activeRestrictedApps.isNotEmpty()
                    )

                    TextButton(
                        onClick = {
                            val sampleName = activeRestrictedApps.firstOrNull()?.displayName ?: "Quick Commerce"
                            onPreviewPauseScreen(sampleName)
                        },
                        modifier = Modifier.testTag("preview_pause_button")
                    ) {
                        Text("Preview Pause")
                    }
                }

                Text(
                    text = when {
                        isGloballyPaused -> {
                            val minsLeft = TimeUnit.MILLISECONDS.toMinutes(
                                (uiState.settings.globalPauseUntilEpochMs - now).coerceAtLeast(60_000L)
                            )
                            "Mindful pauses are resting for ~$minsLeft more min."
                        }
                        activeRestrictedApps.isEmpty() ->
                            "Choose the delivery or shopping apps where you want a calm moment before ordering."
                        !serviceActive ->
                            "Your app list is ready. Enable the Mindful Pause Service whenever you want automatic pauses."
                        else ->
                            "Whenever you open a selected app, ${BrandConfig.APP_NAME} will offer a brief, non-judgmental pause."
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Active temporary unlocks or global pause override controls
                val currentlyUnlockedApps = activeRestrictedApps.filter { it.unlockedUntilEpochMs > now }
                if (currentlyUnlockedApps.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        currentlyUnlockedApps.forEach { unlockedApp ->
                            val minsRemaining = TimeUnit.MILLISECONDS.toMinutes(
                                (unlockedApp.unlockedUntilEpochMs - now).coerceAtLeast(60_000L)
                            )
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, saviorColors.borderSubtle),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.LockClock,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "${unlockedApp.displayName} unlocked (${minsRemaining}m left)",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            onRelockApp(unlockedApp.packageName, unlockedApp.displayName)
                                        }
                                    ) {
                                        Text("Re-lock")
                                    }
                                }
                            }
                        }
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onOpenShield,
                        modifier = Modifier.testTag("today_choose_apps_button"),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(if (activeRestrictedApps.isEmpty()) "Select Apps to Pause" else "Manage Paused Apps")
                    }

                    if (!serviceActive && activeRestrictedApps.isNotEmpty()) {
                        OutlinedButton(
                            onClick = onRequestEnableAccessibility,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.testTag("today_enable_service_button")
                        ) {
                            Text("Enable Pause Service")
                        }
                    }

                    if (isGloballyPaused) {
                        OutlinedButton(
                            onClick = { onSetGlobalPauseMinutes(0) },
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PlayCircle,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Resume Pauses")
                        }
                    } else if (activeRestrictedApps.isNotEmpty()) {
                        AssistChip(
                            onClick = { onSetGlobalPauseMinutes(30) },
                            label = { Text("Pause all for 30m") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.PauseCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }
        }

        // Evening Routine Card: Plan Before You Order
        item {
            CalmCard(
                containerColor = MaterialTheme.colorScheme.surface,
                onClick = onOpenEveningPlan,
                modifier = Modifier.testTag("today_evening_plan_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Nightlight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "EVENING ROUTINE • ${
                                BlockingRuleEngine.formatMinutesOfDay(
                                    uiState.settings.eveningReminderHour * 60 + uiState.settings.eveningReminderMinute
                                )
                            }",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "Open evening grocery plan",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = "Plan before you order",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = when {
                        runningLowItems.isNotEmpty() ->
                            "${runningLowItems.size} ${if (runningLowItems.size == 1) "item is" else "items are"} marked running low (${runningLowItems.take(3).joinToString { it.name }})."
                        uncheckedItems.isNotEmpty() ->
                            "${uncheckedItems.size} ${if (uncheckedItems.size == 1) "essential" else "essentials"} ready on your shopping list for tomorrow."
                        else ->
                            "Take two quiet minutes tonight to check your kitchen and list tomorrow's essentials."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ShoppingBag,
                        contentDescription = null,
                        tint = saviorColors.positive,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Open evening check-in & grocery lists",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Calm Weekly Habit Reflection Card (No streaks, no gamified scores)
        item {
            val weekly = uiState.weeklyHabitSummary
            CalmCard(
                containerColor = MaterialTheme.colorScheme.surface,
                onClick = onOpenInsights
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "QUIET PROGRESS • THIS WEEK",
                        style = MaterialTheme.typography.labelMedium,
                        color = saviorColors.positive
                    )
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = saviorColors.positive,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = weekly.encouragingNote,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuietMetricTile(
                        value = weekly.timesReturnedInsteadOfOrdering.toString(),
                        label = "Mindful returns",
                        modifier = Modifier.weight(1f)
                    )
                    QuietMetricTile(
                        value = weekly.plannedShoppingSessions.toString(),
                        label = "Evening plans",
                        modifier = Modifier.weight(1f)
                    )
                    QuietMetricTile(
                        value = weekly.daysFollowingRestrictions.toString(),
                        label = "Intentional days",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Manifesto Teaser Card
        item {
            CalmCard(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                onClick = onOpenManifesto
            ) {
                Text(
                    text = BrandConfig.MANIFESTO_SECTION_TITLE,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "“We built technology to make life easier. Somewhere along the way, ease began to replace intention.”",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Read the manifesto and explore how frictionless commerce and digital footprints work.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

@Composable
private fun QuietMetricTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
