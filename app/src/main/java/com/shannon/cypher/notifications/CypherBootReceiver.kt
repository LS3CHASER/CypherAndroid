package com.shannon.cypher.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class CypherBootReceiver :
    BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {

        if (
            intent.action !=
            Intent.ACTION_BOOT_COMPLETED
        ) {

            return
        }


        val pendingResult =
            goAsync()


        Thread {

            try {

                CypherCalendarReminderSync(
                    context.applicationContext
                ).syncUpcomingCalendarReminders()

            } finally {

                pendingResult.finish()
            }

        }.start()
    }
}