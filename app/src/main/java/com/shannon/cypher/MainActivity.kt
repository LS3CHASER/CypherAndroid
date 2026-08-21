package com.shannon.cypher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.shannon.cypher.ui.CypherHomeScreen
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