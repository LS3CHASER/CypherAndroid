package com.shannon.cypher.ui.weather

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.shannon.cypher.R
import com.shannon.cypher.weather.CypherWeatherDay
import com.shannon.cypher.weather.CypherWeatherResult
import com.shannon.cypher.weather.CypherWeatherService
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch


private const val BOM_RADAR_URL =
    "https://www.bom.gov.au/weather-and-climate/rain-radar-and-weather-maps"


@Composable
fun CypherWeatherScreen(
    weatherService: CypherWeatherService,
    isListening: Boolean,
    isThinking: Boolean,
    isSpeaking: Boolean,
    onMenuClick: () -> Unit,
    onMicClick: () -> Unit,
) {

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val background = Color(0xFF070509)
    val panel = Color(0xFF110D16)
    val accent = Color(0xFF8A2BE2)
    val secondaryAccent = Color(0xFF76FF03)
    val primaryText = Color.White
    val secondaryText = Color(0xFFA99AAF)

    val compactRingColor =
        if (isListening) {
            secondaryAccent
        } else {
            accent
        }

    val infiniteTransition =
        rememberInfiniteTransition(
            label = "WeatherScreenCypherAnimation"
        )

    val ringRotation by
    infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis =
                            when {
                                isThinking -> 1600
                                isListening -> 2400
                                isSpeaking -> 3500
                                else -> 7000
                            },
                        easing = FastOutSlowInEasing,
                    ),
                repeatMode = RepeatMode.Restart,
            ),
        label = "WeatherScreenRingRotation",
    )

    var weatherResult by
    remember {
        mutableStateOf<CypherWeatherResult?>(null)
    }

    var isLoading by
    remember {
        mutableStateOf(true)
    }

    var errorMessage by
    remember {
        mutableStateOf<String?>(null)
    }

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED
    }

    fun refreshWeather() {

        if (!hasLocationPermission()) {
            isLoading = false
            errorMessage =
                "Location permission is required to load your local weather."
            return
        }

        isLoading = true
        errorMessage = null

        coroutineScope.launch {
            try {
                weatherResult =
                    weatherService.getCurrentWeatherResult()
                errorMessage = null
            } catch (_: SecurityException) {
                errorMessage =
                    "Location permission is required to load your local weather."
            } catch (_: Exception) {
                errorMessage =
                    "Cypher couldn't retrieve the weather right now."
            } finally {
                isLoading = false
            }
        }
    }

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->

            val granted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (granted) {
                refreshWeather()
            } else {
                isLoading = false
                errorMessage =
                    "Location permission is required to load your local weather."
            }
        }

    LaunchedEffect(Unit) {
        refreshWeather()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = background,
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(
                        horizontal = 20.dp,
                        vertical = 12.dp,
                    ),
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(52.dp)
                            .clickable {
                                onMenuClick()
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "☰",
                        color = accent,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Light,
                    )
                }

                Box(
                    modifier =
                        Modifier
                            .size(76.dp)
                            .clickable {
                                if (!isThinking) {
                                    onMicClick()
                                }
                            },
                    contentAlignment = Alignment.Center,
                ) {

                    Canvas(
                        modifier =
                            Modifier
                                .size(70.dp)
                                .rotate(ringRotation),
                    ) {

                        drawArc(
                            color = compactRingColor,
                            startAngle = -90f,
                            sweepAngle = 85f,
                            useCenter = false,
                            style =
                                Stroke(
                                    width = 2.4.dp.toPx(),
                                    cap = StrokeCap.Round,
                                ),
                        )

                        drawArc(
                            color = compactRingColor,
                            startAngle = 45f,
                            sweepAngle = 55f,
                            useCenter = false,
                            style =
                                Stroke(
                                    width = 2.4.dp.toPx(),
                                    cap = StrokeCap.Round,
                                ),
                        )

                        drawArc(
                            color = compactRingColor,
                            startAngle = 150f,
                            sweepAngle = 120f,
                            useCenter = false,
                            style =
                                Stroke(
                                    width = 2.4.dp.toPx(),
                                    cap = StrokeCap.Round,
                                ),
                        )
                    }

                    Image(
                        painter = painterResource(
                            id = R.drawable.cypher_head
                        ),
                        contentDescription = "Cypher microphone",
                        modifier = Modifier.size(46.dp),
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "WEATHER",
                color = primaryText,
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
            )

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(
                            rememberScrollState()
                        ),
            ) {

                when {
                    isLoading -> {
                        WeatherMessageCard(
                            text = "Retrieving local weather...",
                            panel = panel,
                            accent = accent,
                            textColor = secondaryText,
                        )
                    }

                    errorMessage != null -> {
                        WeatherMessageCard(
                            text = errorMessage.orEmpty(),
                            panel = panel,
                            accent = accent,
                            textColor = secondaryText,
                        )

                        Spacer(
                            modifier = Modifier.height(14.dp)
                        )

                        WeatherActionButton(
                            label =
                                if (hasLocationPermission()) {
                                    "TRY AGAIN"
                                } else {
                                    "ENABLE LOCATION"
                                },
                            panel = panel,
                            accent = accent,
                            secondaryAccent = secondaryAccent,
                            onClick = {
                                if (hasLocationPermission()) {
                                    refreshWeather()
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION,
                                        )
                                    )
                                }
                            },
                        )
                    }

                    weatherResult != null -> {
                        val result = weatherResult!!

                        CurrentWeatherCard(
                            result = result,
                            panel = panel,
                            accent = accent,
                            secondaryAccent = secondaryAccent,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                        )

                        Spacer(
                            modifier = Modifier.height(14.dp)
                        )

                        WeatherActionButton(
                            label = "RAIN RADAR",
                            panel = panel,
                            accent = accent,
                            secondaryAccent = secondaryAccent,
                            onClick = {
                                val intent =
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(BOM_RADAR_URL),
                                    )
                                context.startActivity(intent)
                            },
                        )

                        Spacer(
                            modifier = Modifier.height(10.dp)
                        )

                        WeatherActionButton(
                            label = "REFRESH WEATHER",
                            panel = panel,
                            accent = accent,
                            secondaryAccent = secondaryAccent,
                            onClick = {
                                refreshWeather()
                            },
                        )

                        Spacer(
                            modifier = Modifier.height(26.dp)
                        )

                        Text(
                            text = "FORECAST",
                            color = accent,
                            fontSize = 12.sp,
                            letterSpacing = 2.sp,
                        )

                        Spacer(
                            modifier = Modifier.height(10.dp)
                        )

                        result.daily.forEachIndexed { index, day ->
                            ForecastDayCard(
                                index = index,
                                day = day,
                                panel = panel,
                                accent = accent,
                                secondaryAccent = secondaryAccent,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                            )

                            Spacer(
                                modifier = Modifier.height(10.dp)
                            )
                        }

                        Spacer(
                            modifier = Modifier.height(20.dp)
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun CurrentWeatherCard(
    result: CypherWeatherResult,
    panel: Color,
    accent: Color,
    secondaryAccent: Color,
    primaryText: Color,
    secondaryText: Color,
) {

    val currentTemperature =
        result.currentTemperatureC?.roundToInt()

    val feelsLike =
        result.apparentTemperatureC?.roundToInt()

    val today =
        result.daily.firstOrNull()

    val condition =
        weatherCondition(
            result.currentWeatherCode
        )

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color = panel,
                    shape = RoundedCornerShape(18.dp),
                )
                .border(
                    width = 1.dp,
                    color = accent.copy(alpha = 0.30f),
                    shape = RoundedCornerShape(18.dp),
                )
                .padding(20.dp),
    ) {

        Text(
            text = result.locationName,
            color = secondaryAccent,
            fontSize = 30.sp,
            letterSpacing = 1.sp,
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Text(
                text =
                    currentTemperature
                        ?.let { "$it°" }
                        ?: "--°",
                color = primaryText,
                fontSize = 64.sp,
                fontWeight = FontWeight.Light,
            )

            Text(
                text = weatherSymbol(result.currentWeatherCode),
                color = secondaryAccent,
                fontSize = 44.sp,
                textAlign = TextAlign.End,
            )
        }

        Text(
            text = condition,
            color = primaryText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
        )

        if (feelsLike != null) {
            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "Feels like $feelsLike°",
                color = secondaryText,
                fontSize = 14.sp,
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {

            WeatherMetric(
                label = "RAIN",
                value =
                    result.currentPrecipitationProbability
                        ?.let { "$it%" }
                        ?: today
                            ?.precipitationProbability
                            ?.let { "$it%" }
                        ?: "--",
                accent = accent,
                primaryText = primaryText,
            )

            WeatherMetric(
                label = "HIGH",
                value =
                    today
                        ?.maxTemperatureC
                        ?.roundToInt()
                        ?.let { "$it°" }
                        ?: "--",
                accent = accent,
                primaryText = primaryText,
            )

            WeatherMetric(
                label = "LOW",
                value =
                    today
                        ?.minTemperatureC
                        ?.roundToInt()
                        ?.let { "$it°" }
                        ?: "--",
                accent = accent,
                primaryText = primaryText,
            )
        }
    }
}


