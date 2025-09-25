package com.example.grindsphere.customer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.grindsphere.ui.theme.GrindSphereTheme

class CustomerDashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GrindSphereTheme {
                CustomerDashboard()
            }
        }
    }
}
