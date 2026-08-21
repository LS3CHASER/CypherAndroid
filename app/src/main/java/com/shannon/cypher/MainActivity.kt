package com.shannon.cypher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shannon.cypher.ui.theme.CypherTheme
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.clickable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.util.Calendar
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.runtime.DisposableEffect
import kotlin.math.sqrt
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.graphics.Path
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            CypherTheme {
                CypherHomeScreen()
            }
        }
    }
}


@Composable
fun CypherHomeScreen() {

    val background = Color(0xFF070509)
    val panel = Color(0xFF110D16)

    // Joker-inspired colours
    val accent = Color(0xFF8A2BE2)
    val secondaryAccent = Color(0xFF76FF03)

    val secondaryText = Color(0xFFA99AAF)

    var isListening by remember {
        mutableStateOf(false)
    }

    var microphoneLevel by remember {
        mutableStateOf(0f)
    }

    val context = LocalContext.current

    val microphonePermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->

            isListening = granted
        }
    DisposableEffect(isListening) {

        var audioRecord: AudioRecord? = null

        var keepRecording = true

        val mainHandler = Handler(
            Looper.getMainLooper()
        )

        var recordingThread: Thread? = null

        if (isListening) {

            val sampleRate = 16000

            val bufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )

            if (bufferSize > 0) {

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                )

                audioRecord?.startRecording()

                recordingThread = Thread {

                    val buffer = ShortArray(
                        bufferSize
                    )

                    while (keepRecording) {

                        val samplesRead =
                            audioRecord?.read(
                                buffer,
                                0,
                                buffer.size,
                            ) ?: 0

                        if (samplesRead > 0) {

                            var sum = 0.0

                            for (index in 0 until samplesRead) {

                                val sample =
                                    buffer[index].toDouble()

                                sum += sample * sample
                            }

                            val rms = sqrt(
                                sum / samplesRead
                            )

                            val normalisedLevel =
                                (rms / 6000.0)
                                    .coerceIn(
                                        0.0,
                                        1.0,
                                    )
                                    .toFloat()

                            mainHandler.post {
                                microphoneLevel =
                                    normalisedLevel
                            }
                        }
                    }
                }

                recordingThread?.start()
            }
        }

        onDispose {

            keepRecording = false

            try {
                audioRecord?.stop()
            } catch (_: IllegalStateException) {
            }

            audioRecord?.release()

            recordingThread?.interrupt()

            microphoneLevel = 0f
        }
    }

    val currentHour = Calendar
        .getInstance()
        .get(Calendar.HOUR_OF_DAY)

    val greeting = when (currentHour) {
        in 5..11 -> "Good morning, Shannon."
        in 12..16 -> "Good afternoon, Shannon."
        else -> "Good evening, Shannon."
    }

    val infiniteTransition = rememberInfiniteTransition(
        label = "CypherCoreAnimation"
    )

    // Slow breathing animation for the outer ring.
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1800,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "IdlePulse",
    )

    // Continuous rotation for the segmented HUD ring.
    val middleRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isListening) 3000 else 9000,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "MiddleRotation",
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = background,
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 24.dp,
                    vertical = 40.dp,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Text(
                text = "C Y P H E R",
                color = accent,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 8.sp,
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = if (isListening) "LISTENING" else "SYSTEM ONLINE",
                color = if (isListening) secondaryAccent else secondaryText,
                fontSize = 12.sp,
                letterSpacing = 3.sp,
            )

            Spacer(
                modifier = Modifier.height(56.dp)
            )

            // Main Cypher core.
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .scale(
                        if (isListening) 1.0f else idlePulse
                    ),
                contentAlignment = Alignment.Center,
            ) {

                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {

                    val centerX = size.width / 2f
                    val centerY = size.height / 2f

                    val baseRadius =
                        (size.minDimension / 2f) - 6.dp.toPx()

                    val waveStrength = if (isListening) {
                        microphoneLevel * 18.dp.toPx()
                    } else {
                        0f
                    }

                    val path = Path()

                    val points = 180

                    for (index in 0..points) {

                        val angle =
                            (index.toFloat() / points.toFloat()) *
                                    (Math.PI * 2.0)

                        val wave =
                            sin(angle * 12.0).toFloat()

                        val radius =
                            baseRadius + (wave * waveStrength)

                        val x =
                            centerX +
                                    cos(angle).toFloat() * radius

                        val y =
                            centerY +
                                    sin(angle).toFloat() * radius

                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }

                    path.close()

                    drawPath(
                        path = path,
                        color = if (isListening) {
                            secondaryAccent
                        } else {
                            accent
                        },
                        style = Stroke(
                            width = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                        ),
                    )
                }

                // Stationary container for the segmented ring
                // and centre Cypher core.
                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center,
                ) {

                    // Rotating segmented HUD ring.
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(middleRotation)
                    ) {

                        val strokeWidth = 3.dp.toPx()

                        val arcSize = Size(
                            width = size.width - strokeWidth,
                            height = size.height - strokeWidth,
                        )

                        val topLeft = Offset(
                            x = strokeWidth / 2,
                            y = strokeWidth / 2,
                        )

                        // Purple segment 1
                        drawArc(
                            color = accent,
                            startAngle = -90f,
                            sweepAngle = 70f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(
                                width = strokeWidth,
                                cap = StrokeCap.Round,
                            ),
                        )

                        // Purple segment 2
                        drawArc(
                            color = accent,
                            startAngle = 20f,
                            sweepAngle = 85f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(
                                width = strokeWidth,
                                cap = StrokeCap.Round,
                            ),
                        )

                        // Green accent segment
                        drawArc(
                            color = secondaryAccent,
                            startAngle = 145f,
                            sweepAngle = 45f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(
                                width = strokeWidth,
                                cap = StrokeCap.Round,
                            ),
                        )

                        // Purple segment 3
                        drawArc(
                            color = accent,
                            startAngle = 220f,
                            sweepAngle = 95f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(
                                width = strokeWidth,
                                cap = StrokeCap.Round,
                            ),
                        )
                    }

                    // Stationary Cypher centre core.
                    Image(
                            painter = painterResource(
                                id = R.drawable.cypher_head
                            ),
                            contentDescription = "Cypher",
                            modifier = Modifier.size(105.dp),
                        )

                }
            }

            Spacer(
                modifier = Modifier.height(38.dp)
            )

            Text(
                text = greeting,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text = if (isListening) {
                    "I'm listening..."
                } else {
                    "Awaiting your command."
                },
                color = if (isListening) secondaryAccent else secondaryText,
                fontSize = 14.sp,
            )

            Spacer(
                modifier = Modifier.height(42.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = panel,
                        shape = RoundedCornerShape(
                            18.dp
                        ),
                    )
                    .border(
                        width = 1.dp,
                        color = accent.copy(
                            alpha = 0.35f
                        ),
                        shape = RoundedCornerShape(
                            18.dp
                        ),
                    )
                    .padding(20.dp),
            ) {

                Column {

                    Text(
                        text = "SYSTEM STATUS",
                        color = accent,
                        fontSize = 12.sp,
                        letterSpacing = 2.sp,
                    )

                    Spacer(
                        modifier = Modifier.height(
                            18.dp
                        )
                    )

                    StatusRow(
                        label = "LOCATION",
                        value = "Taree",
                        valueColor = Color.White,
                    )

                    Spacer(
                        modifier = Modifier.height(
                            12.dp
                        )
                    )

                    StatusRow(
                        label = "STATUS",
                        value = "ONLINE",
                        valueColor = secondaryAccent,
                    )
                }
            }

            Spacer(
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "TAP TO SPEAK",
                color = secondaryText,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clickable {

                        if (isListening) {

                            isListening = false

                        } else {

                            val permissionGranted =
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                            if (permissionGranted) {

                                isListening = true

                            } else {

                                microphonePermissionLauncher.launch(
                                    Manifest.permission.RECORD_AUDIO
                                )
                            }
                        }
                    }
                    .background(
                        color = accent.copy(
                            alpha = 0.12f
                        ),
                        shape = CircleShape,
                    )
                    .border(
                        width = if (isListening) 2.dp else 1.dp,
                        color = if (isListening) secondaryAccent else accent,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {

                Text(
                    text = if (isListening) "STOP" else "MIC",
                    color = secondaryAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )
        }
    }
}


@Composable
fun StatusRow(
    label: String,
    value: String,
    valueColor: Color,
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {

        Text(
            text = label,
            color = Color(0xFF607D8B),
            fontSize = 12.sp,
        )

        Text(
            text = value,
            color = valueColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
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