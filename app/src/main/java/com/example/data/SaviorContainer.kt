package com.example.data

import android.content.Context
import com.example.blocking.InstalledAppsProvider
import com.example.data.local.SaviorDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.privacy.KeystoreCryptoManager
import com.example.privacy.LocalDataExporter

class SaviorContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: SaviorDatabase = SaviorDatabase.getInstance(appContext)
    val preferencesRepository: UserPreferencesRepository = UserPreferencesRepository(appContext)
    val cryptoManager: KeystoreCryptoManager = KeystoreCryptoManager()
    val installedAppsProvider: InstalledAppsProvider = InstalledAppsProvider(appContext)
    val dataExporter: LocalDataExporter = LocalDataExporter(
        database = database,
        preferences = preferencesRepository,
        cryptoManager = cryptoManager
    )
}
