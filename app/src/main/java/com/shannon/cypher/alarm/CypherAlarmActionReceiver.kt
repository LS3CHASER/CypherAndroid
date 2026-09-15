package com.shannon.cypher.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class CypherAlarmActionReceiver :
    BroadcastReceiver() {

    companion object {
        const val ACTION_SNOOZE =
            "com.shannon.cypher.alarm.SNOOZE"

        const val ACTION_DISMISS =
            "com.shannon.cypher.alarm.DISMISS"

        const val SNOOZE_MILLIS =
            10L *
                    60L *
                    1000L
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val alarmId =
            intent.getLongExtra(
                CypherAlarmRingingService
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

        stopRinging(
            appContext
        )

        when (
            intent.action
        ) {
            ACTION_SNOOZE -> {
                CypherAlarmScheduler(
                    appContext
                ).scheduleAlarmAt(
                    alarmId =
                        alarmId,
                    triggerAtMillis =
                        System.currentTimeMillis() +
                                SNOOZE_MILLIS,
                )
            }

            ACTION_DISMISS -> {
                /*
                 * The original receiver already disabled a one-off
                 * alarm or scheduled the next repeating occurrence.
                 * Dismiss therefore only stops the current ringing.
                 */
            }
        }

        CypherAlarmRingingActivity
            .finishActiveInstance()
    }

    private fun stopRinging(
        context: Context,
    ) {
        context.stopService(
            Intent(
                context,
                CypherAlarmRingingService::class.java,
            )
        )
    }
}
