package com.example.blocking

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.SaviorApplication
import com.example.data.local.PauseAction
import com.example.data.local.PauseEventEntity
import com.example.data.preferences.UserSettings
import com.example.domain.BlockingRuleEngine
import com.example.domain.ReflectionPrompts
import com.example.ui.theme.BrandConfig
import com.example.ui.theme.LocalSaviorColors
import com.example.ui.theme.SaviorTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class PauseStep {
    REFLECT,
    BREATHE_MINUTE,
    UNLOCK_OPTIONS
}

class PauseInterruptionActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val targetPackage = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()
        val targetAppName = intent.getStringExtra(EXTRA_APP_NAME).orEmpty().ifBlank { "this app" }
        val scheduleLabel = intent.getStringExtra(EXTRA_SCHEDULE_LABEL).orEmpty()
        val isPreview = intent.getBooleanExtra(EXTRA_IS_PREVIEW, false)
        val container = (applicationContext as SaviorApplication).container

        setContent {
            val settings by container.preferencesRepository.settingsFlow
                .collectAsStateWithLifecycle(initialValue = UserSettings())
            val scope = rememberCoroutineScope()

            SaviorTheme(themePreference = settings.themePreference) {
                PauseInterruptionScreen(
                    appName = targetAppName,
                    scheduleLabel = scheduleLabel,
                    cooldownSeconds = settings.cooldownSeconds,
                    isPreview = isPreview,
                    onGoBack = {
                        scope.launch {
                            if (!isPreview && targetPackage.isNotBlank()) {
                                container.database.pauseEventDao().insert(
                                    PauseEventEntity(
                                        packageName = targetPackage,
                                        appName = targetAppName,
                                        action = PauseAction.RETURNED
                                    )
                                )
                            }
                            navigateBackSafely(isPreview)
                        }
                    },
                    onCompletedMinute = {
                        scope.launch {
                            if (!isPreview && targetPackage.isNotBlank()) {
                                container.database.pauseEventDao().insert(
                                    PauseEventEntity(
                                        packageName = targetPackage,
                                        appName = targetAppName,
                                        action = PauseAction.WAITED_MINUTE
                                    )
                                )
                            }
                        }
                    },
                    onUnlock = { minutes, endOfDay, reason ->
                        scope.launch {
                            if (!isPreview && targetPackage.isNotBlank()) {
                                val now = System.currentTimeMillis()
                                val until = if (endOfDay) {
                                    BlockingRuleEngine.computeEndOfDayEpochMs(now)
                                } else {
                                    now + minutes * 60_000L
                                }
                                container.database.restrictedAppDao().updateUnlock(
                                    packageName = targetPackage,
                                    untilMs = until,
                                    reason = reason
                                )
                                container.database.pauseEventDao().insert(
                                    PauseEventEntity(
                                        packageName = targetPackage,
                                        appName = targetAppName,
                                        action = PauseAction.UNLOCKED,
                                        unlockMinutes = if (endOfDay) -1 else minutes,
                                        reason = reason
                                    )
                                )
                                // Return cleanly to target app
                                val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)
                                if (launchIntent != null) {
                                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    runCatching { startActivity(launchIntent) }
                                }
                            }
                            finish()
                        }
                    }
                )
            }
        }
    }

    private fun navigateBackSafely(isPreview: Boolean) {
        if (!isPreview) {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            runCatching { startActivity(homeIntent) }
        }
        finish()
    }

    companion object {
        private const val EXTRA_PACKAGE = "extra_package"
        private const val EXTRA_APP_NAME = "extra_app_name"
        private const val EXTRA_SCHEDULE_LABEL = "extra_schedule_label"
        private const val EXTRA_IS_PREVIEW = "extra_is_preview"

        fun createIntent(
            context: Context,
            targetPackage: String,
            targetAppName: String,
            scheduleLabel: String = "",
            isPreview: Boolean = false
        ): Intent {
            return Intent(context, PauseInterruptionActivity::class.java).apply {
                putExtra(EXTRA_PACKAGE, targetPackage)
                putExtra(EXTRA_APP_NAME, targetAppName)
                putExtra(EXTRA_SCHEDULE_LABEL, scheduleLabel)
                putExtra(EXTRA_IS_PREVIEW, isPreview)
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PauseInterruptionScreen(
    appName: String,
    scheduleLabel: String,
    cooldownSeconds: Int,
    isPreview: Boolean,
    onGoBack: () -> Unit,
    onCompletedMinute: () -> Unit,
    onUnlock: (minutes: Int, endOfDay: Boolean, reason: String) -> Unit
) {
    val prompt = remember { ReflectionPrompts.pickForAttempt(System.currentTimeMillis()) }
    var step by remember { mutableStateOf(PauseStep.REFLECT) }
    var minuteRemaining by remember { mutableIntStateOf(60) }
    var unlockCooldownRemaining by remember { mutableIntStateOf(cooldownSeconds) }
    var selectedDurationMinutes by remember { mutableIntStateOf(15) }
    var unlockEndOfDay by remember { mutableStateOf(false) }
    var selectedReason by remember { mutableStateOf("") }

    BackHandler { onGoBack() }

    LaunchedEffect(step) {
        if (step == PauseStep.BREATHE_MINUTE) {
            minuteRemaining = 60
            while (minuteRemaining > 0) {
                delay(1000L)
                minuteRemaining--
            }
            onCompletedMinute()
            step = PauseStep.REFLECT
        } else if (step == PauseStep.UNLOCK_OPTIONS) {
            unlockCooldownRemaining = cooldownSeconds.coerceIn(0, 60)
            while (unlockCooldownRemaining > 0) {
                delay(1000L)
                unlockCooldownRemaining--
            }
        }
    }

    val saviorColors = LocalSaviorColors.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Brand & context pill
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, saviorColors.borderSubtle)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Spa,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isPreview) {
                                "${BrandConfig.APP_NAME} Preview • $appName"
                            } else {
                                "${BrandConfig.APP_NAME} • Opening $appName"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                AnimatedContent(targetState = step, label = "pause_step") { currentStep ->
                    when (currentStep) {
                        PauseStep.REFLECT -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                Text(
                                    text = prompt.headline,
                                    style = MaterialTheme.typography.displayMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    textAlign = TextAlign.Center
                                )

                                Surface(
                                    shape = MaterialTheme.shapes.large,
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, saviorColors.borderSubtle),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = prompt.body,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Start,
                                        modifier = Modifier.padding(22.dp)
                                    )
                                }

                                if (scheduleLabel.isNotBlank()) {
                                    Text(
                                        text = "Active boundary: $scheduleLabel",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Button(
                                    onClick = onGoBack,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .testTag("pause_go_back_button"),
                                    shape = MaterialTheme.shapes.medium,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text(
                                        text = "Go Back",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }

                                OutlinedButton(
                                    onClick = { step = PauseStep.BREATHE_MINUTE },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("pause_give_minute_button"),
                                    shape = MaterialTheme.shapes.medium,
                                    border = BorderStroke(1.dp, saviorColors.borderSubtle)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.HourglassTop,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text(
                                        text = "Give Me a Minute",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }

                                TextButton(
                                    onClick = { step = PauseStep.UNLOCK_OPTIONS },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("pause_unlock_anyway_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.LockOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text(
                                        text = "Unlock Anyway",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        PauseStep.BREATHE_MINUTE -> {
                            BreathingReflectionPane(
                                secondsRemaining = minuteRemaining,
                                onGoBack = onGoBack,
                                onReturnToOptions = { step = PauseStep.REFLECT }
                            )
                        }

                        PauseStep.UNLOCK_OPTIONS -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Proceed consciously",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "You always stay in control of your phone. Choose how long to open $appName.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                Surface(
                                    shape = MaterialTheme.shapes.large,
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, saviorColors.borderSubtle),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "Unlock duration",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf(5 to "5 min", 15 to "15 min", 30 to "30 min").forEach { (mins, label) ->
                                                FilterChip(
                                                    selected = !unlockEndOfDay && selectedDurationMinutes == mins,
                                                    onClick = {
                                                        unlockEndOfDay = false
                                                        selectedDurationMinutes = mins
                                                    },
                                                    label = { Text(label) },
                                                    modifier = Modifier.testTag("unlock_duration_$mins")
                                                )
                                            }
                                            FilterChip(
                                                selected = unlockEndOfDay,
                                                onClick = { unlockEndOfDay = true },
                                                label = { Text("Until end of day") },
                                                modifier = Modifier.testTag("unlock_duration_eod")
                                            )
                                        }

                                        Text(
                                            text = "Optional reason (stored locally only)",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            ReflectionPrompts.unlockReasons.forEach { reason ->
                                                FilterChip(
                                                    selected = selectedReason == reason,
                                                    onClick = {
                                                        selectedReason = if (selectedReason == reason) "" else reason
                                                    },
                                                    label = { Text(reason, style = MaterialTheme.typography.labelMedium) }
                                                )
                                            }
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        onUnlock(selectedDurationMinutes, unlockEndOfDay, selectedReason)
                                    },
                                    enabled = unlockCooldownRemaining == 0,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("confirm_unlock_button"),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Text(
                                        text = if (unlockCooldownRemaining > 0) {
                                            "Taking a breath… (${unlockCooldownRemaining}s)"
                                        } else if (unlockEndOfDay) {
                                            "Open $appName until end of day"
                                        } else {
                                            "Open $appName for $selectedDurationMinutes min"
                                        }
                                    )
                                }

                                OutlinedButton(
                                    onClick = onGoBack,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Text("Actually, Go Back")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BreathingReflectionPane(
    secondsRemaining: Int,
    onGoBack: () -> Unit,
    onReturnToOptions: () -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "breath")
    val pulse by infinite.animateFloat(
        initialValue = 0.62f,
        targetValue = 0.96f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_scale"
    )
    val outerColor = MaterialTheme.colorScheme.primaryContainer
    val innerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Take a quiet minute",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Notice whether you truly need something right now, or if a glass of water and a planned grocery list will serve you better.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Box(
            modifier = Modifier
                .size(180.dp)
                .semantics { contentDescription = "$secondsRemaining seconds remaining in reflection" },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val maxRadius = size.minDimension / 2f
                drawCircle(color = outerColor, radius = maxRadius * pulse)
                drawCircle(color = innerColor, radius = maxRadius * 0.65f * pulse)
            }
            Text(
                text = "${secondsRemaining}s",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Button(
            onClick = onGoBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("I don't need it right now — Go Back")
        }

        TextButton(
            onClick = onReturnToOptions,
            modifier = Modifier.height(48.dp)
        ) {
            Text("Back to choices")
        }
    }
}
