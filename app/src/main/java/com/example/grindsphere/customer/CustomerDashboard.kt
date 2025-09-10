package com.example.grindsphere.customer

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.ui.tooling.preview.Preview
import java.text.SimpleDateFormat

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(navController: NavHostController) {
    var services by remember { mutableStateOf<List<Service>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Home Services", "Beauty", "Tech", "Education", "Other")
    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }

    val firestore = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        firestore.collection("services")
            .get()
            .addOnSuccessListener { result ->
                services = result.documents.mapNotNull { doc ->
                    val imagesFromFirestore = doc.get("images") as? List<String> ?: emptyList()

                    Service(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        location = doc.getString("location") ?: "",
                        images = imagesFromFirestore,
                        ownerUid = doc.getString("ownerUid") ?: "",
                        ownerName = doc.getString("ownerName") ?: "",
                        category = doc.getString("category") ?: "Other",
                        price = doc.getDouble("price") ?: 0.0,
                        rating = doc.getDouble("rating") ?: 0.0
                    )
                }
                isLoading = false
            }
            .addOnFailureListener { isLoading = false }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onSearch = { searchActive = false },
            active = searchActive,
            onActiveChange = { searchActive = it },
            placeholder = { Text("Search services") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
            trailingIcon = {
                if (searchActive && searchQuery.isNotEmpty()) {
                    Icon(
                        modifier = Modifier.clickable {
                            if (searchQuery.isNotEmpty()) {
                                searchQuery = ""
                            } else {
                                searchActive = false
                            }
                        },
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear Search"
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (searchActive) 0.dp else 16.dp)
                .padding(top = 16.dp, bottom = if (searchActive) 0.dp else 16.dp)
        ) {
            // Search suggestions (can be empty)
        }

        if (!searchActive) {
            Text(
                text = "Categories",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) }
                    )
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val servicesToDisplay = services.filter { service ->
                val categoryMatch = selectedCategory == "All" || service.category == selectedCategory
                val searchMatch = searchQuery.isBlank() ||
                        service.name.contains(searchQuery, ignoreCase = true) ||
                        service.description.contains(searchQuery, ignoreCase = true) ||
                        service.location.contains(searchQuery, ignoreCase = true) ||
                        service.category.contains(searchQuery, ignoreCase = true)
                categoryMatch && searchMatch
            }

            if (servicesToDisplay.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (searchQuery.isNotBlank())
                            "No services found for '$searchQuery'."
                        else "No services found in this category."
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(all = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(servicesToDisplay, key = { it.id }) { service ->
                        ServiceCard(
                            service = service,
                            onClick = { navController.navigate("serviceDetails/${service.id}") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceCard(service: Service, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            if (service.images.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(service.images.first()),
                    contentDescription = "Service image for ${service.name}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.LightGray.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "No image available",
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = service.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = service.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "R${String.format(Locale.US, "%.2f", service.price)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f", service.rating),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = service.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { /* Navigate to service details */ },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(" Tap to View Details")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceDetailsScreen(
    serviceId: String,
    navController: NavHostController
) {
    var service by remember { mutableStateOf<Service?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var bookingMessage by remember { mutableStateOf("") }
    var bookingDate by remember { mutableStateOf(Date()) }
    val context = LocalContext.current

    // Fetch service from Firestore
    LaunchedEffect(serviceId) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("services")
            .document(serviceId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    service = Service(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        location = doc.getString("location") ?: "",
                        images = doc.get("images") as? List<String> ?: listOf(),
                        ownerUid = doc.getString("ownerUid") ?: "",
                        ownerName = doc.getString("ownerName") ?: "",
                        category = doc.getString("category") ?: "Other",
                        price = doc.getDouble("price") ?: 0.0,
                        rating = doc.getDouble("rating") ?: 0.0
                    )
                }
                isLoading = false
            }
            .addOnFailureListener { isLoading = false }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    service?.let { srv ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)) {
            if (srv.images.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(srv.images.first()),
                    contentDescription = "Service image for ${srv.name}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.LightGray.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "No image available",
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Text(srv.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(srv.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Category: ${srv.category}", style = MaterialTheme.typography.bodySmall)
            Text("Location: ${srv.location}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Provider: ${srv.ownerName}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Price: R${String.format(Locale.US, "%.2f", srv.price)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Rating",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(String.format(Locale.US, "%.1f", srv.rating), style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Booking Form
            OutlinedTextField(
                value = bookingMessage,
                onValueChange = { bookingMessage = it },
                label = { Text("Message to provider") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val auth = FirebaseAuth.getInstance()
                    val customerId = auth.currentUser?.uid ?: return@Button
                    val booking = hashMapOf(
                        "serviceId" to srv.id,
                        "serviceName" to srv.name,
                        "customerId" to customerId,
                        "customerName" to (auth.currentUser?.displayName ?: ""),
                        "hustlerId" to srv.ownerUid,
                        "hustlerName" to srv.ownerName,
                        "status" to "pending",
                        "date" to bookingDate,
                        "message" to bookingMessage,
                        "price" to srv.price
                    )
                    FirebaseFirestore.getInstance()
                        .collection("bookingRequests")
                        .add(booking)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Booking requested!", Toast.LENGTH_SHORT).show()
                            navController.navigate("bookings")
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed to book.", Toast.LENGTH_SHORT).show()
                        }
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Book Now")
            }
        }
    } ?: Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Service not found.")
    }
}

@Composable
fun BookingsScreen() {
    var bookings by remember { mutableStateOf<List<BookingRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    LaunchedEffect(Unit) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            isLoading = false; return@LaunchedEffect
        }
        firestore.collection("bookingRequests")
            .whereEqualTo("customerId", currentUserId)
            .get()
            .addOnSuccessListener { result ->
                bookings = result.documents.mapNotNull { doc ->
                    BookingRequest(
                        id = doc.id,
                        serviceId = doc.getString("serviceId") ?: "",
                        serviceName = doc.getString("serviceName") ?: "",
                        customerId = doc.getString("customerId") ?: "",
                        customerName = doc.getString("customerName") ?: "",
                        hustlerId = doc.getString("hustlerId") ?: "",
                        hustlerName = doc.getString("hustlerName") ?: "",
                        status = doc.getString("status") ?: "pending",
                        date = doc.getDate("date") ?: Date(),
                        message = doc.getString("message") ?: "",
                        price = doc.getDouble("price") ?: 0.0
                    )
                }
                isLoading = false
            }
            .addOnFailureListener { isLoading = false }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("My Bookings", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (bookings.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("You have no bookings yet.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(bookings, key = { it.id }) { booking ->
                    BookingCard(booking = booking)
                }
            }
        }
    }
}

@Composable
fun BookingCard(booking: BookingRequest) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(booking.serviceName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                StatusBadge(status = booking.status)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Provider: ${booking.hustlerName}", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Date: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(booking.date)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text("Price: R${String.format(Locale.US, "%.2f", booking.price)}", style = MaterialTheme.typography.bodyMedium)
            if (booking.message.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Message: ${booking.message}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (backgroundColor, textColor) = when (status.lowercase(Locale.ROOT)) {
        "accepted" -> Color.Green.copy(alpha = 0.2f) to Color.Green.copy(alpha = 0.9f)
        "rejected" -> Color.Red.copy(alpha = 0.2f) to Color.Red.copy(alpha = 0.9f)
        "completed" -> Color.Blue.copy(alpha = 0.2f) to Color.Blue.copy(alpha = 0.9f)
        "pending" -> Color.Yellow.copy(alpha = 0.3f) to Color(0xFFB8860B)
        else -> Color.Gray.copy(alpha = 0.2f) to Color.DarkGray
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun CustomerProfileScreen(navController: NavHostController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Customer Profile", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                FirebaseAuth.getInstance().signOut()
                // Navigate to login screen
            }) {
                Text("Sign Out")
            }
        }
    }
}

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
    ServiceCard(service = sampleService, onClick = {})
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