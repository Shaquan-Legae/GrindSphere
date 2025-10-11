@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.grindsphere.customer

import android.content.ContentValues.TAG
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Logout
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
import androidx.compose.ui.text.style.TextAlign
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
import com.example.grindsphere.hustler.ChatScreen
import com.example.grindsphere.hustler.MessageBubble
import com.example.grindsphere.models.Booking
import com.example.grindsphere.models.Conversation
import com.example.grindsphere.models.Message
import com.example.grindsphere.models.Service
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
                composable("search") { CustomerSearchScreen(navController) } // ADD THIS
                composable("conversations") { ConversationsScreen(navController) }
                composable("profile") { CustomerProfileScreen(navController) }
                composable("serviceDetails/{serviceId}") { backStackEntry ->
                    val serviceId = backStackEntry.arguments?.getString("serviceId") ?: ""
                    ServiceDetailsScreen(serviceId = serviceId, navController = navController)
                }
                composable("chat/{conversationId}") { backStackEntry ->
                    val conversationId = backStackEntry.arguments?.getString("conversationId")
                    if (conversationId != null) {
                        CustomerChatScreen(
                            conversationId = conversationId,
                            onBack = { navController.popBackStack() }
                        )
                    } else {
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

    var unreadMessagesCount by remember { mutableIntStateOf(0) }
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    // Load unread messages count
    LaunchedEffect(auth.currentUser?.uid) {
        auth.currentUser?.uid?.let { uid ->
            firestore.collection("conversations")
                .whereArrayContains("participants", uid)
                .addSnapshotListener { snapshot, _ ->
                    // You'll need to implement proper unread count logic here
                    // For now, we'll just show if there are any conversations
                    unreadMessagesCount = snapshot?.size() ?: 0
                }
        }
    }

    NavigationBar(
        containerColor = Color.White
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = "Services") },
            label = { Text("Services") },
            selected = currentRoute == "services",
            onClick = { navController.navigate("services") }
        )

        // ADD SEARCH NAVIGATION ITEM
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
            label = { Text("Search") },
            selected = currentRoute == "search",
            onClick = { navController.navigate("search") }
        )

        NavigationBarItem(
            icon = {
                BadgedBox(
                    badge = {
                        if (unreadMessagesCount > 0) {
                            Badge {
                                Text(unreadMessagesCount.toString())
                            }
                        }
                    }
                ) {
                    Icon(Icons.Filled.Email, contentDescription = "Messages")
                }
            },
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
    val firestore = FirebaseFirestore.getInstance()

    var featuredServices by remember { mutableStateOf<List<Service>>(emptyList()) }
    var popularServices by remember { mutableStateOf<List<Service>>(emptyList()) }
    var newestServices by remember { mutableStateOf<List<Service>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch all services and organize them
    LaunchedEffect(Unit) {
        firestore.collection("services")
            .get()
            .addOnSuccessListener { snapshot ->
                val allServices = snapshot.documents.mapNotNull { doc ->
                    doc.toObject<Service>()?.copy(id = doc.id)
                }

                if (allServices.isNotEmpty()) {
                    // Popular services (most viewed)
                    popularServices = allServices.sortedByDescending { it.views }.take(10)

                    // Featured services (top 3 most viewed) - BIGGER CARDS
                    featuredServices = popularServices.take(5)

                    // NEW: Newest services - sorted by creation date (using document ID as proxy for now)
                    // In a real app, you'd have a createdAt timestamp field
                    newestServices = allServices.sortedByDescending { it.id }.take(8)

                    isLoading = false
                } else {
                    isLoading = false
                }
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    val displayedServices = remember(searchQuery, popularServices) {
        if (searchQuery.isBlank()) {
            popularServices
        } else {
            popularServices.filter { service ->
                service.name.contains(searchQuery, ignoreCase = true) ||
                        service.description.contains(searchQuery, ignoreCase = true) ||
                        service.categories.any { it.contains(searchQuery, ignoreCase = true) }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0D324D),
                        Color(0xFF7F5A83),
                        Color(0xFFA188A6)
                    )
                )
            )
    ) {
        // Header with welcome message
        item {
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

                // Search icon - NAVIGATES TO SEARCH SCREEN
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.White,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { navController.navigate("search") }
                )
            }
        }

        // Show total services count
        item {
            Text(
                "${popularServices.size} Services Available",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
        }

        // Featured Services Section - BIGGER CARDS like Showmax
        if (featuredServices.isNotEmpty()) {
            item {
                Text(
                    "Featured Services",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    items(featuredServices) { service ->
                        // BIGGER CARD - Showmax style
                        Card(
                            modifier = Modifier
                                .width(320.dp) // Increased width
                                .height(280.dp) // Increased height
                                .clickable {
                                    navController.navigate("serviceDetails/${service.id}")
                                },
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
                                                    listOf(
                                                        Color(0xFF7F5A83),
                                                        Color(0xFF0D324D)
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Storefront,
                                            contentDescription = "Service",
                                            tint = Color.White,
                                            modifier = Modifier.size(48.dp) // Bigger icon
                                        )
                                    }
                                }

                                // In the featured services card, replace the gradient Box with:
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = 0.4f) // Lighter overlay
                                                ),
                                                startY = 0f,
                                                endY = 300f
                                            )
                                        )
                                )

                                // Enhanced service info at bottom
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(20.dp) // More padding
                                ) {
                                    Text(
                                        service.name,
                                        color = Color.White,
                                        fontSize = 22.sp, // Bigger font
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2, // Allow 2 lines
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Enhanced rating and views
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (service.rating > 0.0) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Star,
                                                    contentDescription = "Rating",
                                                    tint = Color(0xFFFFD700),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    String.format(Locale.US, "%.1f", service.rating),
                                                    color = Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Visibility,
                                                contentDescription = "Views",
                                                tint = Color.White.copy(alpha = 0.9f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "${service.views} views",
                                                color = Color.White.copy(alpha = 0.9f),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    // Categories if available
                                    if (service.categories.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
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
                }
            }
        }

        // Popular Categories Section
        item {
            Text(
                "Browse Categories",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
        }

        item {
            val popularCategories = listOf("Tutoring", "Beauty", "Food", "Fitness", "Design", "Tech", "Music", "Photography")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 24.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                items(popularCategories) { category ->
                    Card(
                        modifier = Modifier
                            .width(120.dp)
                            .height(80.dp)
                            .clickable {
                                navController.navigate("search") // Navigate to search instead of setting query
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.2f)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xFF7F5A83).copy(alpha = 0.6f),
                                            Color(0xFF0D324D).copy(alpha = 0.6f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                category,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Newest Services Section - ONLY SHOW RECENTLY CREATED SERVICES
        if (newestServices.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(
                        "Newly Added",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        "${newestServices.size} new",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    items(newestServices) { service ->
                        ServiceCardMedium(
                            service = service,
                            onClick = { navController.navigate("serviceDetails/${service.id}") }
                        )
                    }
                }
            }
        }

        // Most Popular Services Section
        if (popularServices.isNotEmpty()) {
            item {
                Text(
                    "Most Popular",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    items(popularServices.take(8)) { service ->
                        ServiceCardMedium(
                            service = service,
                            onClick = { navController.navigate("serviceDetails/${service.id}") }
                        )
                    }
                }
            }
        }

        // Loading state
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }

        // No services state
        if (!isLoading && popularServices.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Storefront,
                            contentDescription = "No services",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No services available yet",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 16.sp
                        )
                        Text(
                            "Be the first to add a service!",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
@Composable
fun ServiceCardMedium(
    service: Service,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(200.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
    ) {
        Column {
            if (service.banner.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(service.banner),
                    contentDescription = service.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .height(120.dp)
                        .fillMaxWidth()
                )
            } else {
                Box(
                    modifier = Modifier
                        .height(120.dp)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF7F5A83),
                                    Color(0xFF0D324D)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Storefront,
                        contentDescription = "Service",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    service.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Rating if available
                if (service.rating > 0.0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            String.format(Locale.US, "%.1f", service.rating),
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }

                Text(
                    "${service.views} views",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )

                // Categories if available
                if (service.categories.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        service.categories.joinToString(" • "),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// Small service card for search results grid
@Composable
fun ServiceCardSmall(
    service: Service,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
    ) {
        Column {
            if (service.banner.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(service.banner),
                    contentDescription = service.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .height(100.dp)
                        .fillMaxWidth()
                )
            } else {
                Box(
                    modifier = Modifier
                        .height(100.dp)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF7F5A83),
                                    Color(0xFF0D324D)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Storefront,
                        contentDescription = "Service",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    service.name,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "${service.views} views",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 9.sp
                )
            }
        }
    }
}


@Composable
fun CustomerProfileScreen(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    var customerName by remember { mutableStateOf("") }
    var customerEmail by remember { mutableStateOf("") }
    var profilePicUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }

    // Stats
    var totalBookings by remember { mutableIntStateOf(0) }
    var completedBookings by remember { mutableIntStateOf(0) }
    var favoriteServices by remember { mutableStateOf<List<Service>>(emptyList()) }
    var showFavorites by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { uploadProfilePicture(uri, auth.currentUser?.uid, firestore) }
    }

    // Load customer data
    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Load user profile
            firestore.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { doc ->
                    customerName = doc.getString("name") ?: "Customer"
                    customerEmail = doc.getString("email") ?: currentUser.email ?: ""
                    profilePicUrl = doc.getString("profilePicUrl") ?: ""
                    editName = customerName
                    isLoading = false
                }

            // Load booking stats
            firestore.collection("bookingRequests")
                .whereEqualTo("customerId", currentUser.uid)
                .addSnapshotListener { snapshot, error ->
                    if (snapshot != null) {
                        totalBookings = snapshot.size()
                        completedBookings = snapshot.documents.count {
                            it.getString("status") == "completed"
                        }
                    }
                }

            // Load favorite services
            @Suppress("UNCHECKED_CAST")
            firestore.collection("users").document(currentUser.uid)
                .addSnapshotListener { doc, error ->
                    if (doc != null) {
                        val savedServiceIds = doc.get("savedServices") as? List<String> ?: listOf()
                        if (savedServiceIds.isNotEmpty()) {
                            firestore.collection("services")
                                .whereIn("__name__", savedServiceIds)
                                .get()
                                .addOnSuccessListener { querySnapshot ->
                                    favoriteServices = querySnapshot.documents.mapNotNull { doc ->
                                        doc.toObject<Service>()?.copy(id = doc.id)
                                    }
                                }
                        } else {
                            favoriteServices = emptyList()
                        }
                    }
                }
        }
    }

    if (showFavorites) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { showFavorites = false }
                    .padding(bottom = 16.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Profile",
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Back to Profile", color = Color.White, fontSize = 16.sp)
            }

            Text(
                "Your Favorites",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (favoriteServices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.StarOutline,
                            contentDescription = "No favorites",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No favorite services yet",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 16.sp
                        )
                        Text(
                            "Tap the star icon on services to add them to favorites",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.height(140.dp)
                ) {
                    items(favoriteServices) { service ->
                        Card(
                            modifier = Modifier
                                .width(120.dp)
                                .clickable {
                                    navController.navigate("serviceDetails/${service.id}")
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.1f)
                            )
                        ) {
                            Column {
                                if (service.banner.isNotEmpty()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(service.banner),
                                        contentDescription = service.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .height(80.dp)
                                            .fillMaxWidth()
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .height(80.dp)
                                            .fillMaxWidth()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        Color(0xFF7F5A83),
                                                        Color(0xFF0D324D)
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Storefront,
                                            contentDescription = "Service",
                                            tint = Color.White,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                Text(
                                    service.name,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(8.dp)
                                )
                                Text(
                                    "${service.views} views",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        return
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0D324D), Color(0xFF7F5A83))))
            .verticalScroll(rememberScrollState())
    ) {
        // Header with Sign Out at top right
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                "My Profile",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
            val context = LocalContext.current
            // Sign Out Icon at top right
            IconButton(
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(context, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Sign Out",
                    tint = Color.White
                )
            }
        }

        // Profile Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Picture
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (profilePicUrl.isNotEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(profilePicUrl),
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF7F5A83)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    // Edit overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Profile Picture",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Name and Email with Edit Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        customerName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            showEditDialog = true
                            editName = customerName
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Name",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    customerEmail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        // Stats Section
        Text(
            "My Activity",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Total Bookings
            StatCard(
                title = "Total Bookings",
                value = totalBookings.toString(),
                icon = Icons.AutoMirrored.Filled.ListAlt,
                modifier = Modifier.weight(1f)
            )

            // Completed Bookings
            StatCard(
                title = "Completed",
                value = completedBookings.toString(),
                icon = Icons.Default.CheckCircle,
                modifier = Modifier.weight(1f)
            )

            // Favorites - MAKE IT CLICKABLE
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showFavorites = true },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Favorites",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        favoriteServices.size.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Favorites",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Edit Name Dialog
    if (showEditDialog) {
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    "Edit Your Name",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "Update your display name:",
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    TextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Full Name") },
                        placeholder = { Text("Enter your name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = Color(0xFF7F5A83),
                            unfocusedIndicatorColor = Color.Gray,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "This name will be visible to service providers",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank() && editName != customerName) {
                            auth.currentUser?.uid?.let { uid ->
                                firestore.collection("users").document(uid)
                                    .update("name", editName)
                                    .addOnSuccessListener {
                                        customerName = editName
                                        Toast.makeText(context, "Name updated successfully!", Toast.LENGTH_SHORT).show()
                                        showEditDialog = false
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context, "Failed to update name: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        } else if (editName.isBlank()) {
                            Toast.makeText(context, "Please enter a name", Toast.LENGTH_SHORT).show()
                        } else {
                            showEditDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F5A83)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditDialog = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", color = Color(0xFF7F5A83))
                }
            }
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// Helper function to upload profile picture
private fun uploadProfilePicture(uri: Uri, userId: String?, firestore: FirebaseFirestore) {
    if (userId == null) return

    val storage = FirebaseStorage.getInstance()
    val storageRef = storage.reference
    val imageRef = storageRef.child("profile_pictures/$userId/${UUID.randomUUID()}")

    imageRef.putFile(uri)
        .addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                firestore.collection("users").document(userId)
                    .update("profilePicUrl", downloadUri.toString())
                    .addOnSuccessListener {
                        // Profile picture updated successfully
                    }
            }
        }
        .addOnFailureListener {
            // Handle error
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

// Add this composable function to your CustomerDashboard.kt file
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerChatScreen(
    conversationId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }
    var conversation by remember { mutableStateOf<Conversation?>(null) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hustlerName by remember { mutableStateOf("Service Provider") }
    var serviceName by remember { mutableStateOf("Service") }
    var hustlerProfilePic by remember { mutableStateOf("") }
    var currentUserProfilePic by remember { mutableStateOf("") }
    var hustlerId by remember { mutableStateOf("") }
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Show error toast when errorMessage changes
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            errorMessage = null
        }
    }

    // Load current user's profile picture
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    currentUserProfilePic = doc.getString("profilePicUrl") ?: ""
                }
        }
    }

    // Load conversation details and extract hustler info
    LaunchedEffect(conversationId) {
        try {
            firestore.collection("conversations").document(conversationId)
                .addSnapshotListener { snapshot, error ->
                    conversation = snapshot?.toObject<Conversation>()

                    // Extract hustler info from participantNames
                    val currentUserId = currentUser?.uid
                    if (currentUserId != null && conversation != null) {
                        // Find the other participant (the hustler)
                        hustlerId = conversation!!.participants.find { it != currentUserId } ?: ""
                        hustlerName = conversation!!.participantNames[hustlerId] ?: "Service Provider"
                        serviceName = conversation!!.serviceName ?: "Service"

                        // Load hustler's profile picture
                        if (hustlerId.isNotEmpty()) {
                            firestore.collection("users").document(hustlerId).get()
                                .addOnSuccessListener { doc ->
                                    hustlerProfilePic = doc.getString("profilePicUrl") ?: ""
                                }
                        }
                    }

                    loading = false
                }
        } catch (e: Exception) {
            errorMessage = "Error loading conversation: ${e.message}"
            loading = false
        }
    }

    // Load messages
    LaunchedEffect(conversationId) {
        try {
            firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error loading messages: ${error.message}")
                        return@addSnapshotListener
                    }

                    messages = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject<Message>()?.copy(id = doc.id)
                    } ?: emptyList()

                    // Auto-scroll to bottom when new messages arrive
                    if (messages.isNotEmpty()) {
                        coroutineScope.launch {
                            scrollState.animateScrollToItem(messages.size - 1)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up messages listener: ${e.message}")
        }
    }

    // Send message function
    fun sendMessage() {
        if (messageText.isBlank() || currentUser == null) return

        coroutineScope.launch {
            try {
                // Get user name from Firestore
                val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
                val userName = userDoc.getString("name") ?: "Customer"

                val messageData = hashMapOf(
                    "senderId" to currentUser.uid,
                    "senderName" to userName,
                    "senderProfilePicUrl" to currentUserProfilePic, // Add profile picture URL
                    "text" to messageText,
                    "timestamp" to Timestamp.now(),
                    "type" to "text"
                )

                // Add message to subcollection
                firestore.collection("conversations")
                    .document(conversationId)
                    .collection("messages")
                    .add(messageData)
                    .await()

                // Update conversation last message
                firestore.collection("conversations")
                    .document(conversationId)
                    .update(
                        mapOf(
                            "lastMessage" to messageText,
                            "lastMessageTimestamp" to FieldValue.serverTimestamp()
                        )
                    )

                messageText = ""

            } catch (e: Exception) {
                errorMessage = "Failed to send message: ${e.message}"
                Log.e(TAG, "Failed to send message: ${e.message}", e)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Hustler profile picture in top bar
                        if (hustlerProfilePic.isNotEmpty()) {
                            Image(
                                painter = rememberAsyncImagePainter(hustlerProfilePic),
                                contentDescription = "Hustler Profile",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF7F5A83)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    hustlerName.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(hustlerName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(
                                serviceName,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D324D)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0D324D),
                            Color(0xFF7F5A83)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Messages list
                if (loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Loading messages...",
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else if (messages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "No messages",
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No messages yet",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Start the conversation with $hustlerName!",
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Service: $serviceName",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        state = scrollState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(messages) { message ->
                            MessageBubble(
                                message = message,
                                isMe = message.senderId == currentUser?.uid,
                                currentUserProfilePic = currentUserProfilePic,
                                otherUserProfilePic = hustlerProfilePic
                            )
                        }
                    }
                }

                // Message input
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...", color = Color.Gray) },
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        keyboardActions = KeyboardActions(onSend = { sendMessage() }),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    IconButton(
                        onClick = { sendMessage() },
                        enabled = messageText.isNotBlank(),
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                if (messageText.isNotBlank()) Color(0xFFFFD700)
                                else Color.Gray.copy(alpha = 0.5f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (messageText.isNotBlank()) Color.Black else Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    isMe: Boolean,
    currentUserProfilePic: String,
    otherUserProfilePic: String
) {
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val messageTime = timeFormat.format(message.timestamp.toDate())
    val profilePicUrl = if (isMe) currentUserProfilePic else otherUserProfilePic

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMe) {
            // Profile picture for other user's messages (left side)
            ProfilePicture(
                profilePicUrl = profilePicUrl,
                userName = message.senderName ?: "User",
                size = 32.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.weight(1f)
        ) {
            // Sender name (only show for other user's messages)
            if (!isMe) {
                Text(
                    text = message.senderName ?: "User",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            Row(
                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.Bottom
            ) {
                if (isMe) {
                    // Timestamp for my messages (on left side of bubble)
                    Text(
                        text = messageTime,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(bottom = 12.dp, end = 8.dp)
                    )
                }

                // Message bubble
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isMe) Color(0xFFFFD700) // Gold for current user
                        else Color(0xFF7F5A83) // Purple for other user
                    ),
                    shape = RoundedCornerShape(
                        topStart = if (isMe) 16.dp else 4.dp,
                        topEnd = if (isMe) 4.dp else 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    ),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // Message text
                        Text(
                            text = message.text,
                            color = if (isMe) Color.Black else Color.White,
                            fontSize = 16.sp,
                            lineHeight = 20.sp
                        )
                    }
                }

                if (!isMe) {
                    // Timestamp for other user's messages (on right side of bubble)
                    Text(
                        text = messageTime,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)
                    )
                }
            }
        }

        if (isMe) {
            // Profile picture for my messages (right side)
            Spacer(modifier = Modifier.width(8.dp))
            ProfilePicture(
                profilePicUrl = profilePicUrl,
                userName = message.senderName ?: "You",
                size = 32.dp
            )
        }
    }
}

// Reusable Profile Picture Component
@Composable
fun ProfilePicture(
    profilePicUrl: String,
    userName: String,
    size: Dp = 40.dp
) {
    if (profilePicUrl.isNotEmpty()) {
        Image(
            painter = rememberAsyncImagePainter(profilePicUrl),
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(0xFF7F5A83)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                userName.take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value / 2).sp
            )
        }
    }
}