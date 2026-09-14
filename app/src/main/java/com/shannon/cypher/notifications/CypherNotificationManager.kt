package com.shannon.cypher.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.shannon.cypher.MainActivity
import com.shannon.cypher.R

class CypherNotificationManager(
    private val context: Context,
) {
    companion object {
        const val CHANNEL_GENERAL = "cypher_general"
        const val CHANNEL_TASKS = "cypher_tasks"
        const val CHANNEL_CALENDAR = "cypher_calendar"
        const val CHANNEL_WEATHER = "cypher_weather"
        const val CHANNEL_ALARMS = "cypher_alarms"
        const val CHANNEL_TIMERS = "cypher_timers"

        const val EXTRA_OPEN_SCREEN = "cypher_open_screen"
        const val EXTRA_TASK_ID = "cypher_notification_task_id"
        const val EXTRA_CALENDAR_EVENT_ID = "cypher_notification_calendar_event_id"

        const val SCREEN_HOME = "home"
        const val SCREEN_TASKS = "tasks"
        const val SCREEN_CALENDAR = "calendar"
        const val SCREEN_WEATHER = "weather"
        const val SCREEN_ALARMS = "alarms"
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        val generalChannel =
            NotificationChannel(
                CHANNEL_GENERAL,
                "Cypher",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "General Cypher notifications"
            }

        val taskChannel =
            NotificationChannel(
                CHANNEL_TASKS,
                "Tasks",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Task reminders and overdue task alerts"
            }

        val calendarChannel =
            NotificationChannel(
                CHANNEL_CALENDAR,
                "Calendar",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Calendar event reminders and alerts"
            }

        val weatherChannel =
            NotificationChannel(
                CHANNEL_WEATHER,
                "Weather Alerts",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Severe weather and important weather alerts"
            }

        val alarmSound =
            RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_ALARM
            )

        val alarmAudioAttributes =
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

        val alarmChannel =
            NotificationChannel(
                CHANNEL_ALARMS,
                "Alarms",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Cypher alarm alerts"
                enableVibration(true)
                setSound(
                    alarmSound,
                    alarmAudioAttributes,
                )
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

        val timerChannel =
            NotificationChannel(
                CHANNEL_TIMERS,
                "Timers",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Cypher timer completion alerts"
                enableVibration(true)
            }

        manager.createNotificationChannels(
            listOf(
                generalChannel,
                taskChannel,
                calendarChannel,
                weatherChannel,
                alarmChannel,
                timerChannel,
            )
        )
    }

    fun showTestNotification() {
        val contentIntent =
            createNavigationPendingIntent(
                notificationId = 1001,
                screen = SCREEN_HOME,
            )

        val notification =
            NotificationCompat.Builder(
                context,
                CHANNEL_GENERAL,
            )
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Cypher")
                .setContentText("Notification system is online.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build()

        postNotification(
            notificationId = 1001,
            notification = notification,
        )
    }

    fun showTaskNotification(
        notificationId: Int,
        title: String,
        message: String,
        taskId: Long = -1L,
    ) {
        val contentIntent =
            createNavigationPendingIntent(
                notificationId = notificationId,
                screen = SCREEN_TASKS,
                taskId = taskId,
            )

        val notification =
            NotificationCompat.Builder(
                context,
                CHANNEL_TASKS,
            )
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build()

        postNotification(notificationId, notification)
    }

    fun showCalendarNotification(
        notificationId: Int,
        title: String,
        message: String,
        calendarEventId: Long = -1L,
    ) {
        val contentIntent =
            createNavigationPendingIntent(
                notificationId = notificationId,
                screen = SCREEN_CALENDAR,
                calendarEventId = calendarEventId,
            )

        val notification =
            NotificationCompat.Builder(
                context,
                CHANNEL_CALENDAR,
            )
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build()

        postNotification(notificationId, notification)
    }

    fun showWeatherNotification(
        notificationId: Int,
        title: String,
        message: String,
        bomWarningUrl: String? = null,
    ) {
        val contentIntent =
            createNavigationPendingIntent(
                notificationId = notificationId,
                screen = SCREEN_WEATHER,
            )

        val builder =
            NotificationCompat.Builder(
                context,
                CHANNEL_WEATHER,
            )
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(message)
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)

        bomWarningUrl
            ?.takeIf { it.isNotBlank() }
            ?.let { url ->
                builder.addAction(
                    0,
                    "VIEW BOM WARNING",
                    createWebPendingIntent(
                        notificationId = notificationId,
                        url = url,
                    ),
                )
            }

        postNotification(
            notificationId = notificationId,
            notification = builder.build(),
        )
    }

    fun showAlarmNotification(
        notificationId: Int,
        title: String,
        message: String,
    ) {
        val contentIntent =
            createNavigationPendingIntent(
                notificationId = notificationId,
                screen = SCREEN_ALARMS,
            )

        val notification =
            NotificationCompat.Builder(
                context,
                CHANNEL_ALARMS,
            )
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build()

        postNotification(notificationId, notification)
    }

    fun showTimerNotification(
        notificationId: Int,
        title: String,
        message: String,
    ) {
        val contentIntent =
            createNavigationPendingIntent(
                notificationId = notificationId,
                screen = SCREEN_ALARMS,
            )

        val notification =
            NotificationCompat.Builder(
                context,
                CHANNEL_TIMERS,
            )
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build()

        postNotification(notificationId, notification)
    }

    private fun createNavigationPendingIntent(
        notificationId: Int,
        screen: String,
        taskId: Long = -1L,
        calendarEventId: Long = -1L,
    ): PendingIntent {
        val intent =
            Intent(
                context,
                MainActivity::class.java,
            ).apply {
                flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP

                putExtra(EXTRA_OPEN_SCREEN, screen)

                if (taskId > 0L) {
                    putExtra(EXTRA_TASK_ID, taskId)
                }

                if (calendarEventId > 0L) {
                    putExtra(
                        EXTRA_CALENDAR_EVENT_ID,
                        calendarEventId,
                    )
                }
            }

        return PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createWebPendingIntent(
        notificationId: Int,
        url: String,
    ): PendingIntent {
        val intent =
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url),
            )

        return PendingIntent.getActivity(
            context,
            notificationId xor 0x5A17,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun postNotification(
        notificationId: Int,
        notification: Notification,
    ) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            val permissionGranted =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED

            if (!permissionGranted) return
        }

        NotificationManagerCompat.from(context)
            .notify(
                notificationId,
                notification,
            )
    }
}
