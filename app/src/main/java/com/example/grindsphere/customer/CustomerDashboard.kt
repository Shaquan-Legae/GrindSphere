@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.grindsphere.customer

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
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.grindsphere.models.Booking
import com.example.grindsphere.models.Conversation
import com.example.grindsphere.models.Message
import com.example.grindsphere.models.Service
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import java.text.SimpleDateFormat
import java.util.*

class CustomerDashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
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
            composable("conversations") { ConversationsScreen(navController) }
            composable("profile") { CustomerProfileScreen() }
            composable("serviceDetails/{serviceId}") { backStackEntry ->
                val serviceId = backStackEntry.arguments?.getString("serviceId") ?: ""
                ServiceDetailsScreen(serviceId = serviceId, navController = navController)
            }
            composable("chat/{conversationId}") { backStackEntry ->
                val conversationId = backStackEntry.arguments?.getString("conversationId")
                if (conversationId != null) {
                    ChatScreen(conversationId = conversationId)
                } else {
                    // Handle the case where conversationId is null, maybe show an error message
                }
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
            icon = { Icon(Icons.Filled.Email, contentDescription = "Messages") },
            label = { Text("Messages") },
            selected = currentRoute == "conversations",
            onClick = { navController.navigate("conversations") }
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
    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }

    // categories shown to the user remain generic
    val categories = listOf("All", "Home Services", "Beauty", "Tech", "Education", "Other")

    val firestore = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        firestore.collection("services")
            .get()
            .addOnSuccessListener { result ->
                services = result.documents.mapNotNull { doc ->
                    doc.toObject<Service>()?.copy(id = doc.id)
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
            // Search suggestions slot left empty
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
                        service.categories.joinToString(" ").contains(searchQuery, ignoreCase = true)
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
                        // ServiceCard is not edited. It will receive the Service model.
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceDetailsScreen(
    serviceId: String,
    navController: NavHostController
) {
    var service by remember { mutableStateOf<Service?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var bookingMessage by remember { mutableStateOf("") }
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    // Fetch service from Firestore
    LaunchedEffect(serviceId) {
        firestore.collection("services")
            .document(serviceId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    service = doc.toObject<Service>()?.copy(id = doc.id)
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
            // Use banner if available, else first image, else placeholder
            val imageToShow = srv.banner.ifBlank { srv.images.firstOrNull() ?: "" }
            if (imageToShow.isNotBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(imageToShow),
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
            // Provider display only if present in document
            if (srv.ownerName.isNotBlank()) {
                Text("Provider: ${srv.ownerName}", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
            }
            // Price only show when present (> 0.0)
            if (srv.price > 0.0) {
                Text("Price: R${String.format(Locale.US, "%.2f", srv.price)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
            }
            // Rating only show when present (> 0.0)
            if (srv.rating > 0.0) {
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
                Spacer(modifier = Modifier.height(8.dp))
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val customerId = auth.currentUser?.uid ?: return@Button
                        val booking = Booking(
                            serviceId = srv.id,
                            serviceName = srv.name,
                            customerId = customerId,
                            customerName = auth.currentUser?.displayName ?: "",
                            hustlerId = srv.ownerUid,
                            hustlerName = srv.ownerName,
                            message = bookingMessage,
                            price = srv.price
                        )
                        firestore.collection("bookingRequests")
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
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Book Now")
                }
                OutlinedButton(
                    onClick = {
                        val currentUserId = auth.currentUser?.uid
                        if (currentUserId == null || srv.ownerUid.isEmpty()) return@OutlinedButton

                        // Check for existing conversation
                        firestore.collection("conversations")
                            .whereEqualTo("serviceId", srv.id)
                            .whereArrayContains("participants", currentUserId)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                if (!querySnapshot.isEmpty) {
                                    // Conversation exists
                                    val conversationId = querySnapshot.documents.first().id
                                    navController.navigate("chat/$conversationId")
                                } else {
                                    // Create new conversation
                                    val newConversation = Conversation(
                                        participants = listOf(currentUserId, srv.ownerUid),
                                        participantNames = mapOf(
                                            currentUserId to (auth.currentUser?.displayName ?: "Customer"),
                                            srv.ownerUid to srv.ownerName
                                        ),
                                        serviceId = srv.id,
                                        serviceName = srv.name,
                                        lastMessage = "Chat started..."
                                    )
                                    firestore.collection("conversations")
                                        .add(newConversation)
                                        .addOnSuccessListener { docRef ->
                                            navController.navigate("chat/${docRef.id}")
                                        }
                                }
                            }
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Message")
                }
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
    var bookings by remember { mutableStateOf<List<Booking>>(emptyList()) }
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
                    doc.toObject<Booking>()?.copy(id = doc.id)
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
fun BookingCard(booking: Booking) {
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
            booking.timestamp?.let {
                Text(
                    "Date: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(it)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
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
fun CustomerProfileScreen() {
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

@Composable
fun ServiceCard(
    service: Service,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Banner image
            if (service.banner.isNotBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(service.banner),
                    contentDescription = "Banner for ${service.name}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(Color.LightGray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "No banner",
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                // Profile pic and service info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (service.profilePicUrl.isNotBlank()) {
                        Image(
                            painter = rememberAsyncImagePainter(service.profilePicUrl),
                            contentDescription = "Provider profile picture",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(50)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.Gray.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "No profile pic",
                                tint = Color.DarkGray,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            service.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (service.ownerName.isNotBlank()) {
                            Text(
                                text = "by ${service.ownerName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Short description
                Text(
                    text = service.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Location, price, rating, bookings
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        if (service.location.isNotBlank()) {
                            Text(
                                "📍 ${service.location}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (service.price > 0.0) {
                            Text(
                                "R${String.format(Locale.US, "%.2f", service.price)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        if (service.rating > 0.0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Rating",
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    String.format(Locale.US, "%.1f", service.rating),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        if (service.bookings > 0) {
                            Text(
                                "${service.bookings} bookings",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}


// Previews updated to match new Service model
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
        banner = "",
        profilePicUrl = "",
        ownerUid = "123",
        ownerName = "John Doe",
        categories = listOf("Tutoring"),
        bookings = 2L,
        views = 10L,
        category = "Home Services",
        price = 49.99,
        rating = 4.5
    )
    ServiceCard(service = sampleService, onClick = {})
}



@Preview(showBackground = true)
@Composable
fun BookingCardPreview() {
    val sampleBooking = Booking(
        id = "1",
        serviceId = "123",
        serviceName = "Sample Service",
        customerId = "456",
        customerName = "Customer Name",
        hustlerId = "789",
        hustlerName = "Provider Name",
        status = "pending",
        timestamp = Date(),
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

@Composable
fun ConversationsScreen(navController: NavHostController) {
    var conversations by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        val currentUserId = auth.currentUser?.uid ?: return@LaunchedEffect
        firestore.collection("conversations")
            .whereArrayContains("participants", currentUserId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    isLoading = false
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    conversations = snapshots.documents.mapNotNull { doc ->
                        doc.toObject<Conversation>()?.copy(id = doc.id)
                    }
                }
                isLoading = false
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Messages", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (conversations.isEmpty()) {
            Text("No conversations yet.", modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(conversations, key = { it.id }) { conversation ->
                    ConversationCard(conversation = conversation, navController = navController)
                }
            }
        }
    }
}

@Composable
fun ConversationCard(conversation: Conversation, navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid
    val otherParticipantId = conversation.participants.find { it != currentUserId }
    // Use the actual name from participantNames map
    val otherParticipantName = conversation.participantNames[otherParticipantId] ?: "Unknown"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate("chat/${conversation.id}") },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(otherParticipantName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(conversation.serviceName, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                conversation.lastMessage,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
        }
    }
}


@Composable
fun ChatScreen(conversationId: String) {
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var messageText by remember { mutableStateOf("") } // This is the correct state variable
    var isLoading by remember { mutableStateOf(true) }
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid

    // Function to send a message
    fun onSendMessage(text: String) {
        if (text.isBlank() || currentUserId == null) return

        val message = Message(
            senderId = currentUserId,
            text = text,
            timestamp = Timestamp.now()
        )

        val conversationRef = firestore.collection("conversations").document(conversationId)

        // Add message to the messages sub-collection
        conversationRef.collection("messages").add(message)

        // Update the last message in the parent conversation document
        conversationRef.update(
            "lastMessage", text,
            "lastMessageTimestamp", FieldValue.serverTimestamp()
        )
    }

    // Fetch messages
    LaunchedEffect(conversationId) {
        val messagesListener = firestore.collection("conversations").document(conversationId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    isLoading = false
                    return@addSnapshotListener
                }
                messages = snapshot?.documents?.mapNotNull { it.toObject<Message>() } ?: emptyList()
                isLoading = false
            }
        // Remember to remove the listener when the composable leaves the screen (not shown here but good practice)
    }

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Type a message...") },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    if (messageText.isNotBlank()) {
                        // FIX: Use the 'messageText' variable here
                        onSendMessage(messageText)
                        // Also, clear the input field after sending
                        messageText = ""
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Message")
                }
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 8.dp),
                reverseLayout = true // Show latest messages at the bottom
            ) {
                items(messages.reversed()) { message ->
                    // Basic message bubble layout
                    val isSentByCurrentUser = message.senderId == currentUserId
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = if (isSentByCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Card(
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .widthIn(max = 300.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSentByCurrentUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(text = message.text)
                                Text(
                                    text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(message.timestamp.toDate()),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message, isFromCurrentUser: Boolean) {
    val bubbleColor = if (isFromCurrentUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.LightGray.copy(alpha = 0.3f)
    val alignment = if (isFromCurrentUser) Arrangement.End else Arrangement.Start

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = alignment
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(bubbleColor)
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(message.timestamp.toDate()),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
    }
}