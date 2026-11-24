package com.example.domain

import com.example.data.local.SpendingEntryEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToLong

data class CategorySpend(
    val category: String,
    val totalAmount: Double,
    val entryCount: Int
)

data class MonthlySpendPoint(
    val monthLabel: String,
    val totalSpent: Double,
    val impulsiveSpent: Double
)

data class SpendingSummary(
    val weeklyTotal: Double,
    val monthlyTotal: Double,
    val monthlyImpulsiveTotal: Double,
    val monthlyNecessaryTotal: Double,
    val estimatedAvoidedTotal: Double,
    val topCategories: List<CategorySpend>,
    val sixMonthTrends: List<MonthlySpendPoint>,
    val neutralInsightSentence: String
)

/**
 * Computes transparent, strictly user-entered financial awareness metrics.
 * Never fabricates savings figures unless directly backed by user-recorded avoided estimates.
 */
object SpendingAnalyticsEngine {

    val spendingCategories = listOf(
        "Quick Commerce",
        "Food Delivery",
        "Groceries",
        "Online Shopping",
        "Snacks & Beverages",
        "Household",
        "Other"
    )

    fun summarize(
        entries: List<SpendingEntryEntity>,
        currencySymbol: String,
        nowEpochMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): SpendingSummary {
        val sevenDaysAgo = nowEpochMs - 7L * 24L * 60L * 60L * 1000L
        val thirtyDaysAgo = nowEpochMs - 30L * 24L * 60L * 60L * 1000L

        val actualPurchases = entries.filter { !it.isAvoidedEstimate }
        val avoidedRecords = entries.filter { it.isAvoidedEstimate }

        val weeklyTotal = actualPurchases
            .filter { it.dateEpochMs >= sevenDaysAgo }
            .sumOf { it.amount }

        val monthlyEntries = actualPurchases.filter { it.dateEpochMs >= thirtyDaysAgo }
        val monthlyTotal = monthlyEntries.sumOf { it.amount }
        val monthlyImpulsive = monthlyEntries.filter { it.isImpulsive }.sumOf { it.amount }
        val monthlyNecessary = monthlyEntries.filter { !it.isImpulsive }.sumOf { it.amount }

        val avoidedTotal = avoidedRecords
            .filter { it.dateEpochMs >= thirtyDaysAgo }
            .sumOf { it.amount }

        val topCategories = monthlyEntries
            .groupBy { it.category }
            .map { (cat, list) ->
                CategorySpend(
                    category = cat,
                    totalAmount = list.sumOf { it.amount },
                    entryCount = list.size
                )
            }
            .sortedByDescending { it.totalAmount }
            .take(5)

        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault()).apply {
            this.timeZone = timeZone
        }
        val sixMonthTrends = (5 downTo 0).map { offset ->
            val cal = Calendar.getInstance(timeZone).apply {
                timeInMillis = nowEpochMs
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.MONTH, -offset)
            }
            val startMs = cal.timeInMillis
            val label = monthFormat.format(cal.time)
            cal.add(Calendar.MONTH, 1)
            val endMs = cal.timeInMillis

            val inMonth = actualPurchases.filter { it.dateEpochMs in startMs until endMs }
            MonthlySpendPoint(
                monthLabel = label,
                totalSpent = inMonth.sumOf { it.amount },
                impulsiveSpent = inMonth.filter { it.isImpulsive }.sumOf { it.amount }
            )
        }

        val insight = when {
            monthlyEntries.isEmpty() && avoidedTotal <= 0.0 ->
                "Record convenience orders optionally whenever you want clearer awareness. Nothing is ever collected automatically."
            monthlyTotal > 0.0 -> {
                val formatted = formatCurrency(monthlyTotal, currencySymbol)
                "Over the past 30 days, you recorded $formatted across ${monthlyEntries.size} convenience or delivery entries. Planning your groceries beforehand may help reduce unplanned orders."
            }
            else ->
                "You recorded ${formatCurrency(avoidedTotal, currencySymbol)} in planned alternatives this month."
        }

        return SpendingSummary(
            weeklyTotal = weeklyTotal,
            monthlyTotal = monthlyTotal,
            monthlyImpulsiveTotal = monthlyImpulsive,
            monthlyNecessaryTotal = monthlyNecessary,
            estimatedAvoidedTotal = avoidedTotal,
            topCategories = topCategories,
            sixMonthTrends = sixMonthTrends,
            neutralInsightSentence = insight
        )
    }

    fun formatCurrency(amount: Double, symbol: String): String {
        val rounded = amount.roundToLong()
        val formatted = String.format(Locale.getDefault(), "%,d", rounded)
        return "$symbol$formatted"
    }
}
