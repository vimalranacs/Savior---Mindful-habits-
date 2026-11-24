package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.blocking.PauseInterruptionActivity
import com.example.notifications.NotificationChannels
import com.example.presentation.SaviorUiState
import com.example.presentation.SaviorViewModel
import com.example.presentation.screens.AccessibilityDisclosureDialog
import com.example.presentation.screens.InsightsScreen
import com.example.presentation.screens.ManifestoScreen
import com.example.presentation.screens.OnboardingScreen
import com.example.presentation.screens.PlanScreen
import com.example.presentation.screens.SettingsPrivacyLegalScreen
import com.example.presentation.screens.ShieldScreen
import com.example.presentation.screens.TodayScreen
import com.example.ui.theme.SaviorTheme

enum class SaviorDestination(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    TODAY("Today", Icons.Filled.Home, Icons.Outlined.Home, "nav_today"),
    SHIELD("Pause", Icons.Filled.Shield, Icons.Outlined.Shield, "nav_shield"),
    PLAN("Plan", Icons.Filled.ShoppingBag, Icons.Outlined.ShoppingBag, "nav_plan"),
    INSIGHTS("Insights", Icons.Filled.Insights, Icons.Outlined.Insights, "nav_insights"),
    MANIFESTO("Read", Icons.Filled.MenuBook, Icons.Outlined.MenuBook, "nav_manifesto")
}

class MainActivity : ComponentActivity() {

    private val viewModel: SaviorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialDestination = parseNotificationDestination(intent)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val snackbarMsg by viewModel.snackbarMessage.collectAsStateWithLifecycle()

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        viewModel.refreshDiagnostics()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            SaviorTheme(themePreference = uiState.settings.themePreference) {
                SaviorAppRoot(
                    uiState = uiState,
                    initialDestination = initialDestination,
                    snackbarMessage = snackbarMsg,
                    onClearSnackbar = viewModel::clearSnackbar,
                    viewModel = viewModel
                )
            }
        }
    }

    private fun parseNotificationDestination(intent: Intent?): SaviorDestination {
        return when (intent?.getStringExtra(NotificationChannels.EXTRA_OPEN_DESTINATION)) {
            NotificationChannels.DEST_EVENING_PLAN -> SaviorDestination.PLAN
            NotificationChannels.DEST_INSIGHTS -> SaviorDestination.INSIGHTS
            else -> SaviorDestination.TODAY
        }
    }
}

