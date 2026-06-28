package com.deepresearch.app

import android.app.Application
import com.deepresearch.app.data.local.AppDatabase
import com.deepresearch.app.data.local.ReportCacheDao
import com.deepresearch.app.data.local.SettingsDataStore

/**
 * Application class - initializes singletons on app start.
 */
class DeepResearchApp : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var reportCacheDao: ReportCacheDao
        private set
    lateinit var settingsDataStore: SettingsDataStore
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
        reportCacheDao = database.reportCacheDao()
        settingsDataStore = SettingsDataStore(this)
    }

    companion object {
        lateinit var instance: DeepResearchApp
            private set
    }
}
