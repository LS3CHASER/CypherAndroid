package com.shannon.cypher.ui.tasks

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shannon.cypher.R
import com.shannon.cypher.tasks.CypherTaskEntity
import com.shannon.cypher.tasks.CypherTaskRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.launch


@Composable
fun CypherTaskScreen(
    taskRepository: CypherTaskRepository,
    isListening: Boolean,
    isThinking: Boolean,
    isSpeaking: Boolean,
    onMenuClick: () -> Unit,
    onMicClick: () -> Unit,
) {

    val context =
        LocalContext.current

    val background =
        Color(
            0xFF070509
        )

    val panel =
        Color(
            0xFF110D16
        )

    val accent =
        Color(
            0xFF8A2BE2
        )

    val secondaryAccent =
        Color(
            0xFF76FF03
        )

    val secondaryText =
        Color(
            0xFFA99AAF
        )

    val overdueColor =
        Color(
            0xFFFF6B6B
        )


    val compactRingColor =
        if (
            isListening
        ) {
            secondaryAccent
        } else {
            accent
        }


    val coroutineScope =
        rememberCoroutineScope()


    val focusManager =
        LocalFocusManager.current


    /*
     * Live Room observation.
     */
    val openTasks by
    taskRepository
        .observeOpenTasks()
        .collectAsState(
            initial =
                emptyList()
        )


    val completedTasks by
    taskRepository
        .observeCompletedTasks()
        .collectAsState(
            initial =
                emptyList()
        )


    var showTaskDialog by
    remember {
        mutableStateOf(
            false
        )
    }


    var editingTask by
    remember {
        mutableStateOf<
                CypherTaskEntity?
                >(
            null
        )
    }


    var taskText by
    remember {
        mutableStateOf(
            ""
        )
    }


    var taskInputError by
    remember {
        mutableStateOf(
            false
        )
    }


    /*
     * Null = no due date.
     * Otherwise stores the selected local date/time.
     */
    var selectedDueAtMillis by
    remember {
        mutableStateOf<
                Long?
                >(
            null
        )
    }


    fun formatDueDate(
        millis: Long,
    ): String {

        val due =
            Calendar
                .getInstance()
                .apply {
                    timeInMillis =
                        millis
                }


        val now =
            Calendar
                .getInstance()


        val todayStart =
            (now.clone() as Calendar)
                .apply {
                    set(
                        Calendar.HOUR_OF_DAY,
                        0
                    )
                    set(
                        Calendar.MINUTE,
                        0
                    )
                    set(
                        Calendar.SECOND,
                        0
                    )
                    set(
                        Calendar.MILLISECOND,
                        0
                    )
                }


        val dueStart =
            (due.clone() as Calendar)
                .apply {
                    set(
                        Calendar.HOUR_OF_DAY,
                        0
                    )
                    set(
                        Calendar.MINUTE,
                        0
                    )
                    set(
                        Calendar.SECOND,
                        0
                    )
                    set(
                        Calendar.MILLISECOND,
                        0
                    )
                }


        val dayDifference =
            (
                    dueStart.timeInMillis -
                            todayStart.timeInMillis
                    ) /
                    (
                            24L *
                                    60L *
                                    60L *
                                    1000L
                            )


        val dayText =
            when (
                dayDifference
            ) {

                0L ->
                    "Today"

                1L ->
                    "Tomorrow"

                -1L ->
                    "Yesterday"

                else ->
                    SimpleDateFormat(
                        "EEE d MMM",
                        Locale.getDefault(),
                    ).format(
                        millis
                    )
            }


        val timeText =
            SimpleDateFormat(
                "h:mm a",
                Locale.getDefault(),
            ).format(
                millis
            )


        return "$dayText • $timeText"
    }


    fun openDueDateTimePicker() {

        val base =
            Calendar
                .getInstance()
                .apply {

                    selectedDueAtMillis
                        ?.let {
                            timeInMillis =
                                it
                        }
                }


        DatePickerDialog(
            context,
            {
                    _,
                    year,
                    month,
                    dayOfMonth ->

                val selectedDate =
                    Calendar
                        .getInstance()
                        .apply {
                            set(
                                Calendar.YEAR,
                                year
                            )
                            set(
                                Calendar.MONTH,
                                month
                            )
                            set(
                                Calendar.DAY_OF_MONTH,
                                dayOfMonth
                            )
                        }


                val initialHour =
                    if (
                        selectedDueAtMillis != null
                    ) {
                        base.get(
                            Calendar.HOUR_OF_DAY
                        )
                    } else {
                        9
                    }


                val initialMinute =
                    if (
                        selectedDueAtMillis != null
                    ) {
                        base.get(
                            Calendar.MINUTE
                        )
                    } else {
                        0
                    }


                TimePickerDialog(
                    context,
                    {
                            _,
                            hourOfDay,
                            minute ->

                        selectedDate
                            .apply {
                                set(
                                    Calendar.HOUR_OF_DAY,
                                    hourOfDay
                                )
                                set(
                                    Calendar.MINUTE,
                                    minute
                                )
                                set(
                                    Calendar.SECOND,
                                    0
                                )
                                set(
                                    Calendar.MILLISECOND,
                                    0
                                )
                            }


                        selectedDueAtMillis =
                            selectedDate
                                .timeInMillis
                    },
                    initialHour,
                    initialMinute,
                    false,
                ).show()
            },
            base.get(
                Calendar.YEAR
            ),
            base.get(
                Calendar.MONTH
            ),
            base.get(
                Calendar.DAY_OF_MONTH
            ),
        ).show()
    }


    fun completeTask(
        task: CypherTaskEntity,
    ) {

        coroutineScope.launch {

            taskRepository
                .completeTask(
                    task.id
                )
        }
    }


    fun reopenTask(
        task: CypherTaskEntity,
    ) {

        coroutineScope.launch {

            taskRepository
                .reopenTask(
                    task.id
                )
        }
    }


    fun openAddDialog() {

        editingTask =
            null

        taskText =
            ""

        selectedDueAtMillis =
            null

        taskInputError =
            false

        showTaskDialog =
            true
    }


    fun openEditDialog(
        task: CypherTaskEntity,
    ) {

        editingTask =
            task

        taskText =
            task.title

        selectedDueAtMillis =
            task.dueAtMillis

        taskInputError =
            false

        showTaskDialog =
            true
    }


    fun closeTaskDialog() {

        showTaskDialog =
            false

        editingTask =
            null

        taskText =
            ""

        selectedDueAtMillis =
            null

        taskInputError =
            false


        focusManager
            .clearFocus()
    }


    fun saveTask() {

        val cleanText =
            taskText
                .trim()
                .replace(
                    Regex("\\s+"),
                    " "
                )


        if (
            cleanText.isBlank()
        ) {

            taskInputError =
                true

            return
        }


        coroutineScope.launch {

            val existing =
                editingTask


            if (
                existing == null
            ) {

                taskRepository
                    .addTask(
                        title =
                            cleanText,

                        dueAtMillis =
                            selectedDueAtMillis,
                    )

            } else {

                taskRepository
                    .updateTaskTitle(
                        taskId =
                            existing.id,

                        newTitle =
                            cleanText,
                    )


                taskRepository
                    .updateTaskDueDate(
                        taskId =
                            existing.id,

                        dueAtMillis =
                            selectedDueAtMillis,
                    )
            }


            closeTaskDialog()
        }
    }


    val infiniteTransition =
        rememberInfiniteTransition(
            label =
                "TaskScreenCypherAnimation"
        )


    val ringRotation by
    infiniteTransition
        .animateFloat(

            initialValue =
                0f,

            targetValue =
                360f,

            animationSpec =
                infiniteRepeatable(

                    animation =
                        tween(

                            durationMillis =
                                when {

                                    isThinking ->
                                        1600

                                    isListening ->
                                        2400

                                    isSpeaking ->
                                        3500

                                    else ->
                                        7000
                                },

                            easing =
                                FastOutSlowInEasing,
                        ),

                    repeatMode =
                        RepeatMode.Restart,
                ),

            label =
                "TaskScreenRingRotation",
        )


    Surface(
        modifier =
            Modifier
                .fillMaxSize(),

        color =
            background,
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(
                        horizontal =
                            20.dp,

                        vertical =
                            12.dp,
                    ),
        ) {


            Row(
                modifier =
                    Modifier
                        .fillMaxWidth(),

                horizontalArrangement =
                    Arrangement
                        .SpaceBetween,

                verticalAlignment =
                    Alignment
                        .CenterVertically,
            ) {


                Box(
                    modifier =
                        Modifier
                            .size(
                                52.dp
                            )
                            .clickable {
                                onMenuClick()
                            },

                    contentAlignment =
                        Alignment.Center,
                ) {

                    Text(
                        text =
                            "☰",

                        color =
                            accent,

                        fontSize =
                            30.sp,

                        fontWeight =
                            FontWeight.Light,
                    )
                }


                Box(
                    modifier =
                        Modifier
                            .size(
                                76.dp
                            )
                            .clickable {

                                if (
                                    !isThinking
                                ) {

                                    onMicClick()
                                }
                            },

                    contentAlignment =
                        Alignment.Center,
                ) {


                    Canvas(
                        modifier =
                            Modifier
                                .size(
                                    70.dp
                                )
                                .rotate(
                                    ringRotation
                                ),
                    ) {


                        drawArc(
                            color =
                                compactRingColor,

                            startAngle =
                                -90f,

                            sweepAngle =
                                85f,

                            useCenter =
                                false,

                            style =
                                Stroke(
                                    width =
                                        2.4.dp.toPx(),

                                    cap =
                                        StrokeCap.Round,
                                ),
                        )


                        drawArc(
                            color =
                                compactRingColor,

                            startAngle =
                                45f,

                            sweepAngle =
                                55f,

                            useCenter =
                                false,

                            style =
                                Stroke(
                                    width =
                                        2.4.dp.toPx(),

                                    cap =
                                        StrokeCap.Round,
                                ),
                        )


                        drawArc(
                            color =
                                compactRingColor,

                            startAngle =
                                150f,

                            sweepAngle =
                                120f,

                            useCenter =
                                false,

                            style =
                                Stroke(
                                    width =
                                        2.4.dp.toPx(),

                                    cap =
                                        StrokeCap.Round,
                                ),
                        )
                    }


                    Image(
                        painter =
                            painterResource(
                                id =
                                    R.drawable
                                        .cypher_head
                            ),

                        contentDescription =
                            if (
                                isListening
                            ) {
                                "Cypher is listening"
                            } else {
                                "Cypher voice control"
                            },

                        modifier =
                            Modifier
                                .size(
                                    42.dp
                                ),
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        18.dp
                    )
            )


            Row(
                modifier =
                    Modifier
                        .fillMaxWidth(),

                horizontalArrangement =
                    Arrangement
                        .SpaceBetween,

                verticalAlignment =
                    Alignment
                        .CenterVertically,
            ) {

                Text(
                    text =
                        "TO-DO LIST",

                    color =
                        Color.White,

                    fontSize =
                        26.sp,

                    fontWeight =
                        FontWeight.Medium,

                    letterSpacing =
                        3.sp,
                )


                Row(
                    modifier =
                        Modifier
                            .clickable {
                                openAddDialog()
                            }
                            .background(
                                color =
                                    accent.copy(
                                        alpha =
                                            0.16f
                                    ),

                                shape =
                                    RoundedCornerShape(
                                        12.dp
                                    ),
                            )
                            .border(
                                width =
                                    1.dp,

                                color =
                                    accent.copy(
                                        alpha =
                                            0.35f
                                    ),

                                shape =
                                    RoundedCornerShape(
                                        12.dp
                                    ),
                            )
                            .padding(
                                horizontal =
                                    12.dp,

                                vertical =
                                    8.dp,
                            ),

                    verticalAlignment =
                        Alignment
                            .CenterVertically,
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Add,

                        contentDescription =
                            "Add task",

                        tint =
                            secondaryAccent,

                        modifier =
                            Modifier
                                .size(
                                    18.dp
                                ),
                    )


                    Spacer(
                        modifier =
                            Modifier.size(
                                6.dp
                            )
                    )


                    Text(
                        text =
                            "ADD TASK",

                        color =
                            secondaryAccent,

                        fontSize =
                            12.sp,

                        fontWeight =
                            FontWeight.Medium,

                        letterSpacing =
                            1.sp,
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        6.dp
                    )
            )


            Text(
                text =
                    "Tap a task to complete it. Tap completed tasks to restore them.",

                color =
                    secondaryText,

                fontSize =
                    13.sp,
            )


            Spacer(
                modifier =
                    Modifier.height(
                        24.dp
                    )
            )


            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(
                            rememberScrollState()
                        ),
            ) {


                if (
                    openTasks.isEmpty()
                ) {

                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    color =
                                        panel,

                                    shape =
                                        RoundedCornerShape(
                                            16.dp
                                        ),
                                )
                                .border(
                                    width =
                                        1.dp,

                                    color =
                                        accent.copy(
                                            alpha =
                                                0.25f
                                        ),

                                    shape =
                                        RoundedCornerShape(
                                            16.dp
                                        ),
                                )
                                .padding(
                                    20.dp
                                ),
                    ) {

                        Text(
                            text =
                                "Your to-do list is empty.",

                            color =
                                secondaryText,

                            fontSize =
                                15.sp,
                        )
                    }

                } else {


                    Text(
                        text =
                            "OPEN",

                        color =
                            accent,

                        fontSize =
                            12.sp,

                        letterSpacing =
                            2.sp,
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                10.dp
                            )
                    )


                    openTasks.forEach {
                            task ->


                        val dueAt =
                            task.dueAtMillis


                        val isOverdue =
                            dueAt != null &&
                                    dueAt <
                                    System.currentTimeMillis()


                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color =
                                            panel,

                                        shape =
                                            RoundedCornerShape(
                                                14.dp
                                            ),
                                    )
                                    .border(
                                        width =
                                            1.dp,

                                        color =
                                            if (
                                                isOverdue
                                            ) {
                                                overdueColor.copy(
                                                    alpha =
                                                        0.55f
                                                )
                                            } else {
                                                accent.copy(
                                                    alpha =
                                                        0.25f
                                                )
                                            },

                                        shape =
                                            RoundedCornerShape(
                                                14.dp
                                            ),
                                    )
                                    .padding(
                                        horizontal =
                                            10.dp,

                                        vertical =
                                            8.dp,
                                    ),

                            verticalAlignment =
                                Alignment
                                    .CenterVertically,
                        ) {


                            Row(
                                modifier =
                                    Modifier
                                        .weight(
                                            1f
                                        )
                                        .clickable {

                                            completeTask(
                                                task
                                            )
                                        }
                                        .padding(
                                            horizontal =
                                                6.dp,

                                            vertical =
                                                8.dp,
                                        ),

                                verticalAlignment =
                                    Alignment
                                        .CenterVertically,
                            ) {

                                Text(
                                    text =
                                        "☐",

                                    color =
                                        secondaryAccent,

                                    fontSize =
                                        24.sp,
                                )


                                Spacer(
                                    modifier =
                                        Modifier.size(
                                            12.dp
                                        )
                                )


                                Column(
                                    modifier =
                                        Modifier
                                            .weight(
                                                1f
                                            ),
                                ) {

                                    Text(
                                        text =
                                            task.title,

                                        color =
                                            Color.White,

                                        fontSize =
                                            16.sp,
                                    )


                                    if (
                                        dueAt != null
                                    ) {

                                        Spacer(
                                            modifier =
                                                Modifier.height(
                                                    3.dp
                                                )
                                        )


                                        Text(
                                            text =
                                                if (
                                                    isOverdue
                                                ) {
                                                    "OVERDUE • ${formatDueDate(dueAt)}"
                                                } else {
                                                    formatDueDate(
                                                        dueAt
                                                    )
                                                },

                                            color =
                                                if (
                                                    isOverdue
                                                ) {
                                                    overdueColor
                                                } else {
                                                    secondaryText
                                                },

                                            fontSize =
                                                12.sp,

                                            fontWeight =
                                                if (
                                                    isOverdue
                                                ) {
                                                    FontWeight.Medium
                                                } else {
                                                    FontWeight.Normal
                                                },
                                        )
                                    }
                                }
                            }


                            IconButton(
                                onClick = {

                                    openEditDialog(
                                        task
                                    )
                                },
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.Edit,

                                    contentDescription =
                                        "Edit ${task.title}",

                                    tint =
                                        accent,
                                )
                            }
                        }


                        Spacer(
                            modifier =
                                Modifier.height(
                                    10.dp
                                )
                        )
                    }
                }


                if (
                    completedTasks.isNotEmpty()
                ) {


                    Spacer(
                        modifier =
                            Modifier.height(
                                26.dp
                            )
                    )


                    Text(
                        text =
                            "COMPLETED",

                        color =
                            secondaryText,

                        fontSize =
                            12.sp,

                        letterSpacing =
                            2.sp,
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                10.dp
                            )
                    )


                    completedTasks
                        .take(
                            20
                        )
                        .forEach {
                                task ->


                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color =
                                                panel.copy(
                                                    alpha =
                                                        0.65f
                                                ),

                                            shape =
                                                RoundedCornerShape(
                                                    14.dp
                                                ),
                                        )
                                        .border(
                                            width =
                                                1.dp,

                                            color =
                                                secondaryText.copy(
                                                    alpha =
                                                        0.18f
                                                ),

                                            shape =
                                                RoundedCornerShape(
                                                    14.dp
                                                ),
                                        )
                                        .clickable {

                                            reopenTask(
                                                task
                                            )
                                        }
                                        .padding(
                                            horizontal =
                                                16.dp,

                                            vertical =
                                                14.dp,
                                        ),

                                verticalAlignment =
                                    Alignment
                                        .CenterVertically,
                            ) {


                                Text(
                                    text =
                                        "☑",

                                    color =
                                        secondaryText,

                                    fontSize =
                                        22.sp,
                                )


                                Spacer(
                                    modifier =
                                        Modifier.size(
                                            12.dp
                                        )
                                )


                                Column(
                                    modifier =
                                        Modifier
                                            .weight(
                                                1f
                                            ),
                                ) {

                                    Text(
                                        text =
                                            task.title,

                                        color =
                                            secondaryText,

                                        fontSize =
                                            15.sp,
                                    )


                                    task.dueAtMillis
                                        ?.let {
                                                due ->

                                            Spacer(
                                                modifier =
                                                    Modifier.height(
                                                        3.dp
                                                    )
                                            )


                                            Text(
                                                text =
                                                    "Due ${formatDueDate(due)}",

                                                color =
                                                    secondaryText.copy(
                                                        alpha =
                                                            0.7f
                                                    ),

                                                fontSize =
                                                    11.sp,
                                            )
                                        }
                                }
                            }


                            Spacer(
                                modifier =
                                    Modifier.height(
                                        8.dp
                                    )
                            )
                        }
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            40.dp
                        )
                )
            }
        }
    }


    if (
        showTaskDialog
    ) {

        AlertDialog(
            onDismissRequest = {
                closeTaskDialog()
            },

            containerColor =
                panel,

            title = {

                Text(
                    text =
                        if (
                            editingTask == null
                        ) {
                            "ADD TASK"
                        } else {
                            "EDIT TASK"
                        },

                    color =
                        Color.White,

                    letterSpacing =
                        2.sp,
                )
            },

            text = {

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth(),
                ) {


                    OutlinedTextField(
                        value =
                            taskText,

                        onValueChange = {
                                newValue ->

                            taskText =
                                newValue


                            if (
                                newValue.isNotBlank()
                            ) {

                                taskInputError =
                                    false
                            }
                        },

                        modifier =
                            Modifier
                                .fillMaxWidth(),

                        label = {

                            Text(
                                text =
                                    "Task"
                            )
                        },

                        singleLine =
                            true,

                        isError =
                            taskInputError,

                        supportingText =
                            if (
                                taskInputError
                            ) {
                                {
                                    Text(
                                        text =
                                            "Please enter a task."
                                    )
                                }
                            } else {
                                null
                            },

                        keyboardOptions =
                            KeyboardOptions(
                                imeAction =
                                    ImeAction.Done
                            ),

                        keyboardActions =
                            KeyboardActions(
                                onDone = {
                                    saveTask()
                                }
                            ),
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                14.dp
                            )
                    )


                    Text(
                        text =
                            "DUE DATE / TIME",

                        color =
                            secondaryText,

                        fontSize =
                            11.sp,

                        letterSpacing =
                            1.5.sp,
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                8.dp
                            )
                    )


                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth(),

                        horizontalArrangement =
                            Arrangement
                                .SpaceBetween,

                        verticalAlignment =
                            Alignment
                                .CenterVertically,
                    ) {


                        TextButton(
                            onClick = {
                                openDueDateTimePicker()
                            },
                        ) {

                            Text(
                                text =
                                    if (
                                        selectedDueAtMillis == null
                                    ) {
                                        "SET DATE & TIME"
                                    } else {
                                        formatDueDate(
                                            selectedDueAtMillis!!
                                        )
                                    },

                                color =
                                    secondaryAccent,
                            )
                        }


                        if (
                            selectedDueAtMillis != null
                        ) {

                            TextButton(
                                onClick = {

                                    selectedDueAtMillis =
                                        null
                                },
                            ) {

                                Text(
                                    text =
                                        "CLEAR",

                                    color =
                                        overdueColor,
                                )
                            }
                        }
                    }
                }
            },

            confirmButton = {

                TextButton(
                    onClick = {
                        saveTask()
                    },
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Save,

                        contentDescription =
                            null,

                        tint =
                            secondaryAccent,
                    )


                    Spacer(
                        modifier =
                            Modifier.size(
                                6.dp
                            )
                    )


                    Text(
                        text =
                            "SAVE",

                        color =
                            secondaryAccent,
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        closeTaskDialog()
                    },
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Close,

                        contentDescription =
                            null,

                        tint =
                            secondaryText,
                    )


                    Spacer(
                        modifier =
                            Modifier.size(
                                6.dp
                            )
                    )


                    Text(
                        text =
                            "CANCEL",

                        color =
                            secondaryText,
                    )
                }
            },
        )
    }
}