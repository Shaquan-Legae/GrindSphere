@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.grindsphere.customer

import android.content.Intent
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.grindsphere.LoginActivity
import com.example.grindsphere.models.Booking
import com.example.grindsphere.models.Conversation
import com.example.grindsphere.models.Message
import com.example.grindsphere.models.Service
import com.example.grindsphere.models.Review
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF0D324D), Color(0xFF7F5A83))))
        ) {
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
                        // Handle the case where conversationId is null
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Error: No conversation ID")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerBottomNavigation(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "services"

    NavigationBar(
        containerColor = Color.White
    ) {
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

    // Use the same categories as Hustler dashboard
    val predefinedCategories = listOf(
        "All", "Tutoring", "Design", "Tech Support", "Photography",
        "Fashion", "Food", "Music", "Fitness", "Transport", "Nails",
        "Hair", "Beauty", "Cake", "DJ", "Home Services", "Education"
    )

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0D324D), Color(0xFF7F5A83))))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Discover Services",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    "Find amazing services near you",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        // Search bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name or category...", color = Color.Gray) },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF7F5A83))
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF7F5A83))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
            )
        }

        // Categories
        Text(
            text = "Categories",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            items(predefinedCategories) { category ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (selectedCategory == category) Color(0xFFFFD700) else Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.clickable {
                        selectedCategory = category
                    }
                ) {
                    Text(
                        text = category,
                        color = if (selectedCategory == category) Color.Black else Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Clear filters button
        if (searchQuery.isNotBlank() || selectedCategory != "All") {
            Text(
                text = "Clear Filters",
                color = Color(0xFFFFD700),
                modifier = Modifier
                    .clickable {
                        searchQuery = ""
                        selectedCategory = "All"
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .align(Alignment.End)
            )
        }

        // Services list header
        Text(
            text = when {
                searchQuery.isNotBlank() && selectedCategory != "All" -> "Results for '$searchQuery' in $selectedCategory"
                searchQuery.isNotBlank() -> "Results for '$searchQuery'"
                selectedCategory != "All" -> "Results in $selectedCategory"
                else -> "All Services"
            },
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            val servicesToDisplay = services.filter { service ->
                val categoryMatch = selectedCategory == "All" ||
                        service.categories.any { it.equals(selectedCategory, ignoreCase = true) } ||
                        service.category.equals(selectedCategory, ignoreCase = true)

                val searchMatch = searchQuery.isBlank() ||
                        service.name.contains(searchQuery, ignoreCase = true) ||
                        service.description.contains(searchQuery, ignoreCase = true) ||
                        service.location.contains(searchQuery, ignoreCase = true) ||
                        service.categories.any { it.contains(searchQuery, ignoreCase = true) }

                categoryMatch && searchMatch
            }

            if (servicesToDisplay.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = "No results",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            if (searchQuery.isNotBlank() || selectedCategory != "All") {
                                "No services found"
                            } else {
                                "No services available yet"
                            },
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 16.sp
                        )
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    items(servicesToDisplay) { service ->
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
fun BookingsScreen() {
    var bookings by remember { mutableStateOf<List<com.example.grindsphere.models.Booking>>(emptyList()) }
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
                    doc.toObject<com.example.grindsphere.models.Booking>()?.copy(id = doc.id)
                }
                isLoading = false
            }
            .addOnFailureListener { isLoading = false }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0D324D), Color(0xFF7F5A83))))
            .padding(16.dp)
    ) {
        Text(
            "My Bookings",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else if (bookings.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("You have no bookings yet.", color = Color.White)
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
fun BookingCard(booking: com.example.grindsphere.models.Booking) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    booking.serviceName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                StatusBadge(status = booking.status)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Provider: ${booking.hustlerName}", style = MaterialTheme.typography.bodyMedium, color = Color.White)
            booking.timestamp?.let {
                Text(
                    "Date: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(it)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }

            }
        }
    }


@Composable
fun StatusBadge(status: String) {
    val (backgroundColor, textColor) = when (status.lowercase(Locale.ROOT)) {
        "accepted" -> Color.Green.copy(alpha = 0.2f) to Color.Black.copy(alpha = 0.9f)
        "declined" -> Color.Red.copy(alpha = 0.2f) to Color.Black.copy(alpha = 0.9f)
        "completed" -> Color.Blue.copy(alpha = 0.2f) to Color.Black.copy(alpha = 0.9f)
        "pending" -> Color.Yellow.copy(alpha = 0.3f) to Color.Black
        else -> Color.Gray.copy(alpha = 0.2f) to Color.Black
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
    val context = LocalContext.current
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
                val intent = Intent(context, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                context.startActivity(intent)
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
            .width(280.dp)
            .height(200.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (service.banner.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(service.banner),
                    contentDescription = service.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF7F5A83), Color(0xFF0D324D))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Storefront,
                        contentDescription = "Service",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                            startY = 100f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    service.name,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Rating and views
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "${service.views} views",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }

                // Categories
                if (service.categories.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        service.categories.joinToString(" • "),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
        StatusBadge(status = "declined")
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
        firestore.collection("conversations").document(conversationId)
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