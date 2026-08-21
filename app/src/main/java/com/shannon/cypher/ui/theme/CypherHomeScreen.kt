package com.shannon.cypher.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.shannon.cypher.R
import com.shannon.cypher.audio.CypherSpeechRecognizer
import com.shannon.cypher.identity.CypherNameNormalizer
import com.shannon.cypher.ui.theme.CypherTheme
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin


@Composable
fun CypherHomeScreen() {

    val background = Color(0xFF070509)
    val panel = Color(0xFF110D16)

    val accent = Color(0xFF8A2BE2)
    val secondaryAccent = Color(0xFF76FF03)
    val secondaryText = Color(0xFFA99AAF)

    val context = LocalContext.current


    var isListening by remember {
        mutableStateOf(false)
    }

    var microphoneLevel by remember {
        mutableStateOf(0f)
    }

    var recognizedText by remember {
        mutableStateOf("")
    }


    val speechRecognizer = remember {
        CypherSpeechRecognizer(
            context
        )
    }


    DisposableEffect(Unit) {

        onDispose {
            speechRecognizer.destroy()
        }
    }


    fun startListening() {

        recognizedText = ""

        speechRecognizer.start(

            onListeningChanged = {
                    listening ->

                isListening = listening

                if (!listening) {
                    microphoneLevel = 0f
                }
            },

            onLevelChanged = {
                    level ->

                microphoneLevel = level
            },

            onTextRecognized = {
                    text ->

                recognizedText =
                    CypherNameNormalizer.normalize(
                        text
                    )
            },
        )
    }


    val microphonePermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {

                startListening()

            } else {

                isListening = false
            }
        }


    val currentHour =
        Calendar
            .getInstance()
            .get(
                Calendar.HOUR_OF_DAY
            )


    val greeting =
        when (currentHour) {

            in 5..11 ->
                "Good morning, Shannon."

            in 12..16 ->
                "Good afternoon, Shannon."

            else ->
                "Good evening, Shannon."
        }


    val infiniteTransition =
        rememberInfiniteTransition(
            label =
                "CypherCoreAnimation"
        )


    val idlePulse by
    infiniteTransition.animateFloat(

        initialValue = 0.96f,
        targetValue = 1.04f,

        animationSpec =
            infiniteRepeatable(

                animation = tween(
                    durationMillis = 1800,
                    easing =
                        FastOutSlowInEasing,
                ),

                repeatMode =
                    RepeatMode.Reverse,
            ),

        label = "IdlePulse",
    )


    val middleRotation by
    infiniteTransition.animateFloat(

        initialValue = 0f,
        targetValue = 360f,

        animationSpec =
            infiniteRepeatable(

                animation = tween(
                    durationMillis =
                        if (isListening) {
                            3000
                        } else {
                            9000
                        },
                ),

                repeatMode =
                    RepeatMode.Restart,
            ),

        label =
            "MiddleRotation",
    )


    Surface(
        modifier =
            Modifier.fillMaxSize(),
        color = background,
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 24.dp,
                    vertical = 40.dp,
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally,
        ) {


            Text(
                text = "C Y P H E R",
                color = accent,
                fontSize = 28.sp,
                fontWeight =
                    FontWeight.Light,
                letterSpacing = 8.sp,
            )


            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )


            Text(
                text =
                    if (isListening) {
                        "LISTENING"
                    } else {
                        "SYSTEM ONLINE"
                    },

                color =
                    if (isListening) {
                        secondaryAccent
                    } else {
                        secondaryText
                    },

                fontSize = 12.sp,
                letterSpacing = 3.sp,
            )


            Spacer(
                modifier =
                    Modifier.height(56.dp)
            )


            // CYPHER CORE
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .scale(
                        if (isListening) {
                            1.0f
                        } else {
                            idlePulse
                        }
                    ),

                contentAlignment =
                    Alignment.Center,
            ) {


                // Voice-reactive outer ring
                Canvas(
                    modifier =
                        Modifier.fillMaxSize()
                ) {

                    val centerX =
                        size.width / 2f

                    val centerY =
                        size.height / 2f

                    val baseRadius =
                        (
                                size.minDimension /
                                        2f
                                ) -
                                6.dp.toPx()


                    val waveStrength =
                        if (isListening) {

                            microphoneLevel *
                                    18.dp.toPx()

                        } else {

                            0f
                        }


                    val path = Path()

                    val points = 180


                    for (
                    index in 0..points
                    ) {

                        val angle =
                            (
                                    index.toFloat() /
                                            points.toFloat()
                                    ) *
                                    (
                                            Math.PI *
                                                    2.0
                                            )


                        val wave =
                            sin(
                                angle * 12.0
                            ).toFloat()


                        val radius =
                            baseRadius +
                                    (
                                            wave *
                                                    waveStrength
                                            )


                        val x =
                            centerX +
                                    cos(angle)
                                        .toFloat() *
                                    radius


                        val y =
                            centerY +
                                    sin(angle)
                                        .toFloat() *
                                    radius


                        if (index == 0) {

                            path.moveTo(
                                x,
                                y
                            )

                        } else {

                            path.lineTo(
                                x,
                                y
                            )
                        }
                    }


                    path.close()


                    drawPath(
                        path = path,

                        color =
                            if (isListening) {
                                secondaryAccent
                            } else {
                                accent
                            },

                        style = Stroke(
                            width =
                                2.dp.toPx(),

                            cap =
                                StrokeCap.Round,
                        ),
                    )
                }


                // Inner HUD ring
                Box(
                    modifier =
                        Modifier.size(160.dp),

                    contentAlignment =
                        Alignment.Center,
                ) {


                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(
                                middleRotation
                            )
                    ) {

                        val strokeWidth =
                            3.dp.toPx()


                        val arcSize =
                            Size(

                                width =
                                    size.width -
                                            strokeWidth,

                                height =
                                    size.height -
                                            strokeWidth,
                            )


                        val topLeft =
                            Offset(

                                x =
                                    strokeWidth /
                                            2,

                                y =
                                    strokeWidth /
                                            2,
                            )


                        drawArc(
                            color = accent,
                            startAngle = -90f,
                            sweepAngle = 70f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,

                            style = Stroke(
                                width =
                                    strokeWidth,

                                cap =
                                    StrokeCap.Round,
                            ),
                        )


                        drawArc(
                            color = accent,
                            startAngle = 20f,
                            sweepAngle = 85f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,

                            style = Stroke(
                                width =
                                    strokeWidth,

                                cap =
                                    StrokeCap.Round,
                            ),
                        )


                        drawArc(
                            color =
                                secondaryAccent,

                            startAngle = 145f,
                            sweepAngle = 45f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,

                            style = Stroke(
                                width =
                                    strokeWidth,

                                cap =
                                    StrokeCap.Round,
                            ),
                        )


                        drawArc(
                            color = accent,
                            startAngle = 220f,
                            sweepAngle = 95f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,

                            style = Stroke(
                                width =
                                    strokeWidth,

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
                            "Cypher",

                        modifier =
                            Modifier.size(
                                105.dp
                            ),
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(38.dp)
            )


            Text(
                text = greeting,
                color = Color.White,
                fontSize = 20.sp,

                fontWeight =
                    FontWeight.Medium,
            )


            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )


            Text(
                text =
                    if (isListening) {

                        "I'm listening..."

                    } else if (
                        recognizedText
                            .isNotBlank()
                    ) {

                        recognizedText

                    } else {

                        "Awaiting your command."
                    },

                color =
                    if (isListening) {
                        secondaryAccent
                    } else {
                        secondaryText
                    },

                fontSize = 14.sp,
            )


            Spacer(
                modifier =
                    Modifier.height(42.dp)
            )


            // STATUS PANEL
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = panel,

                        shape =
                            RoundedCornerShape(
                                18.dp
                            ),
                    )
                    .border(
                        width = 1.dp,

                        color =
                            accent.copy(
                                alpha = 0.35f
                            ),

                        shape =
                            RoundedCornerShape(
                                18.dp
                            ),
                    )
                    .padding(
                        20.dp
                    ),
            ) {

                Column {

                    Text(
                        text =
                            "SYSTEM STATUS",

                        color = accent,
                        fontSize = 12.sp,
                        letterSpacing = 2.sp,
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                18.dp
                            )
                    )


                    StatusRow(
                        label = "LOCATION",
                        value = "Taree",

                        valueColor =
                            Color.White,
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                12.dp
                            )
                    )


                    StatusRow(
                        label = "STATUS",
                        value = "ONLINE",

                        valueColor =
                            secondaryAccent,
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.weight(1f)
            )


            Text(
                text = "TAP TO SPEAK",
                color = secondaryText,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
            )


            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )


            // MIC BUTTON
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clickable {

                        if (isListening) {

                            speechRecognizer
                                .cancel()

                        } else {

                            val permissionGranted =
                                ContextCompat
                                    .checkSelfPermission(
                                        context,
                                        Manifest
                                            .permission
                                            .RECORD_AUDIO,
                                    ) ==
                                        PackageManager
                                            .PERMISSION_GRANTED


                            if (
                                permissionGranted
                            ) {

                                startListening()

                            } else {

                                microphonePermissionLauncher
                                    .launch(
                                        Manifest
                                            .permission
                                            .RECORD_AUDIO
                                    )
                            }
                        }
                    }
                    .background(
                        color =
                            accent.copy(
                                alpha = 0.12f
                            ),

                        shape =
                            CircleShape,
                    )
                    .border(
                        width =
                            if (isListening) {
                                2.dp
                            } else {
                                1.dp
                            },

                        color =
                            if (isListening) {
                                secondaryAccent
                            } else {
                                accent
                            },

                        shape =
                            CircleShape,
                    ),

                contentAlignment =
                    Alignment.Center,
            ) {


                Text(
                    text =
                        if (isListening) {
                            "STOP"
                        } else {
                            "MIC"
                        },

                    color =
                        secondaryAccent,

                    fontSize =
                        13.sp,

                    fontWeight =
                        FontWeight.Bold,
                )
            }


            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )
        }
    }
}


@Composable
private fun StatusRow(
    label: String,
    value: String,
    valueColor: Color,
) {

    Row(
        modifier =
            Modifier.fillMaxWidth(),

        horizontalArrangement =
            Arrangement.SpaceBetween,
    ) {

        Text(
            text = label,
            color =
                Color(0xFF607D8B),
            fontSize = 12.sp,
        )

        Text(
            text = value,
            color = valueColor,
            fontSize = 13.sp,

            fontWeight =
                FontWeight.Medium,
        )
    }
}


@Preview(
    showBackground = true,
    backgroundColor = 0xFF070509,
)
@Composable
fun CypherHomeScreenPreview() {

    CypherTheme {
        CypherHomeScreen()
    }
}