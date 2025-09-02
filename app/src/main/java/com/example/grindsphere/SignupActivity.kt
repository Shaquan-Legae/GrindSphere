package com.example.grindsphere

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.grindsphere.ui.theme.GrindSphereTheme

class SignupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GrindSphereTheme {
                ExperimentSignupScreen()
            }
        }
    }
}
