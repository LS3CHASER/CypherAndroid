package com.shannon.cypher.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.shannon.cypher.notifications.CypherNotificationManager

class CypherTimerReceiver :
    BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val timerId =
            intent.getLongExtra(
                CypherAlarmScheduler.EXTRA_TIMER_ID,
                -1L,
            )

        if (timerId <= 0L) return

        val appContext = context.applicationContext
        val repository = CypherAlarmRepository(appContext)
        val timer = repository.getTimer(timerId) ?: return

        if (!timer.active) return

        repository.markTimerInactive(timer.id)

        CypherNotificationManager(appContext).apply {
            createNotificationChannels()

            showTimerNotification(
                notificationId = timerNotificationId(timer.id),
                title = timer.label.ifBlank { "Cypher Timer" },
                message = "Timer finished.",
            )
        }
    }

    private fun timerNotificationId(timerId: Long): Int {
        return (((timerId xor (timerId ushr 32)).toInt() xor 0x72520000) and 0x7fffffff)
    }
}
