package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppCategory
import com.example.data.local.GroceryItemEntity
import com.example.data.local.RestrictedAppEntity
import com.example.data.local.SaviorDatabase
import com.example.data.local.SpendingEntryEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.privacy.KeystoreCryptoManager
import com.example.privacy.LocalDataExporter
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: SaviorDatabase
    private lateinit var preferences: UserPreferencesRepository
    private lateinit var cryptoManager: KeystoreCryptoManager
    private lateinit var exporter: LocalDataExporter

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, SaviorDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        preferences = UserPreferencesRepository(context)
        cryptoManager = KeystoreCryptoManager()
        exporter = LocalDataExporter(database, preferences, cryptoManager)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `app_name string resource matches Savior`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Savior", appName)
    }

    @Test
    fun `database persists restricted apps, groceries, and spending and exports and deletes cleanly`() = runBlocking {
        database.ensureInitialDefaults()

        // 1. Add restricted app
        database.restrictedAppDao().upsert(
            RestrictedAppEntity(
                packageName = "com.grofers.customerapp",
                displayName = "Blinkit",
                category = AppCategory.QUICK_COMMERCE,
                isEnabled = true
            )
        )
        assertEquals(1, database.restrictedAppDao().getAllOnce().size)

        // 2. Add grocery item to default list
        val defaultList = database.groceryDao().getListsOnce().first()
        database.groceryDao().upsertItem(
            GroceryItemEntity(
                listId = defaultList.id,
                name = "Milk",
                quantity = 2.0,
                unit = "L",
                category = "Dairy & Eggs",
                approxPrice = 65.0,
                isRunningLow = true
            )
        )
        assertEquals(1, database.groceryDao().getAllItemsOnce().size)

        // 3. Add spending record with encrypted personal note
        val encryptedNote = cryptoManager.encrypt("Late evening snack impulse")
        database.spendingDao().insert(
            SpendingEntryEntity(
                amount = 240.0,
                category = "Quick Commerce",
                appName = "Blinkit",
                isImpulsive = true,
                encryptedNote = encryptedNote
            )
        )
        assertEquals(1, database.spendingDao().getAllOnce().size)

        // 4. Verify JSON export contains app name, restricted app, grocery item, and decrypted note
        val exportedJson = exporter.exportToJsonString()
        assertTrue(exportedJson.contains("Savior"))
        assertTrue(exportedJson.contains("Blinkit"))
        assertTrue(exportedJson.contains("Milk"))
        assertTrue(exportedJson.contains("Late evening snack impulse"))

        // 5. Verify Delete All Data clears user records and restores clean default lists/schedule
        exporter.deleteAllLocalData()
        assertTrue(database.restrictedAppDao().getAllOnce().isEmpty())
        assertTrue(database.groceryDao().getAllItemsOnce().isEmpty())
        assertTrue(database.spendingDao().getAllOnce().isEmpty())
        assertTrue(database.blockingScheduleDao().getAllOnce().isNotEmpty())
        assertTrue(database.groceryDao().getListsOnce().isNotEmpty())
    }
}
