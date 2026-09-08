package com.shannon.cypher.weather.alerts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CypherWeatherAlertReceiver :
    BroadcastReceiver() {

    companion object {
        private const val TAG = "CypherWeatherAlerts"
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        Log.i(
            TAG,
            "Receiver: weather alert alarm fired. action=${intent.action ?: "none"}"
        )

        val pendingResult = goAsync()

        CoroutineScope(
            Dispatchers.IO
        ).launch {
            try {
                Log.i(
                    TAG,
                    "Receiver: starting BOM warning check."
                )

                CypherWeatherAlertService(
                    context.applicationContext
                ).checkForAlerts()

                Log.i(
                    TAG,
                    "Receiver: BOM warning check finished."
                )
            } catch (
                exception: Exception
            ) {
                Log.e(
                    TAG,
                    "Receiver: weather alert check failed.",
                    exception,
                )
            } finally {
                pendingResult.finish()

                Log.i(
                    TAG,
                    "Receiver: background work finished."
                )
            }
        }
    }
}
