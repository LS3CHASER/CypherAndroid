package com.shannon.cypher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shannon.cypher.ui.theme.CypherTheme


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
    val accent = Color(0xFF8A2BE2)          // Electric purple
    val secondaryAccent = Color(0xFF76FF03) // Acid green

    val secondaryText = Color(0xFFA99AAF)

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
                text = "SYSTEM ONLINE",
                color = secondaryText,
                fontSize = 12.sp,
                letterSpacing = 3.sp,
            )

            Spacer(
                modifier = Modifier.height(56.dp)
            )

            Box(
                modifier = Modifier
                    .size(190.dp)
                    .border(
                        width = 2.dp,
                        color = accent,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {

                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .border(
                            width = 1.dp,
                            color = accent.copy(
                                alpha = 0.65f
                            ),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                color = accent.copy(
                                    alpha = 0.12f
                                ),
                                shape = CircleShape,
                            )
                            .border(
                                width = 1.dp,
                                color = accent,
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {

                        Text(
                            text = "C",
                            color = secondaryAccent,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(38.dp)
            )

            Text(
                text = "Good evening, Shannon.",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text = "Awaiting your command.",
                color = secondaryText,
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
                    .background(
                        color = accent.copy(
                            alpha = 0.12f
                        ),
                        shape = CircleShape,
                    )
                    .border(
                        width = 1.dp,
                        color = accent,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {

                Text(
                    text = "MIC",
                    color = accent,
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
    backgroundColor = 0xFF050A0E,
)
@Composable
fun CypherHomeScreenPreview() {

    CypherTheme {
        CypherHomeScreen()
    }
}