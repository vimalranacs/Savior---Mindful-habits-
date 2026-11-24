package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        RestrictedAppEntity::class,
        BlockingScheduleEntity::class,
        PauseEventEntity::class,
        GroceryListEntity::class,
        GroceryItemEntity::class,
        EveningPlanNoteEntity::class,
        SpendingEntryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SaviorDatabase : RoomDatabase() {
    abstract fun restrictedAppDao(): RestrictedAppDao
    abstract fun blockingScheduleDao(): BlockingScheduleDao
    abstract fun pauseEventDao(): PauseEventDao
    abstract fun groceryDao(): GroceryDao
    abstract fun spendingDao(): SpendingDao

    suspend fun ensureInitialDefaults() {
        if (blockingScheduleDao().getAllOnce().isEmpty()) {
            blockingScheduleDao().insert(
                BlockingScheduleEntity(
                    label = "Always Mindful",
                    mode = ScheduleMode.ALWAYS,
                    startMinuteOfDay = 0,
                    endMinuteOfDay = 1439,
                    daysMask = 127,
                    isEnabled = true
                )
            )
        }
        if (groceryDao().getListsOnce().isEmpty()) {
            groceryDao().insertList(
                GroceryListEntity(
                    name = "Tomorrow's Essentials",
                    isDefault = true
                )
            )
            groceryDao().insertList(
                GroceryListEntity(
                    name = "Weekly Groceries",
                    isDefault = false
                )
            )
            groceryDao().insertList(
                GroceryListEntity(
                    name = "Household & Care",
                    isDefault = false
                )
            )
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: SaviorDatabase? = null

        fun getInstance(context: Context): SaviorDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SaviorDatabase::class.java,
                    "savior_local.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
