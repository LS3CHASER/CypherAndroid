package com.shannon.cypher.weather.alerts

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log

class CypherWeatherAlertScheduler(
    context: Context,
) {
    companion object {
        private const val TAG = "CypherWeatherAlerts"
        private const val REQUEST_CODE = 47031
        private const val FIRST_CHECK_DELAY_MS = 60_000L
        private const val CHECK_INTERVAL_MS = 15 * 60 * 1000L
    }

    private val appContext = context.applicationContext

    private val alarmManager =
        appContext.getSystemService(
            Context.ALARM_SERVICE
        ) as AlarmManager

    fun schedule() {
        val pendingIntent = createPendingIntent()

        val firstTrigger =
            SystemClock.elapsedRealtime() +
                    FIRST_CHECK_DELAY_MS

        Log.i(
            TAG,
            "Scheduler: scheduling weather alert checks. " +
                    "First check in 1 minute; requested interval = 15 minutes."
        )

        alarmManager.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            firstTrigger,
            CHECK_INTERVAL_MS,
            pendingIntent,
        )

        Log.i(
            TAG,
            "Scheduler: weather alert alarm registered successfully."
        )
    }

    fun cancel() {
        alarmManager.cancel(
            createPendingIntent()
        )

        Log.i(
            TAG,
            "Scheduler: weather alert alarm cancelled."
        )
    }

    private fun createPendingIntent(): PendingIntent {
        val intent =
            Intent(
                appContext,
                CypherWeatherAlertReceiver::class.java,
            )

        return PendingIntent.getBroadcast(
            appContext,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
