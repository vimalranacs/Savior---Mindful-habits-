package com.example.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.presentation.components.CalmCard
import com.example.ui.theme.BrandConfig
import com.example.ui.theme.LocalSaviorColors

@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    val saviorColors = LocalSaviorColors.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .widthIn(max = 540.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Spa,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = BrandConfig.APP_NAME,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                TextButton(
                    onClick = onFinishOnboarding,
                    modifier = Modifier.testTag("onboarding_skip_button")
                ) {
                    Text("Skip")
                }
            }

            AnimatedContent(
                targetState = step,
                label = "onboarding_step",
                modifier = Modifier
                    .widthIn(max = 540.dp)
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) { currentStep ->
                when (currentStep) {
                    0 -> OnboardingPageContent(
                        badge = "01 • INTENTION OVER IMPULSE",
                        headline = "A thoughtful pause between an impulse and an order.",
                        description = "Instant-delivery and shopping apps offer genuine convenience, but frictionless ordering can turn small cravings into automatic habits.\n\n${BrandConfig.APP_NAME} introduces a calm moment of reflection before opening apps you choose to limit—never guilt, never streaks.",
                        highlights = listOf(
                            Icons.Outlined.Spa to "Choose which apps to pause (Blinkit, Zepto, Swiggy, Zomato, Amazon, or any app on your phone).",
                            Icons.Outlined.CheckCircle to "Plan tomorrow's groceries calmly each evening before hunger or rush decides for you."
                        )
                    )

                    1 -> OnboardingPageContent(
                        badge = "02 • PRIVACY BY ARCHITECTURE",
                        headline = "100% local on your phone. Zero internet permission.",
                        description = "${BrandConfig.APP_NAME} does not ask you to create an account, contains no analytics SDKs, and does not even request Android's INTERNET permission.",
                        highlights = listOf(
                            Icons.Outlined.Lock to "Your selected apps, grocery lists, and spending notes never leave your device.",
                            Icons.Outlined.VisibilityOff to "Never reads screen content, messages, passwords, or banking data."
                        )
                    )

                    else -> OnboardingPageContent(
                        badge = "03 • YOUR AUTONOMY FIRST",
                        headline = "Permissions are requested only when you activate a feature.",
                        description = "You remain in full control of your device at all times. You can unlock any app whenever you consciously choose, pause restrictions temporarily, or export and erase your local data.",
                        highlights = listOf(
                            Icons.Outlined.Shield to "Accessibility access is optional and explained clearly before you ever enable it.",
                            Icons.Outlined.NotificationsNone to "Evening reminders respect quiet sleep hours and daily limits."
                        )
                    )
                }
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 540.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (step < 2) step++ else onFinishOnboarding()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("onboarding_next_button"),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = if (step < 2) "Continue" else "Enter ${BrandConfig.APP_NAME}",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (step > 0) {
                    OutlinedButton(
                        onClick = { step-- },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, saviorColors.borderSubtle)
                    ) {
                        Text("Back")
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    badge: String,
    headline: String,
    description: String,
    highlights: List<Pair<ImageVector, String>>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = badge,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = headline,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        highlights.forEach { (icon, text) ->
            CalmCard(containerColor = MaterialTheme.colorScheme.surface) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Mandatory Google Play-compliant Prominent Disclosure dialog displayed inside the app
 * BEFORE redirecting the user to Android's Accessibility Settings.
 */
@Composable
fun AccessibilityDisclosureDialog(
    onConfirmAndOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Why ${BrandConfig.APP_NAME} Uses Accessibility Service",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "To display a calm reflection screen when you open an application you chose to restrict, ${BrandConfig.APP_NAME} uses Android's AccessibilityService API.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "What is inspected:\n• Only the foreground package name (event.packageName) when a window opens, checked locally against the apps you explicitly selected.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "What is NEVER accessed:\n• ${BrandConfig.APP_NAME} sets canRetrieveWindowContent=\"false\". It never reads screen text, passwords, messages, notifications, or keystrokes, and never transmits any data off your device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Honest limitation:\n• You remain in full control. You can unlock any app or turn off this service in Android Settings at any time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmAndOpenSettings,
                modifier = Modifier.testTag("disclosure_agree_button")
            ) {
                Text("I Understand • Open Settings")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("disclosure_cancel_button")
            ) {
                Text("Not Now")
            }
        }
    )
}
