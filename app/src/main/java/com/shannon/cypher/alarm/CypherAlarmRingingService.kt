package com.shannon.cypher.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.shannon.cypher.R
import com.shannon.cypher.notifications.CypherNotificationManager

class CypherAlarmRingingService :
    Service() {

    companion object {
        const val ACTION_START =
            "com.shannon.cypher.alarm.START_RINGING"

        const val ACTION_STOP =
            "com.shannon.cypher.alarm.STOP_RINGING"

        const val EXTRA_ALARM_ID =
            "cypher_ringing_alarm_id"

        const val NOTIFICATION_ID =
            0x43595048

        private const val REQUEST_OPEN =
            0x4301

        private const val REQUEST_SNOOZE =
            0x4302

        private const val REQUEST_DISMISS =
            0x4303
    }

    private var ringtone: Ringtone? =
        null

    private var vibrator: Vibrator? =
        null

    private var activeAlarmId: Long =
        -1L

    override fun onCreate() {
        super.onCreate()

        CypherNotificationManager(
            applicationContext
        ).createNotificationChannels()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {

        if (
            intent?.action ==
            ACTION_STOP
        ) {
            stopRinging()
            stopSelf()

            return START_NOT_STICKY
        }

        val alarmId =
            intent?.getLongExtra(
                EXTRA_ALARM_ID,
                -1L,
            ) ?: -1L

        if (
            alarmId <=
            0L
        ) {
            stopSelf()
            return START_NOT_STICKY
        }

        val alarm =
            CypherAlarmRepository(
                applicationContext
            ).getAlarm(
                alarmId
            )

        if (
            alarm ==
            null
        ) {
            stopSelf()
            return START_NOT_STICKY
        }

        activeAlarmId =
            alarmId

        startForeground(
            NOTIFICATION_ID,
            buildRingingNotification(
                alarm
            ),
        )

        startAlarmSound()
        startVibration()

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopRinging()
        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?,
    ): IBinder? =
        null

    private fun buildRingingNotification(
        alarm: CypherAlarm,
    ): Notification {

        val fullScreenIntent =
            Intent(
                this,
                CypherAlarmRingingActivity::class.java,
            ).apply {
                putExtra(
                    EXTRA_ALARM_ID,
                    alarm.id,
                )

                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

        val fullScreenPendingIntent =
            PendingIntent.getActivity(
                this,
                REQUEST_OPEN,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE,
            )

        val snoozePendingIntent =
            PendingIntent.getBroadcast(
                this,
                REQUEST_SNOOZE,
                Intent(
                    this,
                    CypherAlarmActionReceiver::class.java,
                ).apply {
                    action =
                        CypherAlarmActionReceiver
                            .ACTION_SNOOZE

                    putExtra(
                        EXTRA_ALARM_ID,
                        alarm.id,
                    )
                },
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE,
            )

        val dismissPendingIntent =
            PendingIntent.getBroadcast(
                this,
                REQUEST_DISMISS,
                Intent(
                    this,
                    CypherAlarmActionReceiver::class.java,
                ).apply {
                    action =
                        CypherAlarmActionReceiver
                            .ACTION_DISMISS

                    putExtra(
                        EXTRA_ALARM_ID,
                        alarm.id,
                    )
                },
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE,
            )

        return NotificationCompat
            .Builder(
                this,
                CypherNotificationManager
                    .CHANNEL_ALARMS,
            )
            .setSmallIcon(
                R.mipmap.ic_launcher
            )
            .setContentTitle(
                alarm.label
                    .ifBlank {
                        "Cypher Alarm"
                    }
            )
            .setContentText(
                "Alarm is ringing."
            )
            .setCategory(
                NotificationCompat
                    .CATEGORY_ALARM
            )
            .setPriority(
                NotificationCompat
                    .PRIORITY_MAX
            )
            .setVisibility(
                NotificationCompat
                    .VISIBILITY_PUBLIC
            )
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(
                fullScreenPendingIntent
            )
            .setFullScreenIntent(
                fullScreenPendingIntent,
                true,
            )
            .addAction(
                0,
                "SNOOZE",
                snoozePendingIntent,
            )
            .addAction(
                0,
                "DISMISS",
                dismissPendingIntent,
            )
            .build()
    }

    private fun startAlarmSound() {
        if (
            ringtone?.isPlaying ==
            true
        ) {
            return
        }

        val alarmUri =
            RingtoneManager
                .getDefaultUri(
                    RingtoneManager
                        .TYPE_ALARM
                )
                ?: RingtoneManager
                    .getDefaultUri(
                        RingtoneManager
                            .TYPE_NOTIFICATION
                    )

        val nextRingtone =
            RingtoneManager
                .getRingtone(
                    applicationContext,
                    alarmUri,
                )
                ?: return

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.P
        ) {
            nextRingtone.isLooping =
                true
        }

        nextRingtone.audioAttributes =
            AudioAttributes
                .Builder()
                .setUsage(
                    AudioAttributes
                        .USAGE_ALARM
                )
                .setContentType(
                    AudioAttributes
                        .CONTENT_TYPE_SONIFICATION
                )
                .build()

        runCatching {
            nextRingtone.play()
        }

        ringtone =
            nextRingtone
    }

    private fun startVibration() {
        val nextVibrator =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S
            ) {
                getSystemService(
                    VibratorManager::class.java
                )
                    ?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(
                    Context.VIBRATOR_SERVICE
                ) as? Vibrator
            }

        vibrator =
            nextVibrator

        val pattern =
            longArrayOf(
                0L,
                700L,
                350L,
                700L,
                350L,
            )

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {
            nextVibrator?.vibrate(
                VibrationEffect
                    .createWaveform(
                        pattern,
                        0,
                    )
            )
        } else {
            @Suppress("DEPRECATION")
            nextVibrator?.vibrate(
                pattern,
                0,
            )
        }
    }

    private fun stopRinging() {
        runCatching {
            ringtone?.stop()
        }

        ringtone =
            null

        runCatching {
            vibrator?.cancel()
        }

        vibrator =
            null

        stopForeground(
            STOP_FOREGROUND_REMOVE
        )
    }
}
