package com.example.grindsphere.customer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CustomerDashboardScreen() {
        val navController = rememberNavController()

        Scaffold(
            bottomBar = { CustomerBottomNavigation(navController) }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "services",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                composable("services") { ServicesScreen(navController) }
                composable("bookings") { BookingsScreen() }
                composable("profile") { CustomerProfileScreen(navController) }
                composable("serviceDetails/{serviceId}") { backStackEntry ->
                    val serviceId = backStackEntry.arguments?.getString("serviceId") ?: ""
                    ServiceDetailsScreen(serviceId = serviceId, navController = navController)
                }
            }
        }
    }