@Composable
private fun WeatherMetric(
    label: String,
    value: String,
    accent: Color,
    primaryText: Color,
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            color = accent,
            fontSize = 11.sp,
            letterSpacing = 1.5.sp,
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Text(
            text = value,
            color = primaryText,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}


@Composable
private fun ForecastDayCard(
    index: Int,
    day: CypherWeatherDay,
    panel: Color,
    accent: Color,
    secondaryAccent: Color,
    primaryText: Color,
    secondaryText: Color,
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color = panel,
                    shape = RoundedCornerShape(14.dp),
                )
                .border(
                    width = 1.dp,
                    color = accent.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(14.dp),
                )
                .padding(
                    horizontal = 14.dp,
                    vertical = 13.dp,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = forecastDayLabel(index, day.date),
                color = primaryText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(
                modifier = Modifier.height(3.dp)
            )

            Text(
                text = weatherCondition(day.weatherCode),
                color = secondaryText,
                fontSize = 12.sp,
            )
        }

        Text(
            text = weatherSymbol(day.weatherCode),
            color = secondaryAccent,
            fontSize = 24.sp,
        )

        Spacer(
            modifier = Modifier.size(14.dp)
        )

        Text(
            text =
                day.precipitationProbability
                    ?.let { "$it%" }
                    ?: "--",
            color = secondaryAccent,
            fontSize = 13.sp,
            modifier =
                Modifier.size(
                    width = 44.dp,
                    height = 24.dp,
                ),
            textAlign = TextAlign.End,
        )

        Spacer(
            modifier = Modifier.size(10.dp)
        )

        Text(
            text =
                buildString {
                    append(
                        day.maxTemperatureC
                            ?.roundToInt()
                            ?.let { "$it°" }
                            ?: "--"
                    )
                    append(" / ")
                    append(
                        day.minTemperatureC
                            ?.roundToInt()
                            ?.let { "$it°" }
                            ?: "--"
                    )
                },
            color = primaryText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}


@Composable
private fun WeatherActionButton(
    label: String,
    panel: Color,
    accent: Color,
    secondaryAccent: Color,
    onClick: () -> Unit,
) {

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color = panel,
                    shape = RoundedCornerShape(14.dp),
                )
                .border(
                    width = 1.dp,
                    color = accent.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(14.dp),
                )
                .clickable {
                    onClick()
                }
                .padding(
                    vertical = 15.dp
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = secondaryAccent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.8.sp,
        )
    }
}


