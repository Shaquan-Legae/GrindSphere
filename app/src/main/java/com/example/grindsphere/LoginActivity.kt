package com.example.grindsphere

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GrindSphereLogin(
                isPreview = TODO(),
                onNavigateToSignup = TODO()
            ) // Call the login composable
        }
    }
}
