package com.example.domain

import com.example.data.local.GroceryItemEntity
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class GrocerySuggestion(
    val name: String,
    val category: String,
    val defaultUnit: String,
    val defaultQuantity: Double,
    val suggestedFrequencyDays: Int,
    val explanation: String,
    val priority: Int
)

/**
 * Simple, 100% offline, explainable suggestion rules for evening grocery planning.
 * Never claims to magically know kitchen stock—only relies on user-marked items,
 * explicitly configured replenishment intervals, purchase frequency, and seasonal context.
 */
object GrocerySuggestionEngine {

    private val indianPantryEssentials = listOf(
        EssentialTemplate("Milk", "Dairy & Eggs", "L", 1.0, 2),
        EssentialTemplate("Eggs", "Dairy & Eggs", "pcs", 12.0, 7),
        EssentialTemplate("Bread", "Bakery", "pack", 1.0, 4),
        EssentialTemplate("Seasonal Vegetables", "Produce", "kg", 1.0, 3),
        EssentialTemplate("Fresh Fruits", "Produce", "kg", 1.0, 4),
        EssentialTemplate("Cooking Oil", "Pantry", "L", 1.0, 30),
        EssentialTemplate("Rice", "Pantry", "kg", 2.0, 21),
        EssentialTemplate("Atta (Whole Wheat)", "Pantry", "kg", 5.0, 21),
        EssentialTemplate("Dal / Lentils", "Pantry", "kg", 1.0, 21),
        EssentialTemplate("Dishwash & Cleaning", "Household", "bottle", 1.0, 30),
        EssentialTemplate("Personal Care Essentials", "Personal Care", "pcs", 1.0, 30)
    )

    private data class EssentialTemplate(
        val name: String,
        val category: String,
        val unit: String,
        val qty: Double,
        val frequencyDays: Int
    )

    fun isDueForReplenishment(
        item: GroceryItemEntity,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (item.isAvailableAtHome) return false
        if (item.isRunningLow) return true
        if (item.frequencyDays <= 0 || item.lastPurchasedAt <= 0L) return false
        val elapsedDays = TimeUnit.MILLISECONDS.toDays(
            (nowEpochMs - item.lastPurchasedAt).coerceAtLeast(0L)
        )
        return elapsedDays >= item.frequencyDays
    }

    fun generateSuggestions(
        existingItems: List<GroceryItemEntity>,
        nowEpochMs: Long = System.currentTimeMillis()
    ): List<GrocerySuggestion> {
        val suggestions = mutableListOf<GrocerySuggestion>()
        val activeUncheckedNames = existingItems
            .filter { !it.isChecked && !it.isAvailableAtHome }
            .map { it.name.trim().lowercase() }
            .toSet()

        val atHomeNames = existingItems
            .filter { it.isAvailableAtHome }
            .map { it.name.trim().lowercase() }
            .toSet()

        // 1. User-marked running-low or replenishment interval due items
        existingItems.forEach { item ->
            val key = item.name.trim().lowercase()
            if (item.isAvailableAtHome) return@forEach
            if (item.isRunningLow && item.isChecked) {
                suggestions.add(
                    GrocerySuggestion(
                        name = item.name,
                        category = item.category,
                        defaultUnit = item.unit,
                        defaultQuantity = item.quantity,
                        suggestedFrequencyDays = item.frequencyDays,
                        explanation = "You marked this as running low.",
                        priority = 1
                    )
                )
            } else if (item.frequencyDays > 0 && item.lastPurchasedAt > 0L) {
                val daysAgo = TimeUnit.MILLISECONDS.toDays(
                    (nowEpochMs - item.lastPurchasedAt).coerceAtLeast(0L)
                )
                if (daysAgo >= item.frequencyDays && key !in activeUncheckedNames) {
                    suggestions.add(
                        GrocerySuggestion(
                            name = item.name,
                            category = item.category,
                            defaultUnit = item.unit,
                            defaultQuantity = item.quantity,
                            suggestedFrequencyDays = item.frequencyDays,
                            explanation = "Last purchased $daysAgo days ago (your ${item.frequencyDays}-day cycle).",
                            priority = 2
                        )
                    )
                }
            }
        }

        // 2. Frequently purchased or saved favourite items not currently on active list
        existingItems
            .filter { (it.isFavourite || it.purchaseCount >= 2) && it.isChecked && !it.isAvailableAtHome }
            .forEach { item ->
                val key = item.name.trim().lowercase()
                if (key !in activeUncheckedNames && suggestions.none { it.name.equals(item.name, ignoreCase = true) }) {
                    val reason = if (item.purchaseCount >= 2) {
                        "Frequently purchased (${item.purchaseCount} times)."
                    } else {
                        "Saved in your household favourites."
                    }
                    suggestions.add(
                        GrocerySuggestion(
                            name = item.name,
                            category = item.category,
                            defaultUnit = item.unit,
                            defaultQuantity = item.quantity,
                            suggestedFrequencyDays = item.frequencyDays,
                            explanation = reason,
                            priority = 3
                        )
                    )
                }
            }

        // 3. Seasonal contextual suggestion (gentle, offline calendar rule)
        val cal = Calendar.getInstance().apply { timeInMillis = nowEpochMs }
        val month = cal.get(Calendar.MONTH) // 0..11
        val seasonal = when (month) {
            in Calendar.MARCH..Calendar.MAY -> EssentialTemplate("Curd / Buttermilk & Hydration", "Dairy & Eggs", "pack", 1.0, 3)
            in Calendar.JUNE..Calendar.SEPTEMBER -> EssentialTemplate("Ginger, Tea & Monsoon Staples", "Produce", "pack", 1.0, 7)
            else -> EssentialTemplate("Warm Soups & Winter Greens", "Produce", "kg", 0.5, 5)
        }
        val seasonalKey = seasonal.name.lowercase()
        if (seasonalKey !in activeUncheckedNames && seasonalKey !in atHomeNames &&
            suggestions.none { it.name.equals(seasonal.name, ignoreCase = true) }
        ) {
            suggestions.add(
                GrocerySuggestion(
                    name = seasonal.name,
                    category = seasonal.category,
                    defaultUnit = seasonal.unit,
                    defaultQuantity = seasonal.qty,
                    suggestedFrequencyDays = seasonal.frequencyDays,
                    explanation = "Optional seasonal suggestion—add only if needed.",
                    priority = 4
                )
            )
        }

        // 4. Starter essentials not already in active list or marked available at home
        indianPantryEssentials.forEach { essential ->
            val key = essential.name.lowercase()
            val alreadyKnown = existingItems.any { it.name.trim().lowercase() == key }
            if (!alreadyKnown && key !in activeUncheckedNames && key !in atHomeNames) {
                suggestions.add(
                    GrocerySuggestion(
                        name = essential.name,
                        category = essential.category,
                        defaultUnit = essential.unit,
                        defaultQuantity = essential.qty,
                        suggestedFrequencyDays = essential.frequencyDays,
                        explanation = "Everyday staple—check your kitchen first.",
                        priority = 5
                    )
                )
            }
        }

        return suggestions.sortedBy { it.priority }.take(10)
    }
}
