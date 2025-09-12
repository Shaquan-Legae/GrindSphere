package com.example.grindsphere.hustler

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.grindsphere.ui.theme.GrindSphereTheme


class HomePageClass : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContent {
                GrindSphereTheme {
                    HomePageScreen(
                        onNavigateToSearch = {
                            // Navigate to HustlerDashboard with search open
                            val intent = Intent(this, HustlerDashboardActivity::class.java)
                            intent.putExtra("openSearch", true)
                            startActivity(intent)
                        },
                        viewModel = viewModel()
                    )
            }
        }
    }

}