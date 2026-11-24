package com.example.presentation.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.MonthlySpendPoint
import com.example.domain.PeriodHabitSummary
import com.example.domain.SpendingAnalyticsEngine
import com.example.presentation.SaviorUiState
import com.example.presentation.components.CalmCard
import com.example.presentation.components.CalmSectionHeader
import com.example.presentation.components.CalmStatusBadge
import com.example.ui.theme.LocalSaviorColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InsightsScreen(
    uiState: SaviorUiState,
    onAddSpendingEntry: (
        amount: Double,
        category: String,
        appName: String,
        isImpulsive: Boolean,
        isAvoidedEstimate: Boolean,
        note: String
    ) -> Unit,
    onDeleteSpendingEntry: (Long) -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    val currency = uiState.settings.currencySymbol
    val summary = uiState.spendingSummary

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_spending_fab")
                ) {
                    Icon(imageVector = Icons.Outlined.Add, contentDescription = "Log spending or avoided order")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("insights_screen_list"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CalmSectionHeader(
                    title = "Financial & Habit Awareness",
                    subtitle = "Neutral, manual reflections—never automated bank scraping or competitive streaks"
                )
            }

            item {
                val tabs = listOf("Spending Awareness", "Habit Reflections")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    tabs.forEachIndexed { idx, label ->
                        SegmentedButton(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = tabs.size),
                            modifier = Modifier.testTag("insights_tab_$idx")
                        ) {
                            Text(label)
                        }
                    }
                }
            }

            if (selectedTab == 0) {
                // Neutral Insight Card
                item {
                    CalmCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(
                            text = summary.neutralInsightSentence,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "100% manual & local • Personal memos encrypted via Android Keystore",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 4 Key Financial Cards
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SummaryNumberCard(
                                label = "Last 7 Days",
                                amountText = SpendingAnalyticsEngine.formatCurrency(summary.weeklyTotal, currency),
                                detail = "Recorded spend",
                                modifier = Modifier.weight(1f)
                            )
                            SummaryNumberCard(
                                label = "Last 30 Days",
                                amountText = SpendingAnalyticsEngine.formatCurrency(summary.monthlyTotal, currency),
                                detail = "Recorded spend",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SummaryNumberCard(
                                label = "Impulsive (30d)",
                                amountText = SpendingAnalyticsEngine.formatCurrency(summary.monthlyImpulsiveTotal, currency),
                                detail = "Unplanned orders",
                                modifier = Modifier.weight(1f)
                            )
                            SummaryNumberCard(
                                label = "Avoided (30d)",
                                amountText = SpendingAnalyticsEngine.formatCurrency(summary.estimatedAvoidedTotal, currency),
                                detail = "User-recorded planned savings",
                                isPositive = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 6-Month Spending Trend Chart
                item {
                    CalmCard {
                        Text(
                            text = "Monthly Spending Trend (6 Months)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Muted blue shows total recorded spending; deeper slate highlights impulsive portion.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        MonthlyTrendBarChart(
                            points = summary.sixMonthTrends,
                            currencySymbol = currency,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                    }
                }

                // Top Categories
                if (summary.topCategories.isNotEmpty()) {
                    item {
                        CalmCard {
                            Text(
                                text = "Frequently Purchased Categories (30 Days)",
                                style = MaterialTheme.typography.titleMedium
                            )
                            val maxCat = summary.topCategories.maxOf { it.totalAmount }.coerceAtLeast(1.0)
                            summary.topCategories.forEach { cat ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${cat.category} (${cat.entryCount})",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = SpendingAnalyticsEngine.formatCurrency(cat.totalAmount, currency),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { (cat.totalAmount / maxCat).toFloat().coerceIn(0.05f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Recent Manual Entries
                item {
                    CalmSectionHeader(
                        title = "Recorded Entries (${uiState.spendingItems.size})",
                        subtitle = "Tap + to record a convenience purchase or an avoided impulse order"
                    )
                }

                if (uiState.spendingItems.isEmpty()) {
                    item {
                        CalmCard {
                            Text(
                                text = "No entries recorded yet",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Whenever you order from a quick-commerce or food-delivery app—or skip an order because you planned ahead—record it here to build gentle awareness.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    items(uiState.spendingItems, key = { it.entity.id }) { item ->
                        val entry = item.entity
                        CalmCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = SpendingAnalyticsEngine.formatCurrency(entry.amount, currency),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        CalmStatusBadge(
                                            label = when {
                                                entry.isAvoidedEstimate -> "Planned / Avoided"
                                                entry.isImpulsive -> "Impulsive"
                                                else -> "Necessary"
                                            },
                                            isPositive = entry.isAvoidedEstimate || !entry.isImpulsive
                                        )
                                    }
                                    val meta = buildList {
                                        add(entry.category)
                                        if (entry.appName.isNotBlank()) add(entry.appName)
                                        add(dateFormat.format(Date(entry.dateEpochMs)))
                                    }.joinToString(" • ")
                                    Text(
                                        text = meta,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (item.note.isNotBlank()) {
                                        Text(
                                            text = "Note: ${item.note}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                IconButton(onClick = { onDeleteSpendingEntry(entry.id) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = "Delete spending entry"
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Habit Awareness Section (Weekly & Monthly)
                item {
                    HabitPeriodCard(
                        title = "Weekly Summary (Last 7 Days)",
                        summary = uiState.weeklyHabitSummary,
                        currencySymbol = currency
                    )
                }
                item {
                    HabitPeriodCard(
                        title = "Monthly Summary (Last 30 Days)",
                        summary = uiState.monthlyHabitSummary,
                        currencySymbol = currency
                    )
                }
                item {
                    CalmCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(
                            text = "Why there are no streaks here",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Streaks often make people feel like they failed when a busy day happens. Here, every single pause and every planned grocery list stands on its own.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (showAddDialog) {
        AddSpendingEntryDialog(
            currencySymbol = currency,
            onDismiss = { showAddDialog = false },
            onSave = { amount, category, appName, isImpulsive, isAvoided, note ->
                onAddSpendingEntry(amount, category, appName, isImpulsive, isAvoided, note)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun SummaryNumberCard(
    label: String,
    amountText: String,
    detail: String,
    isPositive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val saviorColors = LocalSaviorColors.current
    CalmCard(
        modifier = modifier,
        containerColor = if (isPositive) saviorColors.positiveContainer else MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isPositive) saviorColors.positive else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = amountText,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MonthlyTrendBarChart(
    points: List<MonthlySpendPoint>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val deepColor = LocalSaviorColors.current.deepBlue
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val maxVal = points.maxOfOrNull { it.totalSpent }?.coerceAtLeast(1.0) ?: 1.0

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (points.isEmpty()) return@Canvas
            val count = points.size
            val slotWidth = size.width / count
            val barWidth = (slotWidth * 0.48f).coerceAtMost(72f)

            points.forEachIndexed { index, pt ->
                val xCenter = index * slotWidth + slotWidth / 2f
                val left = xCenter - barWidth / 2f
                // Background track
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(left, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = CornerRadius(10f, 10f)
                )
                if (pt.totalSpent > 0.0) {
                    val totalHeight = ((pt.totalSpent / maxVal).toFloat() * size.height)
                        .coerceIn(8f, size.height)
                    drawRoundRect(
                        color = primaryColor.copy(alpha = 0.72f),
                        topLeft = Offset(left, size.height - totalHeight),
                        size = Size(barWidth, totalHeight),
                        cornerRadius = CornerRadius(10f, 10f)
                    )
                }
                if (pt.impulsiveSpent > 0.0) {
                    val impHeight = ((pt.impulsiveSpent / maxVal).toFloat() * size.height)
                        .coerceIn(6f, size.height)
                    drawRoundRect(
                        color = deepColor,
                        topLeft = Offset(left, size.height - impHeight),
                        size = Size(barWidth, impHeight),
                        cornerRadius = CornerRadius(10f, 10f)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            points.forEach { pt ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = pt.monthLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (pt.totalSpent > 0) {
                        Text(
                            text = SpendingAnalyticsEngine.formatCurrency(pt.totalSpent, currencySymbol),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitPeriodCard(
    title: String,
    summary: PeriodHabitSummary,
    currencySymbol: String
) {
    CalmCard {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = summary.encouragingNote,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(color = LocalSaviorColors.current.borderSubtle)
        HabitMetricRow("Chose to return instead of ordering", summary.timesReturnedInsteadOfOrdering.toString())
        HabitMetricRow("Took a 1-minute breathing reflection", summary.timesWaitedAMinute.toString())
        HabitMetricRow("Planned evening grocery sessions", summary.plannedShoppingSessions.toString())
        HabitMetricRow("Days following chosen restrictions", summary.daysFollowingRestrictions.toString())
        HabitMetricRow("Recorded impulsive orders", summary.recordedImpulsiveOrders.toString())
        HabitMetricRow(
            "User-recorded spending avoided",
            SpendingAnalyticsEngine.formatCurrency(summary.estimatedSpendingAvoided, currencySymbol)
        )
    }
}

@Composable
private fun HabitMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddSpendingEntryDialog(
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (
        amount: Double,
        category: String,
        appName: String,
        isImpulsive: Boolean,
        isAvoidedEstimate: Boolean,
        note: String
    ) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(SpendingAnalyticsEngine.spendingCategories.first()) }
    var appName by remember { mutableStateOf("") }
    var entryMode by remember { mutableIntStateOf(0) } // 0 = Impulsive order, 1 = Necessary order, 2 = Avoided via planning
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Spending or Planned Savings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("spending_amount_input")
                )

                Text("Reflection type", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = entryMode == 0,
                        onClick = { entryMode = 0 },
                        label = { Text("Impulsive order") }
                    )
                    FilterChip(
                        selected = entryMode == 1,
                        onClick = { entryMode = 1 },
                        label = { Text("Necessary order") }
                    )
                    FilterChip(
                        selected = entryMode == 2,
                        onClick = { entryMode = 2 },
                        label = { Text("Avoided via planning") }
                    )
                }

                Text("Category", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SpendingAnalyticsEngine.spendingCategories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = appName,
                    onValueChange = { appName = it },
                    label = { Text("Optional app name (e.g., Blinkit, Swiggy)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Optional personal note (encrypted via Keystore)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val amount = amountText.toDoubleOrNull() ?: 0.0
            Button(
                enabled = amount > 0.0,
                onClick = {
                    onSave(
                        amount,
                        category,
                        appName.trim(),
                        entryMode == 0,
                        entryMode == 2,
                        note.trim()
                    )
                },
                modifier = Modifier.testTag("save_spending_button")
            ) {
                Text("Save Entry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
