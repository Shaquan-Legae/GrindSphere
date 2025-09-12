package com.example.grindsphere.hustler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.grindsphere.ui.theme.GrindSphereTheme

class HustlerDashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GrindSphereTheme {
                HustlerDashboard(viewModel = viewModel())
            }
        }
    }
}
