@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.grindsphere.customer

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.google.firebase.storage.FirebaseStorage
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
                composable("profile") { CustomerProfileScreen(navController) }
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
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

        // Search Bar
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search services...", color = Color.Gray) },
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
        }

        // Show search results if searching
        if (searchQuery.isNotBlank()) {
            item {
                Text(
                    "Search Results for '$searchQuery'",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }

            if (displayedServices.isNotEmpty()) {
                items(displayedServices.chunked(2)) { rowServices ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        rowServices.forEach { service ->
                            ServiceCardSmall(
                                service = service,
                                onClick = { navController.navigate("serviceDetails/${service.id}") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Add empty space if odd number of services
                        if (rowServices.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
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
                                "No services found for '$searchQuery'",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 16.sp
                            )
                            Text(
                                "Try different search terms",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        } else {
            // REGULAR HOME PAGE CONTENT (when not searching)

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
                                    searchQuery = category
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

            // REMOVED: All Services Grid section

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
fun BookingCard(booking: Booking) {
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
