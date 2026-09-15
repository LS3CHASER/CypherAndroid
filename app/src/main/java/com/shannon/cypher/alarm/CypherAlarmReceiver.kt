package com.shannon.cypher.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class CypherAlarmReceiver :
    BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val alarmId =
            intent.getLongExtra(
                CypherAlarmScheduler
                    .EXTRA_ALARM_ID,
                -1L,
            )

        if (
            alarmId <=
            0L
        ) {
            return
        }

        val appContext =
            context.applicationContext

        val repository =
            CypherAlarmRepository(
                appContext
            )

        val alarm =
            repository.getAlarm(
                alarmId
            ) ?: return

        if (
            !alarm.enabled
        ) {
            return
        }

        val serviceIntent =
            Intent(
                appContext,
                CypherAlarmRingingService::class.java,
            ).apply {
                action =
                    CypherAlarmRingingService
                        .ACTION_START

                putExtra(
                    CypherAlarmRingingService
                        .EXTRA_ALARM_ID,
                    alarm.id,
                )
            }

        ContextCompat
            .startForegroundService(
                appContext,
                serviceIntent,
            )

        if (
            alarm.repeatDays.isEmpty()
        ) {
            repository
                .setAlarmEnabled(
                    alarmId =
                        alarm.id,
                    enabled =
                        false,
                )
        } else {
            CypherAlarmScheduler(
                appContext
            ).scheduleAlarm(
                alarm
            )
        }
    }
}