@Composable
fun SaviorAppRoot(
    uiState: SaviorUiState,
    initialDestination: SaviorDestination = SaviorDestination.TODAY,
    snackbarMessage: String?,
    onClearSnackbar: () -> Unit,
    viewModel: SaviorViewModel
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var currentDestination by rememberSaveable { mutableStateOf(initialDestination) }
    var planInitialTab by rememberSaveable { mutableIntStateOf(0) }
    var showSettingsHub by rememberSaveable { mutableStateOf(false) }
    var showAccessibilityDisclosure by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(snackbarMessage) {
        if (!snackbarMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(snackbarMessage)
            onClearSnackbar()
        }
    }

    if (!uiState.isReady) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    if (!uiState.settings.onboardingCompleted) {
        OnboardingScreen(
            onFinishOnboarding = { viewModel.completeOnboarding() }
        )
        return
    }

    if (showAccessibilityDisclosure) {
        AccessibilityDisclosureDialog(
            onConfirmAndOpenSettings = {
                showAccessibilityDisclosure = false
                viewModel.acceptAccessibilityDisclosureAndOpenSettings()
            },
            onDismiss = { showAccessibilityDisclosure = false }
        )
    }

    // Handle system back navigation from secondary destinations
    if (!showSettingsHub && currentDestination != SaviorDestination.TODAY) {
        BackHandler { currentDestination = SaviorDestination.TODAY }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!isWideScreen && !showSettingsHub) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        SaviorDestination.entries.forEach { dest ->
                            val selected = currentDestination == dest
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    showSettingsHub = false
                                    currentDestination = dest
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen && !showSettingsHub) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        SaviorDestination.entries.forEach { dest ->
                            val selected = currentDestination == dest
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    showSettingsHub = false
                                    currentDestination = dest
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(modifier = Modifier.widthIn(max = 760.dp)) {
                        if (showSettingsHub) {
                            SettingsPrivacyLegalScreen(
                                uiState = uiState,
                                onBack = { showSettingsHub = false },
                                onSetTheme = viewModel::setThemePreference,
                                onSetCurrency = viewModel::setCurrency,
                                onSetCooldownSeconds = viewModel::setCooldownSeconds,
                                onSaveNotificationSettings = viewModel::saveNotificationSettings,
                                onGenerateExportJson = viewModel::generateExportJson,
                                onResetSettings = viewModel::resetSettings,
                                onDeleteAllData = viewModel::deleteAllUserData,
                                onRequestEnableAccessibility = { showAccessibilityDisclosure = true },
                                onRefreshDiagnostics = viewModel::refreshDiagnostics
                            )
                        } else {
                            when (currentDestination) {
                                SaviorDestination.TODAY -> TodayScreen(
                                    uiState = uiState,
                                    onOpenShield = { currentDestination = SaviorDestination.SHIELD },
                                    onOpenEveningPlan = {
                                        planInitialTab = 1
                                        currentDestination = SaviorDestination.PLAN
                                    },
                                    onOpenInsights = { currentDestination = SaviorDestination.INSIGHTS },
                                    onOpenManifesto = { currentDestination = SaviorDestination.MANIFESTO },
                                    onOpenSettings = { showSettingsHub = true },
                                    onRequestEnableAccessibility = { showAccessibilityDisclosure = true },
                                    onPreviewPauseScreen = { sampleAppName ->
                                        context.startActivity(
                                            PauseInterruptionActivity.createIntent(
                                                context = context,
                                                targetPackage = "preview.package",
                                                targetAppName = sampleAppName,
                                                scheduleLabel = "Preview Mode",
                                                isPreview = true
                                            )
                                        )
                                    },
                                    onSetGlobalPauseMinutes = viewModel::setGlobalPauseMinutes,
                                    onRelockApp = viewModel::relockAppImmediately
                                )

                                SaviorDestination.SHIELD -> ShieldScreen(
                                    uiState = uiState,
                                    onToggleAppRestriction = viewModel::toggleAppRestriction,
                                    onToggleRestrictedAppEnabled = viewModel::toggleRestrictedAppEnabled,
                                    onUnlockAppTemporarily = viewModel::unlockAppTemporarily,
                                    onRelockApp = viewModel::relockAppImmediately,
                                    onSetGlobalPauseMinutes = viewModel::setGlobalPauseMinutes,
                                    onSaveSchedule = viewModel::saveSchedule,
                                    onToggleSchedule = viewModel::toggleSchedule,
                                    onDeleteSchedule = viewModel::deleteSchedule,
                                    onRequestEnableAccessibility = { showAccessibilityDisclosure = true },
                                    onRefreshAppsAndDiagnostics = {
                                        viewModel.refreshInstalledApps()
                                        viewModel.refreshDiagnostics()
                                    },
                                    onPreviewPauseScreen = { sampleAppName ->
                                        context.startActivity(
                                            PauseInterruptionActivity.createIntent(
                                                context = context,
                                                targetPackage = "preview.package",
                                                targetAppName = sampleAppName,
                                                scheduleLabel = "Preview Mode",
                                                isPreview = true
                                            )
                                        )
                                    }
                                )

                                SaviorDestination.PLAN -> PlanScreen(
                                    uiState = uiState,
                                    initialTab = planInitialTab,
                                    onCreateList = viewModel::createGroceryList,
                                    onDeleteList = viewModel::deleteGroceryList,
                                    onSaveItem = viewModel::saveGroceryItem,
                                    onAddSuggestion = viewModel::addSuggestedItemToList,
                                    onToggleChecked = viewModel::toggleGroceryItemChecked,
                                    onToggleFavourite = viewModel::toggleGroceryItemFavourite,
                                    onToggleRunningLow = viewModel::toggleGroceryItemRunningLow,
                                    onToggleAvailableAtHome = viewModel::toggleGroceryItemAvailableAtHome,
                                    onDeleteItem = viewModel::deleteGroceryItem,
                                    onSaveEveningPlan = viewModel::saveEveningPlan
                                )

                                SaviorDestination.INSIGHTS -> InsightsScreen(
                                    uiState = uiState,
                                    onAddSpendingEntry = viewModel::addSpendingEntry,
                                    onDeleteSpendingEntry = viewModel::deleteSpendingEntry
                                )

                                SaviorDestination.MANIFESTO -> ManifestoScreen()
                            }
                        }
                    }
                }
            }
        }
    }
}
