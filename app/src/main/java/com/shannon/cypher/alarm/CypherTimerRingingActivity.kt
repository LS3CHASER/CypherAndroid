package com.shannon.cypher.alarm

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CypherTimerRingingActivity :
    Activity() {

    companion object {
        @Volatile
        private var activeInstance:
                CypherTimerRingingActivity? =
            null

        fun finishActiveInstance() {
            activeInstance
                ?.runOnUiThread {
                    activeInstance
                        ?.finishAndRemoveTask()
                }
        }
    }

    private var timerId: Long =
        -1L

    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(
            savedInstanceState
        )

        activeInstance = this

        showOverLockScreen()

        timerId =
            intent.getLongExtra(
                CypherTimerRingingService.EXTRA_TIMER_ID,
                -1L,
            )

        val timer =
            CypherAlarmRepository(
                applicationContext
            ).getTimer(
                timerId
            )

        buildScreen(
            label =
                timer?.label
                    ?.ifBlank {
                        "Cypher Timer"
                    }
                    ?: "Cypher Timer",
        )
    }

    override fun onDestroy() {
        if (
            activeInstance === this
        ) {
            activeInstance = null
        }

        super.onDestroy()
    }

    private fun showOverLockScreen() {
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O_MR1
        ) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
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
                text = "CYPHER"

                setTextColor(
                    Color.rgb(
                        138,
                        43,
                        226,
                    )
                )

                textSize = 18f
                gravity = Gravity.CENTER
                letterSpacing = 0.28f
            }

        val heading =
            TextView(
                this
            ).apply {
                text = "TIMER FINISHED"

                setTextColor(
                    Color.WHITE
                )

                textSize = 38f
                gravity = Gravity.CENTER

                setPadding(
                    0,
                    60,
                    0,
                    18,
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

                textSize = 18f
                gravity = Gravity.CENTER
                letterSpacing = 0.12f

                setPadding(
                    0,
                    0,
                    0,
                    18,
                )
            }

        val finishedAt =
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
                    Color.LTGRAY
                )

                textSize = 16f
                gravity = Gravity.CENTER

                setPadding(
                    0,
                    0,
                    0,
                    72,
                )
            }

        val dismiss =
            Button(
                this
            ).apply {
                text = "DISMISS"
                textSize = 18f
                isAllCaps = false

                setOnClickListener {
                    sendBroadcast(
                        android.content.Intent(
                            this@CypherTimerRingingActivity,
                            CypherTimerActionReceiver::class.java,
                        ).apply {
                            action =
                                CypherTimerActionReceiver.ACTION_DISMISS

                            putExtra(
                                CypherTimerRingingService.EXTRA_TIMER_ID,
                                timerId,
                            )
                        }
                    )

                    finishAndRemoveTask()
                }
            }

        val buttonParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = 20
            }

        root.addView(brand)
        root.addView(heading)
        root.addView(labelView)
        root.addView(finishedAt)
        root.addView(
            dismiss,
            buttonParams,
        )

        setContentView(root)
    }
}
