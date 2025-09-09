package com.example.grindsphere.hustler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.grindsphere.ui.theme.GrindSphereTheme


class HomePageClass : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContent {
                GrindSphereTheme {
                    HomePageScreen()
            }
        }
    }

}