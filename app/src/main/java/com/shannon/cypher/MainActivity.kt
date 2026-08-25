package com.shannon.cypher

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.shannon.cypher.notifications.CypherNotificationManager
import com.shannon.cypher.ui.CypherHomeScreen
import com.shannon.cypher.ui.theme.CypherTheme


class MainActivity : ComponentActivity() {

    private lateinit var cypherNotificationManager:
            CypherNotificationManager


    /*
     * Destination requested by a notification tap.
     *
     * Examples:
     *
     * home
     * tasks
     * calendar
     * weather
     */
    private val notificationDestination =
        mutableStateOf<String?>(
            null
        )


    /*
     * These IDs are already captured now so we can
     * later highlight/open the exact task or calendar event.
     */
    private val notificationTaskId =
        mutableStateOf<Long?>(
            null
        )


    private val notificationCalendarEventId =
        mutableStateOf<Long?>(
            null
        )


    /*
     * Android 13+ notification permission.
     */
    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            /*
             * Nothing else is required here.
             *
             * Once permission is granted, Cypher's
             * real reminder system can post notifications.
             */
        }


    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {

        super.onCreate(
            savedInstanceState
        )


        /*
         * Initialise Cypher's notification manager.
         */
        cypherNotificationManager =
            CypherNotificationManager(
                applicationContext
            )


        /*
         * Create notification channels.
         *
         * Safe to call every time Cypher launches.
         */
        cypherNotificationManager
            .createNotificationChannels()


        /*
         * Android 13+ requires POST_NOTIFICATIONS
         * runtime permission.
         */
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            val permissionGranted =
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) ==
                        PackageManager.PERMISSION_GRANTED


            if (
                !permissionGranted
            ) {

                notificationPermissionLauncher
                    .launch(
                        Manifest.permission.POST_NOTIFICATIONS
                    )
            }
        }


        /*
         * Check whether Cypher was opened by tapping
         * a notification while the app was closed.
         */
        handleNotificationIntent(
            intent
        )


        enableEdgeToEdge()


        setContent {

            CypherTheme {

                /*
                 * Pass the requested notification destination
                 * into CypherHomeScreen.
                 *
                 * CypherHomeScreen now reacts to this value
                 * and changes to the appropriate internal screen.
                 */
                CypherHomeScreen(
                    notificationDestination =
                        notificationDestination.value,
                )
            }
        }
    }


    /*
     * Called when Cypher is already running and the
     * user taps another Cypher notification.
     */
    override fun onNewIntent(
        intent: Intent,
    ) {

        super.onNewIntent(
            intent
        )


        /*
         * Replace the Activity's current Intent with
         * the newly received notification Intent.
         */
        setIntent(
            intent
        )


        handleNotificationIntent(
            intent
        )
    }


    /*
     * Extract navigation information from a Cypher
     * notification.
     */
    private fun handleNotificationIntent(
        intent: Intent?,
    ) {

        if (
            intent == null
        ) {

            return
        }


        val destination =
            intent.getStringExtra(
                CypherNotificationManager.EXTRA_OPEN_SCREEN
            )


        /*
         * Normal launcher starts do not contain a
         * Cypher notification destination.
         */
        if (
            destination.isNullOrBlank()
        ) {

            return
        }


        notificationDestination.value =
            destination


        /*
         * Optional Task ID.
         */
        val taskId =
            intent.getLongExtra(
                CypherNotificationManager.EXTRA_TASK_ID,
                -1L,
            )


        notificationTaskId.value =
            if (
                taskId > 0L
            ) {

                taskId

            } else {

                null
            }


        /*
         * Optional Calendar Event ID.
         */
        val calendarEventId =
            intent.getLongExtra(
                CypherNotificationManager.EXTRA_CALENDAR_EVENT_ID,
                -1L,
            )


        notificationCalendarEventId.value =
            if (
                calendarEventId > 0L
            ) {

                calendarEventId

            } else {

                null
            }
    }
}