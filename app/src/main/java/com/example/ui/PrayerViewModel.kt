package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.AlarmScheduler
import com.example.data.AppConfig
import com.example.data.AppDatabase
import com.example.data.PrayerRepository
import com.example.data.PrayerSettings
import com.example.util.PrayerTimeCalculator
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PrayerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = PrayerRepository(db.prayerDao())
    private val alarmScheduler = AlarmScheduler(application)

    val prayerSettingsList: StateFlow<List<PrayerSettings>> = repository.prayerSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val appConfig: StateFlow<AppConfig?> = repository.appConfigFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _currentDate = MutableStateFlow(Calendar.getInstance())
    val currentDate: StateFlow<Calendar> = _currentDate.asStateFlow()

    init {
        viewModelScope.launch {
            repository.populateDefaults()
            val settings = repository.getPrayerSettingsList()
            val config = repository.getAppConfig() ?: AppConfig()
            alarmScheduler.scheduleAlarms(settings, config)
        }
    }

    val todayPrayers: StateFlow<Map<String, Calendar>> = combine(currentDate, appConfig) { date, config ->
        if (config == null) return@combine emptyMap()
        
        val systemTimezoneOffset = TimeZone.getDefault().getOffset(date.timeInMillis) / 3600000.0
        
        PrayerTimeCalculator.calculatePrayerTimes(
            calendar = date,
            latitude = config.latitude,
            longitude = config.longitude,
            timezoneOffsetHours = systemTimezoneOffset,
            methodIndex = config.calcMethodIndex,
            asrMethod = config.asrMethod
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    fun updatePrayerSettings(settings: PrayerSettings) {
        viewModelScope.launch {
            repository.updatePrayerSettings(settings)
            val currentList = repository.getPrayerSettingsList()
            val currentConfig = repository.getAppConfig() ?: AppConfig()
            alarmScheduler.scheduleAlarms(currentList, currentConfig)
        }
    }

    fun updateAppConfig(config: AppConfig) {
        viewModelScope.launch {
            repository.updateAppConfig(config)
            val currentList = repository.getPrayerSettingsList()
            alarmScheduler.scheduleAlarms(currentList, config)
        }
    }

    fun triggerTestReminder(prayerName: String) {
        alarmScheduler.scheduleTestAlarm(prayerName, 3)
    }

    fun resyncAlarms() {
        viewModelScope.launch {
            val currentList = repository.getPrayerSettingsList()
            val currentConfig = repository.getAppConfig() ?: AppConfig()
            alarmScheduler.scheduleAlarms(currentList, currentConfig)
        }
    }

    fun updateDayOfYearOffset(days: Int) {
        val newCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, days)
        }
        _currentDate.value = newCal
    }
}
