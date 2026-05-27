package com.example.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.PrayerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    private val alarmScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("AlarmReceiver", "Received intent action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.QUICKBOOT_POWERON") {
            // Reschedule alarms after device reboot
            val pendingResult = goAsync()
            alarmScope.launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val r = PrayerRepository(db.prayerDao())
                    r.populateDefaults()
                    val settings = r.getPrayerSettingsList()
                    val config = r.getAppConfig()
                    if (config != null) {
                        AlarmScheduler(context).scheduleAlarms(settings, config)
                        Log.d("AlarmReceiver", "Successfully rescheduled alarms on boot")
                    }
                } catch (e: Exception) {
                    Log.e("AlarmReceiver", "Error rescheduling on boot: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        // Standard notification trigger
        val prayerName = intent.getStringExtra("EXTRA_PRAYER_NAME") ?: "Prayer"
        val minutesBefore = intent.getIntExtra("EXTRA_MINUTES_BEFORE", 10)
        val customMessageTemplate = intent.getStringExtra("EXTRA_CUSTOM_MESSAGE") ?: "%s is approaching in %d minutes."
        val alarmId = intent.getIntExtra("EXTRA_ALARM_ID", 1001)

        val descriptionStr = String.format(customMessageTemplate, prayerName, minutesBefore)

        showNotification(context, alarmId, "${prayerName} Preparation Alert", descriptionStr)
        
        // Also self-trigger reschedule of subsequent days when a periodic alarm goes off
        val pendingResult = goAsync()
        alarmScope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val r = PrayerRepository(db.prayerDao())
                val settings = r.getPrayerSettingsList()
                val config = r.getAppConfig()
                if (config != null) {
                    AlarmScheduler(context).scheduleAlarms(settings, config)
                }
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "Error updating alarms: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, notificationId: Int, title: String, content: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "prayer_reminders_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Prayer Warnings",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Sends warning reminders minutes before Islamic prayer times"
                enableLights(true)
                enableVibration(true)
                val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                setSound(alarmSound, AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingOpenApp = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Using standard alarm bell system icon
        val smallIcon = android.R.drawable.ic_lock_idle_alarm

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingOpenApp)

        notificationManager.notify(notificationId, builder.build())
        Log.d("AlarmReceiver", "Notification dispatched for alarm id: $notificationId message: $content")
    }
}
