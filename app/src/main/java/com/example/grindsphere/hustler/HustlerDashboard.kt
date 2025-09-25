package com.example.grindsphere.hustler

import android.util.Log
import com.google.firebase.firestore.Query
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.grindsphere.LoginActivity
import com.example.grindsphere.R
import com.example.grindsphere.models.Booking
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import com.google.firebase.firestore.ktx.toObject
import java.text.SimpleDateFormat
import java.util.*

data class HustlerServiceCard(
    val id: String,
    val name: String,
    val bannerUrl: String,
    val views: Long = 0,
    val categories: List<String> = listOf()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HustlerDashboard(
    viewModel: SharedViewModel = viewModel()
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val currentUser = auth.currentUser

    val viewModelSelectedCategory by viewModel.selectedCategory.collectAsState()

    var hustlerName by remember { mutableStateOf("Hustler") }
    var profilePicUrl by remember { mutableStateOf("") }
    var services by remember { mutableStateOf(listOf<HustlerServiceCard>()) }
    var favoriteServices by remember { mutableStateOf(listOf<HustlerServiceCard>()) }
    var allServices by remember { mutableStateOf(listOf<HustlerServiceCard>()) }
    var totalViews by remember { mutableLongStateOf(0L) }
    var showMenu by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showMessagesScreen by remember { mutableStateOf(false) }
    var showFavorites by remember { mutableStateOf(false) }
    var showBookings by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("") }
    var pendingRequests by remember { mutableStateOf(0) }

    LaunchedEffect(viewModelSelectedCategory) {
        selectedCategory = viewModelSelectedCategory
    }

    LaunchedEffect(showSearchBar) {
        if (!showSearchBar) {
            viewModel.clearSelectedCategory()
            selectedCategory = ""
        }
    }

    val predefinedCategories = listOf(
        "Tutoring", "Design", "Tech Support", "Photography",
        "Fashion", "Food", "Music", "Fitness", "Transport", "Nails", "Hair", "Beauty", "Cake", "DJ"
    )

    // Load pending requests count
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            firestore.collection("bookingRequests")
                .whereEqualTo("hustlerUid", uid)
                .whereEqualTo("status", "pending")
                .addSnapshotListener { snapshot, error ->
                    pendingRequests = snapshot?.size() ?: 0
                }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val storageRef = storage.reference
            val imageRef =
                storageRef.child("profile_images/${currentUser?.uid}/${UUID.randomUUID()}")

            imageRef.putFile(uri)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        currentUser?.uid?.let { uid ->
                            firestore.collection("users").document(uid)
                                .update("profilePicUrl", downloadUri.toString())
                                .addOnSuccessListener {
                                    profilePicUrl = downloadUri.toString()
                                    Toast.makeText(
                                        context,
                                        "Profile picture updated!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    }
                }
        }
    }

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    hustlerName = doc.getString("name") ?: "Hustler"
                    profilePicUrl = doc.getString("profilePicUrl") ?: ""
                }
        }
    }

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            firestore.collection("services")
                .whereEqualTo("ownerUid", uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT)
                            .show()
                        return@addSnapshotListener
                    }
                    val list = snapshot?.documents?.map { doc ->
                        @Suppress("UNCHECKED_CAST")
                        HustlerServiceCard(
                            id = doc.id,
                            name = doc.getString("name") ?: "Service",
                            bannerUrl = doc.getString("banner") ?: "",
                            views = doc.getLong("views") ?: 0L,
                            categories = doc.get("categories") as? List<String> ?: listOf()
                        )
                    } ?: listOf()
                    services = list
                    totalViews = list.sumOf { it.views }
                }
        }
    }

    LaunchedEffect(Unit) {
        firestore.collection("services")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(
                        context,
                        "Error loading services: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addSnapshotListener
                }

                val list = snapshot?.documents?.map { doc ->
                    @Suppress("UNCHECKED_CAST")
                    HustlerServiceCard(
                        id = doc.id,
                        name = doc.getString("name") ?: "Service",
                        bannerUrl = doc.getString("banner") ?: "",
                        views = doc.getLong("views") ?: 0L,
                        categories = doc.get("categories") as? List<String> ?: listOf()
                    )
                } ?: listOf()
                allServices = list
            }
    }

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid).addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                @Suppress("UNCHECKED_CAST")
                val savedServiceIds = snapshot?.get("savedServices") as? List<String> ?: listOf()
                if (savedServiceIds.isNotEmpty()) {
                    firestore.collection("services")
                        .whereIn("__name__", savedServiceIds)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            val favoriteList = querySnapshot.documents.map { doc ->
                                @Suppress("UNCHECKED_CAST")
                                HustlerServiceCard(
                                    id = doc.id,
                                    name = doc.getString("name") ?: "Service",
                                    bannerUrl = doc.getString("banner") ?: "",
                                    views = doc.getLong("views") ?: 0L,
                                    categories = doc.get("categories") as? List<String> ?: listOf()
                                )
                            }
                            favoriteServices = favoriteList
                        }
                } else {
                    favoriteServices = listOf()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            showMessagesScreen -> "Messages"
                            showBookings -> "Bookings"
                            selectedTab == 3 -> "Home"
                            selectedTab == 2 -> "Search"
                            selectedTab == 1 -> "Messages"
                            showFavorites -> "Favorites"
                            else -> "Dashboard"
                        },
                        color = Color.White
                    )
                },
                actions = {
                    if (!showFavorites && favoriteServices.isNotEmpty()) {
                        IconButton(
                            onClick = { showFavorites = true },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Favorites",
                                tint = Color(0xFFFFD700)
                            )
                        }
                    }

                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = Color.White
                        )
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Logout") },
                            onClick = {
                                auth.signOut()
                                context.startActivity(Intent(context, LoginActivity::class.java))
                                (context as? ComponentActivity)?.finish()
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D324D)
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = {
                        selectedTab = 3
                        showSearchBar = false
                        showMessagesScreen = false
                        showFavorites = false
                        showBookings = false
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        showSearchBar = true
                        showMessagesScreen = false
                        showFavorites = false
                        showBookings = false
                    },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    label = { Text("Search") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        showMessagesScreen = true
                        showSearchBar = false
                        showFavorites = false
                        showBookings = false
                    },
                    icon = { Icon(Icons.Default.MailOutline, contentDescription = "Messages") },
                    label = { Text("Messages") }
                )
                NavigationBarItem(
                    selected = showBookings,
                    onClick = {
                        showBookings = true
                        showSearchBar = false
                        showMessagesScreen = false
                        showFavorites = false
                        selectedTab = -1 // Deselect other tabs
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = "Bookings") },
                    label = { Text("Bookings") }
                )
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        showSearchBar = false
                        showMessagesScreen = false
                        showFavorites = false
                        showBookings = false
                    },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Brush.verticalGradient(listOf(Color(0xFF0D324D), Color(0xFF7F5A83))))
        ) {
            when {
                showMessagesScreen -> {
                    MessagesScreen(
                        onOpenChat = { conversationId, customerUid, customerName ->
                            val intent = Intent(context, ChatActivity::class.java).apply {
                                putExtra("conversationId", conversationId)
                                putExtra("customerUid", customerUid)
                                putExtra("customerName", customerName)
                            }
                            context.startActivity(intent)
                        },
                        onStartNewChat = {
                            val intent = Intent(context, NewChatActivity::class.java)
                            context.startActivity(intent)
                        }
                    )
                }

                showBookings -> {
                    BookingsScreen()
                }

                showSearchBar -> {
                    SearchScreen(
                        allServices = allServices,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        selectedCategory = selectedCategory,
                        onSelectedCategoryChange = { selectedCategory = it },
                        predefinedCategories = predefinedCategories,
                        onServiceClick = { serviceId ->
                            val intent = Intent(context, ServiceDetailActivity::class.java)
                            intent.putExtra("serviceId", serviceId)
                            context.startActivity(intent)
                        }
                    )
                }

                selectedTab == 3 -> {
                    HomePageScreen(
                        onNavigateToSearch = {
                            selectedTab = 2
                            showSearchBar = true
                        },
                        viewModel = viewModel
                    )
                }

                showFavorites -> {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { showFavorites = false }
                                .padding(bottom = 16.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Dashboard",
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Back to Dashboard", color = Color.White, fontSize = 16.sp)
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
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No favorite services yet",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 16.sp
                                )
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
                                                val intent = Intent(
                                                    context,
                                                    ServiceDetailActivity::class.java
                                                )
                                                intent.putExtra("serviceId", service.id)
                                                context.startActivity(intent)
                                            },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color.White.copy(
                                                alpha = 0.1f
                                            )
                                        )
                                    ) {
                                        Column {
                                            if (service.bannerUrl.isNotEmpty()) {
                                                Image(
                                                    painter = rememberAsyncImagePainter(service.bannerUrl),
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
                                                modifier = Modifier.padding(
                                                    horizontal = 8.dp,
                                                    vertical = 4.dp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (profilePicUrl.isNotEmpty()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(
                                            ImageRequest.Builder(
                                                LocalContext.current
                                            ).data(profilePicUrl).build()
                                        ),
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(CircleShape)
                                            .clickable { imagePickerLauncher.launch("image/*") }
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = R.drawable.noprofile),
                                        contentDescription = "Profile Placeholder",
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(CircleShape)
                                            .clickable { imagePickerLauncher.launch("image/*") }
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Welcome back, $hustlerName 👋",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White.copy(
                                        alpha = 0.15f
                                    )
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "Your Stats",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Total Views: $totalViews",
                                        color = Color.White,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        "Active Services: ${services.size}",
                                        color = Color.White,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        "Favorites: ${favoriteServices.size}",
                                        color = Color.White,
                                        fontSize = 16.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Reviews Section - FIXED VERSION
                            Text("Recent Reviews", color = Color.White, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp))

// Move the recentReviews state declaration outside the LaunchedEffect
                            var recentReviews by remember { mutableStateOf(listOf<Review>()) }

// Load reviews for user's services - FIXED VERSION
                            // Load reviews for user's services - FIXED VERSION
                            LaunchedEffect(currentUser?.uid, services.map { it.id }) {
                                currentUser?.uid?.let { uid ->
                                    if (services.isNotEmpty()) {
                                        val serviceIds = services.map { it.id }
                                        Log.d("HustlerDashboard", "Loading reviews for service IDs: $serviceIds")

                                        // Check if serviceIds is not empty to avoid Firestore error
                                        if (serviceIds.isNotEmpty()) {
                                            try {
                                                firestore.collection("reviews")
                                                    .whereIn("serviceId", serviceIds)
                                                    .orderBy("timestamp", Query.Direction.DESCENDING)
                                                    .limit(3)
                                                    .get()
                                                    .addOnSuccessListener { snapshot ->
                                                        Log.d("HustlerDashboard", "Firestore query returned ${snapshot.size()} documents")

                                                        // Log each document to see what's being returned
                                                        snapshot.documents.forEachIndexed { index, doc ->
                                                            Log.d("HustlerDashboard", "Doc $index: ${doc.data}")
                                                        }

                                                        recentReviews = snapshot.documents.mapNotNull { doc ->
                                                            try {
                                                                val review = Review(
                                                                    id = doc.id,
                                                                    serviceId = doc.getString("serviceId") ?: "",
                                                                    userId = doc.getString("userId") ?: "",
                                                                    userName = doc.getString("userName") ?: "",
                                                                    rating = (doc.getLong("rating") ?: 0L).toInt(),
                                                                    comment = doc.getString("comment") ?: "",
                                                                    timestamp = doc.getLong("timestamp") ?: 0L
                                                                )
                                                                Log.d("HustlerDashboard", "Parsed review: $review")
                                                                review
                                                            } catch (e: Exception) {
                                                                Log.e("HustlerDashboard", "Error parsing review: ${e.message}")
                                                                null
                                                            }
                                                        }
                                                        Log.d("HustlerDashboard", "Final recentReviews count: ${recentReviews.size}")
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Log.e("HustlerDashboard", "Error loading reviews: ${e.message}")
                                                        recentReviews = emptyList()
                                                    }
                                            } catch (e: Exception) {
                                                Log.e("HustlerDashboard", "Error in reviews query: ${e.message}")
                                                recentReviews = emptyList()
                                            }
                                        } else {
                                            Log.d("HustlerDashboard", "Service IDs list is empty")
                                            recentReviews = emptyList()
                                        }
                                    } else {
                                        Log.d("HustlerDashboard", "No services found for user")
                                        recentReviews = emptyList()
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 24.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    if (recentReviews.isNotEmpty()) {
                                        recentReviews.forEachIndexed { index, review ->
                                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                                // User and rating
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(review.userName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                    StarRating(
                                                        rating = review.rating,
                                                        onRatingChange = {},
                                                        interactive = false,
                                                        starSize = 16.dp
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))

                                                // Comment
                                                Text(
                                                    review.comment,
                                                    color = Color.White.copy(alpha = 0.8f),
                                                    fontSize = 12.sp,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )

                                                Spacer(modifier = Modifier.height(4.dp))

                                                // Service name - FIXED: Use proper state management
                                                var serviceName by remember { mutableStateOf("Service") }

                                                LaunchedEffect(review.serviceId) {
                                                    val service = services.find { it.id == review.serviceId }
                                                    serviceName = service?.name ?: "Service"
                                                }

                                                Text(
                                                    "For: $serviceName",
                                                    color = Color.White.copy(alpha = 0.6f),
                                                    fontSize = 10.sp
                                                )
                                            }

                                            // Divider between reviews (except after the last one)
                                            if (index < recentReviews.size - 1) {
                                                HorizontalDivider(
                                                    color = Color.White.copy(alpha = 0.2f),
                                                    thickness = 1.dp,
                                                    modifier = Modifier.padding(vertical = 8.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        Text(
                                            "No reviews yet",
                                            color = Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 16.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                            // Your Services Section
                            Text("Your Services", color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.height(140.dp)
                            ) {
                                items(services) { service ->
                                    Card(
                                        modifier = Modifier
                                            .width(120.dp)
                                            .clickable {
                                                val intent =
                                                    Intent(context, EditServiceActivity::class.java)
                                                intent.putExtra("serviceId", service.id)
                                                context.startActivity(intent)
                                            },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color.White.copy(
                                                alpha = 0.1f
                                            )
                                        )
                                    ) {
                                        Column {
                                            if (service.bannerUrl.isNotEmpty()) {
                                                Image(
                                                    painter = rememberAsyncImagePainter(service.bannerUrl),
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
                                        }
                                    }
                                }

                                item {
                                    Card(
                                        modifier = Modifier
                                            .width(120.dp)
                                            .clickable {
                                                val intent =
                                                    Intent(context, EditServiceActivity::class.java)
                                                context.startActivity(intent)
                                            },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color.White.copy(
                                                alpha = 0.1f
                                            )
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .height(120.dp)
                                                .fillMaxWidth(),
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "Add Service",
                                                tint = Color.White,
                                                modifier = Modifier.size(40.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                "Add Service",
                                                color = Color.White,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SEARCH SCREEN

        }
    }
}

@Composable
fun BookingsScreen() {
    var bookings by remember { mutableStateOf<List<Booking>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            isLoading = false
            return@LaunchedEffect
        }
        firestore.collection("bookingRequests")
            .whereEqualTo("hustlerId", currentUserId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    isLoading = false
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    bookings = snapshots.documents.mapNotNull { doc ->
                        doc.toObject<Booking>()?.copy(id = doc.id)
                    }
                }
                isLoading = false
            }
    }

    fun updateBookingStatus(bookingId: String, newStatus: String) {
        firestore.collection("bookingRequests").document(bookingId)
            .update("status", newStatus)
            .addOnSuccessListener {
                Toast.makeText(context, "Booking status updated to $newStatus", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to update booking status", Toast.LENGTH_SHORT).show()
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Booking Requests", style = MaterialTheme.typography.headlineSmall, color = Color.White, modifier = Modifier.padding(bottom = 16.dp))
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (bookings.isEmpty()) {
            Text("You have no booking requests yet.", color = Color.White, modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(bookings, key = { it.id }) { booking ->
                    BookingCard(
                        booking = booking,
                        onAccept = { updateBookingStatus(booking.id, "accepted") },
                        onDecline = { updateBookingStatus(booking.id, "declined") }
                    )
                }
            }
        }
    }
}

@Composable
fun BookingCard(booking: Booking, onAccept: () -> Unit, onDecline: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(booking.serviceName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Customer: ${booking.customerName}", style = MaterialTheme.typography.bodyMedium, color = Color.White)
            booking.timestamp?.let {
                Text(
                    "Date: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(it)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
            Text("Price: R${String.format(Locale.US, "%.2f", booking.price)}", style = MaterialTheme.typography.bodyMedium, color = Color.White)
            if (booking.message.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Message: ${booking.message}", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (booking.status == "pending") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(onClick = onAccept, modifier = Modifier.weight(1f)) {
                        Text("Accept")
                    }
                    OutlinedButton(onClick = onDecline, modifier = Modifier.weight(1f)) {
                        Text("Decline")
                    }
                }
            } else {
                Text("Status: ${booking.status.replaceFirstChar { it.titlecase(Locale.getDefault()) }}", color = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun HustlerDashboardPreview() {
    HustlerDashboard()
}
