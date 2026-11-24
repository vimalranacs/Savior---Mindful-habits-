package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

object AppCategory {
    const val QUICK_COMMERCE = "Quick Commerce"
    const val FOOD_DELIVERY = "Food Delivery"
    const val SHOPPING = "Shopping"
    const val ENTERTAINMENT = "Entertainment"
    const val OTHER = "Other"

    val all = listOf(QUICK_COMMERCE, FOOD_DELIVERY, SHOPPING, ENTERTAINMENT, OTHER)
}

object ScheduleMode {
    const val ALWAYS = "ALWAYS"
    const val TIME_WINDOW = "TIME_WINDOW"
    const val WORK_STUDY = "WORK_STUDY"
    const val EVENING_CUTOFF = "EVENING_CUTOFF"
}

object PauseAction {
    const val RETURNED = "RETURNED"
    const val WAITED_MINUTE = "WAITED_MINUTE"
    const val UNLOCKED = "UNLOCKED"
}

@Entity(tableName = "restricted_apps")
data class RestrictedAppEntity(
    @PrimaryKey val packageName: String,
    val displayName: String,
    val category: String = AppCategory.OTHER,
    val isEnabled: Boolean = true,
    val unlockedUntilEpochMs: Long = 0L,
    val lastUnlockReason: String = "",
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "blocking_schedules")
data class BlockingScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val mode: String = ScheduleMode.ALWAYS,
    /** Minutes since midnight [0..1439] */
    val startMinuteOfDay: Int = 0,
    /** Minutes since midnight [0..1439] */
    val endMinuteOfDay: Int = 1439,
    /** Bitmask for days of week: bit 0 = Monday ... bit 6 = Sunday. 0b1111111 = 127 (All days) */
    val daysMask: Int = 127,
    val isEnabled: Boolean = true
)

@Entity(tableName = "pause_events")
data class PauseEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val action: String,
    val unlockMinutes: Int = 0,
    val reason: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "grocery_lists")
data class GroceryListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "grocery_items",
    foreignKeys = [
        ForeignKey(
            entity = GroceryListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("listId")]
)
data class GroceryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val name: String,
    val quantity: Double = 1.0,
    val unit: String = "pcs",
    val category: String = "Pantry",
    val approxPrice: Double = 0.0,
    /** 0 means no recurring replenishment interval */
    val frequencyDays: Int = 0,
    val lastPurchasedAt: Long = 0L,
    val purchaseCount: Int = 0,
    val isChecked: Boolean = false,
    val isFavourite: Boolean = false,
    val isRunningLow: Boolean = false,
    val isAvailableAtHome: Boolean = false,
    val reminderEnabled: Boolean = false
)

@Entity(tableName = "evening_plan_notes")
data class EveningPlanNoteEntity(
    /** Format yyyy-MM-dd */
    @PrimaryKey val dateKey: String,
    val tomorrowMeals: String = "",
    val householdNeeds: String = "",
    val completedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "spending_entries")
data class SpendingEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val dateEpochMs: Long = System.currentTimeMillis(),
    val category: String,
    val appName: String = "",
    val isImpulsive: Boolean = false,
    /** True when the user manually records an impulse order they chose to skip/replace with planning */
    val isAvoidedEstimate: Boolean = false,
    /** Encrypted via Android Keystore AES-GCM when non-blank */
    val encryptedNote: String = ""
)
