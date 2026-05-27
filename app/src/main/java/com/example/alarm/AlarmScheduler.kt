package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.AppConfig
import com.example.data.PrayerSettings
import com.example.util.PrayerTimeCalculator
import java.util.Calendar
import java.util.TimeZone

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarms(prayerList: List<PrayerSettings>, config: AppConfig) {
        val today = Calendar.getInstance()
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }

        val systemTimezoneHours = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 3600000.0

        // Calculate prayer times
        val todayPrayers = PrayerTimeCalculator.calculatePrayerTimes(
            today, config.latitude, config.longitude, systemTimezoneHours, config.calcMethodIndex, config.asrMethod
        )
        val tomorrowPrayers = PrayerTimeCalculator.calculatePrayerTimes(
            tomorrow, config.latitude, config.longitude, systemTimezoneHours, config.calcMethodIndex, config.asrMethod
        )

        val nowMs = System.currentTimeMillis()

        prayerList.forEach { setting ->
            val prayerId = setting.prayerId
            if (!setting.isEnabled) {
                // Cancel alarms for this prayer
                cancelAlarm(getAlarmId(prayerId, isToday = true))
                cancelAlarm(getAlarmId(prayerId, isToday = false))
                return@forEach
            }

            // 1. Today's Alarm
            val todayPrayerTime = todayPrayers[prayerId]
            if (todayPrayerTime != null) {
                val alarmTime = todayPrayerTime.clone() as Calendar
                alarmTime.add(Calendar.MINUTE, -setting.minutesBefore)

                if (alarmTime.timeInMillis > nowMs) {
                    setAlarm(
                        id = getAlarmId(prayerId, isToday = true),
                        timeMs = alarmTime.timeInMillis,
                        prayerName = setting.displayName,
                        minutesBefore = setting.minutesBefore,
                        customMessage = setting.customMessage
                    )
                } else {
                    cancelAlarm(getAlarmId(prayerId, isToday = true))
                }
            }

            // 2. Tomorrow's Alarm
            val tomorrowPrayerTime = tomorrowPrayers[prayerId]
            if (tomorrowPrayerTime != null) {
                val alarmTime = tomorrowPrayerTime.clone() as Calendar
                alarmTime.add(Calendar.MINUTE, -setting.minutesBefore)

                if (alarmTime.timeInMillis > nowMs) {
                    setAlarm(
                        id = getAlarmId(prayerId, isToday = false),
                        timeMs = alarmTime.timeInMillis,
                        prayerName = setting.displayName,
                        minutesBefore = setting.minutesBefore,
                        customMessage = setting.customMessage
                    )
                }
            }
        }
    }

    private fun setAlarm(id: Int, timeMs: Long, prayerName: String, minutesBefore: Int, customMessage: String) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("EXTRA_PRAYER_NAME", prayerName)
            putExtra("EXTRA_MINUTES_BEFORE", minutesBefore)
            putExtra("EXTRA_CUSTOM_MESSAGE", customMessage)
            putExtra("EXTRA_ALARM_ID", id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMs, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMs, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMs, pendingIntent)
            }
            Log.d("AlarmScheduler", "Status: Scheduled alarm $id for $prayerName in ${timeMs - System.currentTimeMillis()} ms")
        } catch (e: SecurityException) {
            // Graceful fallback for devices restricting exact alarms
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMs, pendingIntent)
            Log.d("AlarmScheduler", "SecurityException fallback: scheduled alarm inexactly")
        }
    }

    fun cancelAlarm(id: Int) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun scheduleTestAlarm(prayerName: String, secondsInFuture: Int) {
        val id = 9999
        val timeMs = System.currentTimeMillis() + (secondsInFuture * 1000)
        setAlarm(
            id = id,
            timeMs = timeMs,
            prayerName = prayerName,
            minutesBefore = 10,
            customMessage = "Remember, %d mins before $prayerName! This is a test reminder."
        )
    }

    private fun getAlarmId(prayerId: String, isToday: Boolean): Int {
        val offset = if (isToday) 1000 else 2000
        val baseId = when (prayerId) {
            "fajr" -> 1
            "dhuhr" -> 2
            "asr" -> 3
            "maghrib" -> 4
            "isha" -> 5
            else -> 6
        }
        return offset + baseId
    }
}
