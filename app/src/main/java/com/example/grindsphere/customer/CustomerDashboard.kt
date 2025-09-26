@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.grindsphere.customer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import java.util.*
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.ui.tooling.preview.Preview

// Data classes
data class Service(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val location: String = "",
    val images: List<String> = listOf(),
    val ownerUid: String = "",
    val ownerName: String = "",
    val category: String = "",
    val price: Double = 0.0,
    val rating: Double = 0.0
)

data class BookingRequest(
    val id: String = "",
    val serviceId: String = "",
    val serviceName: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val hustlerId: String = "",
    val hustlerName: String = "",
    val status: String = "pending",
    val date: Date = Date(),
    val message: String = "",
    val price: Double = 0.0
)

class CustomerDashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CustomerDashboardScreen()
        }
    }
}



@Composable
fun CustomerBottomNavigation(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "services"

    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = "Services") },
            label = { Text("Services") },
            selected = currentRoute == "services",
            onClick = { navController.navigate("services") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = "Bookings") },
            label = { Text("Bookings") },
            selected = currentRoute == "bookings",
            onClick = { navController.navigate("bookings") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
            label = { Text("Profile") },
            selected = currentRoute == "profile",
            onClick = { navController.navigate("profile") }
        )
    }
}






/*@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentItemsAndSuggestions() {
    val context = LocalContext.current

    // Suggestions list
    val suggestions = listOf("Snap My Grad", "ClawsBySihle", "DJ T-HYPE", "Hairtyslists")

    // Load recent items from SharedPreferences
    val sharedPref = context.getSharedPreferences("recent_items", Context.MODE_PRIVATE)
    val recentItemsInitial = sharedPref.getStringSet("items", emptySet())?.toList() ?: emptyList()
    var recentItems by remember { mutableStateOf(recentItemsInitial) }

    var text by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val dropdownItems = (recentItems + suggestions).distinct()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Recently visited items row
        if (recentItems.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recentItems.forEach { item ->
                    Button(onClick = { text = item }) {
                        Text(item)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // suggestions
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            TextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Search items") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.textFieldColors()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                dropdownItems.filter { it.contains(text, ignoreCase = true) || text.isEmpty() }
                    .forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                text = selectionOption
                                expanded = false

                                // Saves to recent items
                                val updatedSet = (recentItems + selectionOption).toSet()
                                recentItems = updatedSet.toList()
                                sharedPref.edit().putStringSet("items", updatedSet).apply()
                            }
                        )
                    }
            }
        }
    }
}
*
 */

// Preview functions
@Preview(showBackground = true)
@Composable
fun ServicesScreenPreview() {
    ServicesScreen(navController = rememberNavController())
}

@Preview(showBackground = true)
@Composable
fun ServiceCardPreview() {
    val sampleService = Service(
        id = "1",
        name = "Sample Service",
        description = "This is a sample service description that might be a bit longer to test text truncation",
        location = "New York, NY",
        images = listOf(),
        ownerUid = "123",
        ownerName = "John Doe",
        category = "Home Services",
        price = 49.99,
        rating = 4.5
    )

}

@Preview(showBackground = true)
@Composable
fun BookingCardPreview() {
    val sampleBooking = BookingRequest(
        id = "1",
        serviceId = "123",
        serviceName = "Sample Service",
        customerId = "456",
        customerName = "Customer Name",
        hustlerId = "789",
        hustlerName = "Provider Name",
        status = "pending",
        date = Date(),
        message = "Please arrive by 2 PM",
        price = 49.99
    )
    BookingCard(booking = sampleBooking)
}

@Preview(showBackground = true)
@Composable
fun StatusBadgePreview() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusBadge(status = "pending")
        StatusBadge(status = "accepted")
        StatusBadge(status = "rejected")
        StatusBadge(status = "completed")
        StatusBadge(status = "unknown")
    }
}
