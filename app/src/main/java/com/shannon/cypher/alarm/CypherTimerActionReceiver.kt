package com.shannon.cypher.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CypherTimerActionReceiver :
    BroadcastReceiver() {

    companion object {
        const val ACTION_DISMISS =
            "com.shannon.cypher.timer.DISMISS"
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (
            intent.action != ACTION_DISMISS
        ) {
            return
        }

        context.applicationContext.stopService(
            Intent(
                context.applicationContext,
                CypherTimerRingingService::class.java,
            )
        )

        CypherTimerRingingActivity
            .finishActiveInstance()
    }
}
