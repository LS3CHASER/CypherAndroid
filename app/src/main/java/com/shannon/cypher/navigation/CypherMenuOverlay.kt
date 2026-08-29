package com.shannon.cypher.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shannon.cypher.navigation.CypherScreen


@Composable
fun CypherMenuOverlay(
    currentScreen: CypherScreen,
    onScreenSelected: (CypherScreen) -> Unit,
    onDismiss: () -> Unit,
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


    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Color.Black.copy(
                        alpha =
                            0.55f
                    )
                )
                .clickable {
                    onDismiss()
                },
    ) {


        /*
         * Left-side navigation panel.
         */
        Column(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(
                        0.76f
                    )
                    .background(
                        color =
                            panel,

                        shape =
                            RoundedCornerShape(
                                topEnd =
                                    24.dp,

                                bottomEnd =
                                    24.dp,
                            ),
                    )
                    .clickable(
                        onClick = {
                            /*
                             * Consume taps inside the menu
                             * so they do not close it.
                             */
                        }
                    )
                    .padding(
                        horizontal =
                            22.dp,

                        vertical =
                            32.dp,
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


                Text(
                    text =
                        "C Y P H E R",

                    color =
                        accent,

                    fontSize =
                        20.sp,

                    fontWeight =
                        FontWeight.Medium,

                    letterSpacing =
                        4.sp,
                )


                Box(
                    modifier =
                        Modifier
                            .size(
                                44.dp
                            )
                            .clickable {
                                onDismiss()
                            },

                    contentAlignment =
                        Alignment.Center,
                ) {

                    Text(
                        text =
                            "×",

                        color =
                            secondaryText,

                        fontSize =
                            30.sp,
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.size(
                        36.dp
                    )
            )


            MenuItem(
                title =
                    "HOME",

                selected =
                    currentScreen ==
                            CypherScreen.HOME,

                accent =
                    accent,

                selectedAccent =
                    secondaryAccent,

                secondaryText =
                    secondaryText,

                onClick = {

                    onScreenSelected(
                        CypherScreen.HOME
                    )

                    onDismiss()
                },
            )


            Spacer(
                modifier =
                    Modifier.size(
                        12.dp
                    )
            )


            MenuItem(
                title =
                    "TO-DO LIST",

                selected =
                    currentScreen ==
                            CypherScreen.TASKS,

                accent =
                    accent,

                selectedAccent =
                    secondaryAccent,

                secondaryText =
                    secondaryText,

                onClick = {

                    onScreenSelected(
                        CypherScreen.TASKS
                    )

                    onDismiss()
                },
            )


            Spacer(
                modifier =
                    Modifier.size(
                        12.dp
                    )
            )


            MenuItem(
                title =
                    "VOICE LAB",

                selected =
                    currentScreen ==
                            CypherScreen.VOICE_LAB,

                accent =
                    accent,

                selectedAccent =
                    secondaryAccent,

                secondaryText =
                    secondaryText,

                onClick = {

                    onScreenSelected(
                        CypherScreen.VOICE_LAB
                    )

                    onDismiss()
                },
            )


            Spacer(
                modifier =
                    Modifier.weight(
                        1f
                    )
            )


            Text(
                text =
                    "More Cypher modules will appear here as they are added.",

                color =
                    secondaryText,

                fontSize =
                    12.sp,

                lineHeight =
                    18.sp,
            )
        }
    }
}


@Composable
private fun MenuItem(
    title: String,
    selected: Boolean,
    accent: Color,
    selectedAccent: Color,
    secondaryText: Color,
    onClick: () -> Unit,
) {

    val itemBackground =
        if (
            selected
        ) {

            accent.copy(
                alpha =
                    0.16f
            )

        } else {

            Color.Transparent
        }


    val textColor =
        if (
            selected
        ) {

            selectedAccent

        } else {

            secondaryText
        }


    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color =
                        itemBackground,

                    shape =
                        RoundedCornerShape(
                            14.dp
                        ),
                )
                .clickable {
                    onClick()
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
                if (
                    selected
                ) {
                    "●"
                } else {
                    "○"
                },

            color =
                if (
                    selected
                ) {
                    selectedAccent
                } else {
                    accent
                },

            fontSize =
                11.sp,
        )


        Spacer(
            modifier =
                Modifier.size(
                    14.dp
                )
        )


        Text(
            text =
                title,

            color =
                textColor,

            fontSize =
                15.sp,

            fontWeight =
                if (
                    selected
                ) {
                    FontWeight.Bold
                } else {
                    FontWeight.Medium
                },

            letterSpacing =
                2.sp,
        )
    }
}