package com.shannon.cypher.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build


class CypherTaskReminderScheduler(
    context: Context,
) {

    private val appContext =
        context.applicationContext


    private val alarmManager =
        appContext.getSystemService(
            Context.ALARM_SERVICE
        ) as AlarmManager


    fun scheduleTaskReminder(
        taskId: Long,
        taskTitle: String,
        dueAtMillis: Long,
    ) {

        if (
            taskId <= 0L ||
            taskTitle.isBlank()
        ) {

            return
        }


        /*
         * Don't schedule reminders that are already
         * in the past.
         */
        if (
            dueAtMillis <=
            System.currentTimeMillis()
        ) {

            return
        }


        val pendingIntent =
            createPendingIntent(
                taskId =
                    taskId,

                taskTitle =
                    taskTitle,
            )


        /*
         * Android 12+ restricts exact alarms.
         *
         * If Cypher has permission to schedule exact
         * alarms, use setExactAndAllowWhileIdle().
         *
         * Otherwise fall back to setAndAllowWhileIdle()
         * until exact-alarm access is granted.
         */
        val canScheduleExactAlarm =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S
            ) {

                alarmManager
                    .canScheduleExactAlarms()

            } else {

                true
            }


        if (
            canScheduleExactAlarm
        ) {

            alarmManager
                .setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    dueAtMillis,
                    pendingIntent,
                )

        } else {

            alarmManager
                .setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    dueAtMillis,
                    pendingIntent,
                )
        }
    }


    fun cancelTaskReminder(
        taskId: Long,
    ) {

        if (
            taskId <= 0L
        ) {

            return
        }


        val intent =
            Intent(
                appContext,
                CypherTaskReminderReceiver::class.java,
            )


        val pendingIntent =
            PendingIntent.getBroadcast(
                appContext,
                requestCodeForTask(
                    taskId
                ),
                intent,
                PendingIntent.FLAG_NO_CREATE or
                        PendingIntent.FLAG_IMMUTABLE,
            )


        if (
            pendingIntent != null
        ) {

            alarmManager
                .cancel(
                    pendingIntent
                )


            pendingIntent
                .cancel()
        }
    }


    fun rescheduleTaskReminder(
        taskId: Long,
        taskTitle: String,
        dueAtMillis: Long?,
    ) {

        /*
         * Remove any reminder previously associated
         * with this task.
         */
        cancelTaskReminder(
            taskId
        )


        /*
         * A null due date means this task no longer
         * needs a reminder.
         */
        if (
            dueAtMillis == null
        ) {

            return
        }


        scheduleTaskReminder(
            taskId =
                taskId,

            taskTitle =
                taskTitle,

            dueAtMillis =
                dueAtMillis,
        )
    }


    private fun createPendingIntent(
        taskId: Long,
        taskTitle: String,
    ): PendingIntent {

        val intent =
            Intent(
                appContext,
                CypherTaskReminderReceiver::class.java,
            ).apply {

                putExtra(
                    CypherTaskReminderReceiver.EXTRA_TASK_ID,
                    taskId,
                )


                putExtra(
                    CypherTaskReminderReceiver.EXTRA_TASK_TITLE,
                    taskTitle,
                )
            }


        return PendingIntent.getBroadcast(
            appContext,
            requestCodeForTask(
                taskId
            ),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE,
        )
    }


    private fun requestCodeForTask(
        taskId: Long,
    ): Int {

        /*
         * Each task gets its own PendingIntent.
         *
         * This prevents one task reminder from
         * replacing another task's reminder.
         */
        return taskId.hashCode()
    }
}