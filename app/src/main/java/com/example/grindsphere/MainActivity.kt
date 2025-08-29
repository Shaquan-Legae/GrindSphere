package com.example.grindsphere

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.grindsphere.ui.theme.GrindSphereTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GrindSphereTheme {
                // We are now showing the ExperimentSignupScreen directly
                ExperimentSignupScreen()
            }
        }
    }
}
