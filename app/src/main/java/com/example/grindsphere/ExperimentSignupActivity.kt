package com.example.grindsphere

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class ExperimentSignupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExperimentSignupScreen() // loads your Composable
        }
    }
}