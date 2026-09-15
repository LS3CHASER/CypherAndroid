package com.shannon.cypher.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

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

        if (
            timerId <= 0L
        ) {
            return
        }

        val appContext =
            context.applicationContext

        val repository =
            CypherAlarmRepository(
                appContext
            )

        val timer =
            repository.getTimer(
                timerId
            ) ?: return

        if (
            !timer.active
        ) {
            return
        }

        /*
         * Mark it finished immediately so persistence and the
         * Alarms & Timers screen remain correct even while the
         * ringing UI is active.
         */
        repository.markTimerInactive(
            timer.id
        )

        val serviceIntent =
            Intent(
                appContext,
                CypherTimerRingingService::class.java,
            ).apply {
                action =
                    CypherTimerRingingService.ACTION_START

                putExtra(
                    CypherTimerRingingService.EXTRA_TIMER_ID,
                    timer.id,
                )
            }

        ContextCompat.startForegroundService(
            appContext,
            serviceIntent,
        )
    }
}
