package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerDao {
    @Query("SELECT * FROM prayer_settings")
    fun getPrayerSettingsFlow(): Flow<List<PrayerSettings>>

    @Query("SELECT * FROM prayer_settings")
    suspend fun getPrayerSettingsList(): List<PrayerSettings>

    @Query("SELECT * FROM prayer_settings WHERE prayerId = :id")
    suspend fun getPrayerSettings(id: String): PrayerSettings?

    @Query("SELECT * FROM app_config WHERE id = 1")
    fun getAppConfigFlow(): Flow<AppConfig?>

    @Query("SELECT * FROM app_config WHERE id = 1")
    suspend fun getAppConfig(): AppConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayerSettings(settings: List<PrayerSettings>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updatePrayerSettings(settings: PrayerSettings)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateAppConfig(config: AppConfig)

    @Transaction
    suspend fun populateDefaultsIfNeeded() {
        val count = getPrayerSettingsList().size
        if (count == 0) {
            val defaults = listOf(
                PrayerSettings("fajr", "Fajr", isEnabled = true, minutesBefore = 60, customMessage = "Fajr is approaching in %d mins. Wake up and prepare!"),
                PrayerSettings("dhuhr", "Dhuhr", isEnabled = true, minutesBefore = 10, customMessage = "Dhuhr is approaching in %d mins. Put away work!"),
                PrayerSettings("asr", "Asr", isEnabled = true, minutesBefore = 10, customMessage = "Asr is approaching in %d mins. Set everything aside!"),
                PrayerSettings("maghrib", "Maghrib", isEnabled = true, minutesBefore = 10, customMessage = "Maghrib is approaching in %d mins. Turn to prayer!"),
                PrayerSettings("isha", "Isha", isEnabled = true, minutesBefore = 10, customMessage = "Isha is approaching in %d mins. Prepare for the night prayer!")
            )
            insertPrayerSettings(defaults)
        }
        if (getAppConfig() == null) {
            updateAppConfig(AppConfig())
        }
    }
}
