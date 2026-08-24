package com.shannon.cypher.ui.tasks

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shannon.cypher.R
import com.shannon.cypher.tasks.CypherTaskEntity
import com.shannon.cypher.tasks.CypherTaskRepository
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


    /*
     * All compact Cypher rings turn green
     * while the microphone is actively listening.
     */
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


    var openTasks by
    remember {
        mutableStateOf<
                List<CypherTaskEntity>
                >(
            emptyList()
        )
    }


    var completedTasks by
    remember {
        mutableStateOf<
                List<CypherTaskEntity>
                >(
            emptyList()
        )
    }


    suspend fun refreshTasks() {

        openTasks =
            taskRepository
                .getOpenTasks()


        completedTasks =
            taskRepository
                .getCompletedTasks()
    }


    LaunchedEffect(
        Unit
    ) {

        refreshTasks()
    }


    fun completeTask(
        task: CypherTaskEntity,
    ) {

        coroutineScope.launch {

            taskRepository
                .completeTask(
                    task.id
                )


            refreshTasks()
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


            refreshTasks()
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

                    /*
                     * Keep the Tasks UI below Samsung / Android
                     * status and notification icons.
                     */
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


                /*
                 * Hamburger menu.
                 */
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


                /*
                 * Compact Cypher voice control.
                 *
                 * It now sits safely below the system status bar.
                 * All rings turn green while listening.
                 */
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


            Spacer(
                modifier =
                    Modifier.height(
                        6.dp
                    )
            )


            Text(
                text =
                    "Tap an open task to complete it. Tap a completed task to restore it.",

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
                                            accent.copy(
                                                alpha =
                                                    0.25f
                                            ),

                                        shape =
                                            RoundedCornerShape(
                                                14.dp
                                            ),
                                    )
                                    .clickable {

                                        completeTask(
                                            task
                                        )
                                    }
                                    .padding(
                                        horizontal =
                                            16.dp,

                                        vertical =
                                            16.dp,
                                    ),

                            verticalAlignment =
                                Alignment.CenterVertically,
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


                            Text(
                                text =
                                    task.title,

                                color =
                                    Color.White,

                                fontSize =
                                    16.sp,

                                modifier =
                                    Modifier
                                        .weight(
                                            1f
                                        ),
                            )
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


                                Text(
                                    text =
                                        task.title,

                                    color =
                                        secondaryText,

                                    fontSize =
                                        15.sp,

                                    modifier =
                                        Modifier
                                            .weight(
                                                1f
                                            ),
                                )
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
}