package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prayer_settings")
data class PrayerSettings(
    @PrimaryKey val prayerId: String, // "fajr", "dhuhr", "asr", "maghrib", "isha"
    val displayName: String,
    val isEnabled: Boolean = true,
    val minutesBefore: Int = 10, // Default is 10 min, Fajr will be pre-loaded as 60 min
    val customMessage: String = "Time to prepare for prayer!"
)

@Entity(tableName = "app_config")
data class AppConfig(
    @PrimaryKey val id: Int = 1, // Singleton row
    val latitude: Double = 21.4225, // default: Makkah
    val longitude: Double = 39.8262,
    val cityName: String = "Makkah",
    val calcMethodIndex: Int = 3, // default: Umm Al-Qura calculation method
    val asrMethod: Int = 1, // 1 for Shafi'i/Standard, 2 for Hanafi
    val useSystemLocation: Boolean = false
)
