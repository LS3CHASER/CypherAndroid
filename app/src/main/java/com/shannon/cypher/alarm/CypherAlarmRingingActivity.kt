package com.shannon.cypher.alarm

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CypherAlarmRingingActivity :
    Activity() {

    companion object {
        @Volatile
        private var activeInstance:
                CypherAlarmRingingActivity? =
            null

        fun finishActiveInstance() {
            activeInstance
                ?.runOnUiThread {
                    activeInstance
                        ?.finishAndRemoveTask()
                }
        }
    }

    private var alarmId: Long =
        -1L

    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(
            savedInstanceState
        )

        activeInstance =
            this

        showOverLockScreen()

        alarmId =
            intent.getLongExtra(
                CypherAlarmRingingService
                    .EXTRA_ALARM_ID,
                -1L,
            )

        val alarm =
            CypherAlarmRepository(
                applicationContext
            ).getAlarm(
                alarmId
            )

        buildScreen(
            label =
                alarm?.label
                    ?.ifBlank {
                        "Cypher Alarm"
                    }
                    ?: "Cypher Alarm",
        )
    }

    override fun onDestroy() {
        if (
            activeInstance ===
            this
        ) {
            activeInstance =
                null
        }

        super.onDestroy()
    }

    private fun showOverLockScreen() {
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O_MR1
        ) {
            setShowWhenLocked(
                true
            )

            setTurnScreenOn(
                true
            )
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager
                    .LayoutParams
                    .FLAG_SHOW_WHEN_LOCKED or
                        WindowManager
                            .LayoutParams
                            .FLAG_TURN_SCREEN_ON
            )
        }

        window.addFlags(
            WindowManager
                .LayoutParams
                .FLAG_KEEP_SCREEN_ON
        )
    }

    private fun buildScreen(
        label: String,
    ) {
        val root =
            LinearLayout(
                this
            ).apply {
                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER

                setPadding(
                    56,
                    80,
                    56,
                    80,
                )

                setBackgroundColor(
                    Color.rgb(
                        7,
                        5,
                        9,
                    )
                )
            }

        val brand =
            TextView(
                this
            ).apply {
                text =
                    "CYPHER"

                setTextColor(
                    Color.rgb(
                        138,
                        43,
                        226,
                    )
                )

                textSize =
                    18f

                gravity =
                    Gravity.CENTER

                letterSpacing =
                    0.28f
            }

        val time =
            TextView(
                this
            ).apply {
                text =
                    SimpleDateFormat(
                        "h:mm a",
                        Locale.getDefault(),
                    ).format(
                        Date()
                    )

                setTextColor(
                    Color.WHITE
                )

                textSize =
                    56f

                gravity =
                    Gravity.CENTER

                setPadding(
                    0,
                    60,
                    0,
                    16,
                )
            }

        val labelView =
            TextView(
                this
            ).apply {
                text =
                    label.uppercase(
                        Locale.getDefault()
                    )

                setTextColor(
                    Color.rgb(
                        118,
                        255,
                        3,
                    )
                )

                textSize =
                    18f

                gravity =
                    Gravity.CENTER

                letterSpacing =
                    0.12f

                setPadding(
                    0,
                    0,
                    0,
                    72,
                )
            }

        val snooze =
            Button(
                this
            ).apply {
                text =
                    "SNOOZE 10 MIN"

                textSize =
                    16f

                isAllCaps =
                    false

                setOnClickListener {
                    sendAlarmAction(
                        CypherAlarmActionReceiver
                            .ACTION_SNOOZE
                    )
                }
            }

        val dismiss =
            Button(
                this
            ).apply {
                text =
                    "DISMISS"

                textSize =
                    16f

                isAllCaps =
                    false

                setOnClickListener {
                    sendAlarmAction(
                        CypherAlarmActionReceiver
                            .ACTION_DISMISS
                    )
                }
            }

        val buttonParams =
            LinearLayout.LayoutParams(
                LinearLayout
                    .LayoutParams
                    .MATCH_PARENT,
                LinearLayout
                    .LayoutParams
                    .WRAP_CONTENT,
            ).apply {
                topMargin =
                    20
            }

        root.addView(
            brand
        )

        root.addView(
            time
        )

        root.addView(
            labelView
        )

        root.addView(
            snooze,
            buttonParams,
        )

        root.addView(
            dismiss,
            buttonParams,
        )

        setContentView(
            root
        )
    }

    private fun sendAlarmAction(
        action: String,
    ) {
        sendBroadcast(
            Intent(
                this,
                CypherAlarmActionReceiver::class.java,
            ).apply {
                this.action =
                    action

                putExtra(
                    CypherAlarmRingingService
                        .EXTRA_ALARM_ID,
                    alarmId,
                )
            }
        )

        finishAndRemoveTask()
    }
}
