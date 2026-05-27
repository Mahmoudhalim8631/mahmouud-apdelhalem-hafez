package com.example.data

import kotlinx.coroutines.flow.Flow

class PrayerRepository(private val prayerDao: PrayerDao) {
    val prayerSettingsFlow: Flow<List<PrayerSettings>> = prayerDao.getPrayerSettingsFlow()
    val appConfigFlow: Flow<AppConfig?> = prayerDao.getAppConfigFlow()

    suspend fun getPrayerSettingsList(): List<PrayerSettings> = prayerDao.getPrayerSettingsList()
    suspend fun getPrayerSettings(id: String): PrayerSettings? = prayerDao.getPrayerSettings(id)
    suspend fun getAppConfig(): AppConfig? = prayerDao.getAppConfig()

    suspend fun updatePrayerSettings(settings: PrayerSettings) {
        prayerDao.updatePrayerSettings(settings)
    }

    suspend fun updateAppConfig(config: AppConfig) {
        prayerDao.updateAppConfig(config)
    }

    suspend fun populateDefaults() {
        prayerDao.populateDefaultsIfNeeded()
    }
}
