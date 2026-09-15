package com.shannon.cypher.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
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

class CypherTimerRingingService :
    Service() {

    companion object {
        const val ACTION_START =
            "com.shannon.cypher.timer.START_RINGING"

        const val EXTRA_TIMER_ID =
            "cypher_ringing_timer_id"

        const val NOTIFICATION_ID =
            0x43595449

        private const val REQUEST_OPEN =
            0x5401

        private const val REQUEST_DISMISS =
            0x5402
    }

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

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

        val timerId =
            intent?.getLongExtra(
                EXTRA_TIMER_ID,
                -1L,
            ) ?: -1L

        if (
            intent?.action != ACTION_START ||
            timerId <= 0L
        ) {
            stopSelf()
            return START_NOT_STICKY
        }

        val timer =
            CypherAlarmRepository(
                applicationContext
            ).getTimer(
                timerId
            )

        if (timer == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(
            NOTIFICATION_ID,
            buildNotification(
                timer
            ),
        )

        startSound()
        startVibration()

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopRinging()
        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?,
    ): IBinder? = null

    private fun buildNotification(
        timer: CypherTimer,
    ): Notification {

        val screenIntent =
            Intent(
                this,
                CypherTimerRingingActivity::class.java,
            ).apply {
                putExtra(
                    EXTRA_TIMER_ID,
                    timer.id,
                )

                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

        val screenPendingIntent =
            PendingIntent.getActivity(
                this,
                REQUEST_OPEN,
                screenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE,
            )

        val dismissPendingIntent =
            PendingIntent.getBroadcast(
                this,
                REQUEST_DISMISS,
                Intent(
                    this,
                    CypherTimerActionReceiver::class.java,
                ).apply {
                    action =
                        CypherTimerActionReceiver
                            .ACTION_DISMISS

                    putExtra(
                        EXTRA_TIMER_ID,
                        timer.id,
                    )
                },
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE,
            )

        return NotificationCompat
            .Builder(
                this,
                CypherNotificationManager
                    .CHANNEL_TIMERS,
            )
            .setSmallIcon(
                R.mipmap.ic_launcher
            )
            .setContentTitle(
                timer.label.ifBlank {
                    "Cypher Timer"
                }
            )
            .setContentText(
                "Timer finished."
            )
            .setCategory(
                NotificationCompat.CATEGORY_ALARM
            )
            .setPriority(
                NotificationCompat.PRIORITY_MAX
            )
            .setVisibility(
                NotificationCompat.VISIBILITY_PUBLIC
            )
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(
                screenPendingIntent
            )
            .setFullScreenIntent(
                screenPendingIntent,
                true,
            )
            .addAction(
                0,
                "DISMISS",
                dismissPendingIntent,
            )
            .build()
    }

    private fun startSound() {
        if (
            ringtone?.isPlaying == true
        ) {
            return
        }

        val uri =
            RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_ALARM
            )
                ?: RingtoneManager.getDefaultUri(
                    RingtoneManager.TYPE_NOTIFICATION
                )

        val next =
            RingtoneManager.getRingtone(
                applicationContext,
                uri,
            ) ?: return

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.P
        ) {
            next.isLooping = true
        }

        next.audioAttributes =
            AudioAttributes.Builder()
                .setUsage(
                    AudioAttributes.USAGE_ALARM
                )
                .setContentType(
                    AudioAttributes.CONTENT_TYPE_SONIFICATION
                )
                .build()

        runCatching {
            next.play()
        }

        ringtone = next
    }

    private fun startVibration() {
        val next =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S
            ) {
                getSystemService(
                    VibratorManager::class.java
                )?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(
                    VIBRATOR_SERVICE
                ) as? Vibrator
            }

        vibrator = next

        val pattern =
            longArrayOf(
                0L,
                600L,
                300L,
                600L,
                300L,
            )

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {
            next?.vibrate(
                VibrationEffect.createWaveform(
                    pattern,
                    0,
                )
            )
        } else {
            @Suppress("DEPRECATION")
            next?.vibrate(
                pattern,
                0,
            )
        }
    }

    private fun stopRinging() {
        runCatching {
            ringtone?.stop()
        }

        runCatching {
            vibrator?.cancel()
        }

        ringtone = null
        vibrator = null

        stopForeground(
            STOP_FOREGROUND_REMOVE
        )
    }
}