@Composable
private fun WeatherMessageCard(
    text: String,
    panel: Color,
    accent: Color,
    textColor: Color,
) {

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color = panel,
                    shape = RoundedCornerShape(16.dp),
                )
                .border(
                    width = 1.dp,
                    color = accent.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(20.dp),
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 14.sp,
        )
    }
}


private fun weatherCondition(
    code: Int?,
): String {

    return when (code) {
        0 -> "Clear skies"
        1 -> "Mostly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Foggy"
        51, 53, 55 -> "Drizzle"
        56, 57 -> "Freezing drizzle"
        61, 63, 65 -> "Rain"
        66, 67 -> "Freezing rain"
        71, 73, 75, 77 -> "Snow"
        80, 81, 82 -> "Rain showers"
        85, 86 -> "Snow showers"
        95 -> "Thunderstorms"
        96, 99 -> "Thunderstorms with hail"
        else -> "Conditions unavailable"
    }
}


private fun weatherSymbol(
    code: Int?,
): String {

    return when (code) {
        0, 1 -> "☀"
        2, 3 -> "☁"
        45, 48 -> "≋"
        51, 53, 55, 56, 57,
        61, 63, 65, 66, 67,
        80, 81, 82 -> "☂"
        71, 73, 75, 77, 85, 86 -> "❄"
        95, 96, 99 -> "⚡"
        else -> "•"
    }
}


private fun forecastDayLabel(
    index: Int,
    dateText: String,
): String {

    if (index == 0) {
        return "TODAY"
    }

    if (index == 1) {
        return "TOMORROW"
    }

    return try {
        val parser =
            SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.US,
            )

        val formatter =
            SimpleDateFormat(
                "EEE",
                Locale.US,
            )

        val parsed =
            parser.parse(dateText)

        if (parsed != null) {
            formatter
                .format(parsed)
                .uppercase(Locale.US)
        } else {
            dateText
        }
    } catch (_: Exception) {
        dateText
    }
}
