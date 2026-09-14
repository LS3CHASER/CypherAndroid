package com.shannon.cypher.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.shannon.cypher.notifications.CypherNotificationManager

class CypherAlarmReceiver :
    BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val alarmId =
            intent.getLongExtra(
                CypherAlarmScheduler.EXTRA_ALARM_ID,
                -1L,
            )

        if (alarmId <= 0L) return

        val appContext = context.applicationContext
        val repository = CypherAlarmRepository(appContext)
        val alarm = repository.getAlarm(alarmId) ?: return

        if (!alarm.enabled) return

        CypherNotificationManager(appContext).apply {
            createNotificationChannels()

            showAlarmNotification(
                notificationId = alarmNotificationId(alarm.id),
                title = alarm.label.ifBlank { "Cypher Alarm" },
                message = "Alarm is ringing.",
            )
        }

        if (alarm.repeatDays.isEmpty()) {
            repository.setAlarmEnabled(
                alarmId = alarm.id,
                enabled = false,
            )
        } else {
            CypherAlarmScheduler(appContext).scheduleAlarm(alarm)
        }
    }

    private fun alarmNotificationId(alarmId: Long): Int {
        return (((alarmId xor (alarmId ushr 32)).toInt() xor 0x61410000) and 0x7fffffff)
    }
}
