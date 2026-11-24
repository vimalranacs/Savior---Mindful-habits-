package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RestrictedAppDao {
    @Query("SELECT * FROM restricted_apps ORDER BY displayName ASC")
    fun observeAll(): Flow<List<RestrictedAppEntity>>

    @Query("SELECT * FROM restricted_apps ORDER BY displayName ASC")
    suspend fun getAllOnce(): List<RestrictedAppEntity>

    @Query("SELECT * FROM restricted_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getByPackage(packageName: String): RestrictedAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(app: RestrictedAppEntity)

    @Query("DELETE FROM restricted_apps WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)

    @Query("UPDATE restricted_apps SET unlockedUntilEpochMs = :untilMs, lastUnlockReason = :reason WHERE packageName = :packageName")
    suspend fun updateUnlock(packageName: String, untilMs: Long, reason: String)

    @Query("UPDATE restricted_apps SET isEnabled = :enabled WHERE packageName = :packageName")
    suspend fun setEnabled(packageName: String, enabled: Boolean)

    @Query("DELETE FROM restricted_apps")
    suspend fun deleteAll()
}

@Dao
interface BlockingScheduleDao {
    @Query("SELECT * FROM blocking_schedules ORDER BY id ASC")
    fun observeAll(): Flow<List<BlockingScheduleEntity>>

    @Query("SELECT * FROM blocking_schedules ORDER BY id ASC")
    suspend fun getAllOnce(): List<BlockingScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: BlockingScheduleEntity): Long

    @Update
    suspend fun update(schedule: BlockingScheduleEntity)

    @Query("DELETE FROM blocking_schedules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM blocking_schedules")
    suspend fun deleteAll()
}

@Dao
interface PauseEventDao {
    @Query("SELECT * FROM pause_events ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<PauseEventEntity>>

    @Query("SELECT * FROM pause_events WHERE timestamp >= :sinceEpochMs ORDER BY timestamp DESC")
    fun observeSince(sinceEpochMs: Long): Flow<List<PauseEventEntity>>

    @Query("SELECT * FROM pause_events ORDER BY timestamp DESC")
    suspend fun getAllOnce(): List<PauseEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: PauseEventEntity): Long

    @Query("DELETE FROM pause_events")
    suspend fun deleteAll()
}

@Dao
interface GroceryDao {
    @Query("SELECT * FROM grocery_lists ORDER BY isDefault DESC, createdAt ASC")
    fun observeLists(): Flow<List<GroceryListEntity>>

    @Query("SELECT * FROM grocery_lists ORDER BY isDefault DESC, createdAt ASC")
    suspend fun getListsOnce(): List<GroceryListEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: GroceryListEntity): Long

    @Query("DELETE FROM grocery_lists WHERE id = :id")
    suspend fun deleteList(id: Long)

    @Query("SELECT * FROM grocery_items ORDER BY isChecked ASC, isRunningLow DESC, isFavourite DESC, name ASC")
    fun observeAllItems(): Flow<List<GroceryItemEntity>>

    @Query("SELECT * FROM grocery_items ORDER BY name ASC")
    suspend fun getAllItemsOnce(): List<GroceryItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(item: GroceryItemEntity): Long

    @Update
    suspend fun updateItem(item: GroceryItemEntity)

    @Query("DELETE FROM grocery_items WHERE id = :id")
    suspend fun deleteItem(id: Long)

    @Query("SELECT * FROM evening_plan_notes ORDER BY completedAt DESC")
    fun observeEveningPlans(): Flow<List<EveningPlanNoteEntity>>

    @Query("SELECT * FROM evening_plan_notes ORDER BY completedAt DESC")
    suspend fun getEveningPlansOnce(): List<EveningPlanNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEveningPlan(note: EveningPlanNoteEntity)

    @Query("DELETE FROM grocery_items")
    suspend fun deleteAllItems()

    @Query("DELETE FROM grocery_lists")
    suspend fun deleteAllLists()

    @Query("DELETE FROM evening_plan_notes")
    suspend fun deleteAllEveningPlans()
}

@Dao
interface SpendingDao {
    @Query("SELECT * FROM spending_entries ORDER BY dateEpochMs DESC, id DESC")
    fun observeAll(): Flow<List<SpendingEntryEntity>>

    @Query("SELECT * FROM spending_entries ORDER BY dateEpochMs DESC, id DESC")
    suspend fun getAllOnce(): List<SpendingEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SpendingEntryEntity): Long

    @Query("DELETE FROM spending_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM spending_entries")
    suspend fun deleteAll()
}
