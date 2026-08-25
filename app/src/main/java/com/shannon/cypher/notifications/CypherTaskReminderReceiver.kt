package com.shannon.cypher.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class CypherTaskReminderReceiver :
    BroadcastReceiver() {

    companion object {

        const val EXTRA_TASK_ID =
            "cypher_task_id"

        const val EXTRA_TASK_TITLE =
            "cypher_task_title"
    }


    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {

        val taskId =
            intent.getLongExtra(
                EXTRA_TASK_ID,
                -1L,
            )


        val taskTitle =
            intent.getStringExtra(
                EXTRA_TASK_TITLE
            )
                ?.trim()
                .orEmpty()


        if (
            taskId <= 0L ||
            taskTitle.isBlank()
        ) {

            return
        }


        val notificationManager =
            CypherNotificationManager(
                context.applicationContext
            )


        /*
         * Make sure Cypher's notification channels exist.
         *
         * Normally MainActivity creates them, but the reminder
         * may fire while Cypher itself is not open.
         */
        notificationManager
            .createNotificationChannels()


        notificationManager
            .showTaskNotification(
                notificationId =
                    taskId.hashCode(),

                title =
                    "Task due",

                message =
                    taskTitle,
            )
    }
}