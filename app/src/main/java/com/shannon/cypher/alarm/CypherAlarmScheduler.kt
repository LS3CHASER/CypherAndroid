package com.shannon.cypher.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

class CypherAlarmScheduler(
    context: Context,
) {
    companion object {
        const val EXTRA_ALARM_ID = "cypher_alarm_id"
        const val EXTRA_TIMER_ID = "cypher_timer_id"

        private const val ACTION_ALARM = "com.shannon.cypher.action.CYPHER_ALARM"
        private const val ACTION_TIMER = "com.shannon.cypher.action.CYPHER_TIMER"
    }

    private val appContext = context.applicationContext

    private val alarmManager =
        appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private val repository = CypherAlarmRepository(appContext)

    fun scheduleAlarm(alarm: CypherAlarm): Long? {
        if (!alarm.enabled) {
            cancelAlarm(alarm.id)
            return null
        }

        val triggerAtMillis = calculateNextAlarmTime(alarm)

        scheduleExactWhenAvailable(
            triggerAtMillis = triggerAtMillis,
            operation = alarmPendingIntent(alarm.id),
        )

        return triggerAtMillis
    }

    fun scheduleTimer(timer: CypherTimer) {
        if (!timer.active) {
            cancelTimer(timer.id)
            return
        }

        val triggerAtMillis =
            timer.endsAtMillis.coerceAtLeast(
                System.currentTimeMillis() + 250L
            )

        scheduleExactWhenAvailable(
            triggerAtMillis = triggerAtMillis,
            operation = timerPendingIntent(timer.id),
        )
    }

    fun cancelAlarm(alarmId: Long) {
        alarmManager.cancel(alarmPendingIntent(alarmId))
    }

    fun cancelTimer(timerId: Long) {
        alarmManager.cancel(timerPendingIntent(timerId))
    }

    fun rescheduleEverything() {
        repository.getAlarms()
            .filter { it.enabled }
            .forEach { scheduleAlarm(it) }

        repository.getTimers()
            .filter { it.active }
            .forEach { timer ->
                if (timer.endsAtMillis > System.currentTimeMillis()) {
                    scheduleTimer(timer)
                } else {
                    scheduleExactWhenAvailable(
                        triggerAtMillis = System.currentTimeMillis() + 500L,
                        operation = timerPendingIntent(timer.id),
                    )
                }
            }
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun calculateNextAlarmTime(
        alarm: CypherAlarm,
        nowMillis: Long = System.currentTimeMillis(),
    ): Long {
        if (alarm.repeatDays.isEmpty()) {
            val candidate =
                Calendar.getInstance().apply {
                    timeInMillis = nowMillis
                    set(Calendar.HOUR_OF_DAY, alarm.hour)
                    set(Calendar.MINUTE, alarm.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)

                    if (timeInMillis <= nowMillis) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }

            return candidate.timeInMillis
        }

        for (daysAhead in 0..7) {
            val candidate =
                Calendar.getInstance().apply {
                    timeInMillis = nowMillis
                    add(Calendar.DAY_OF_YEAR, daysAhead)
                    set(Calendar.HOUR_OF_DAY, alarm.hour)
                    set(Calendar.MINUTE, alarm.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

            val candidateDay = candidate.get(Calendar.DAY_OF_WEEK)

            if (
                candidateDay in alarm.repeatDays &&
                candidate.timeInMillis > nowMillis
            ) {
                return candidate.timeInMillis
            }
        }

        return nowMillis + 24L * 60L * 60L * 1000L
    }

    private fun scheduleExactWhenAvailable(
        triggerAtMillis: Long,
        operation: PendingIntent,
    ) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                operation,
            )
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                operation,
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                operation,
            )
        }
    }

    private fun alarmPendingIntent(alarmId: Long): PendingIntent {
        val intent =
            Intent(
                appContext,
                CypherAlarmReceiver::class.java,
            ).apply {
                action = ACTION_ALARM
                putExtra(EXTRA_ALARM_ID, alarmId)
            }

        return PendingIntent.getBroadcast(
            appContext,
            requestCode(alarmId, 0x41000000),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun timerPendingIntent(timerId: Long): PendingIntent {
        val intent =
            Intent(
                appContext,
                CypherTimerReceiver::class.java,
            ).apply {
                action = ACTION_TIMER
                putExtra(EXTRA_TIMER_ID, timerId)
            }

        return PendingIntent.getBroadcast(
            appContext,
            requestCode(timerId, 0x52000000),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun requestCode(
        id: Long,
        salt: Int,
    ): Int {
        return (((id xor (id ushr 32)).toInt() xor salt) and 0x7fffffff)
    }
}
