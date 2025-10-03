package com.example.grindsphere

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GrindSphereLogin(
                isPreview = false,
                onNavigateToSignup = {
                    startActivity(Intent(this, SignupActivity::class.java))
                }
            ) // Call the login composable
        }
    }
}
