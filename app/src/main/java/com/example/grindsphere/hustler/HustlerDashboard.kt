package com.example.grindsphere.hustler

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.grindsphere.LoginActivity
import com.example.grindsphere.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

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
    var totalViews by remember { mutableStateOf(0L) }
    var showMenu by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showMessagesScreen by remember { mutableStateOf(false) }
    var showFavorites by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("") }

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

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val storageRef = storage.reference
            val imageRef = storageRef.child("profile_images/${currentUser?.uid}/${UUID.randomUUID()}")

            imageRef.putFile(uri)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        currentUser?.uid?.let { uid ->
                            firestore.collection("users").document(uid)
                                .update("profilePicUrl", downloadUri.toString())
                                .addOnSuccessListener {
                                    profilePicUrl = downloadUri.toString()
                                    Toast.makeText(context, "Profile picture updated!", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }
                    val list = snapshot?.documents?.map { doc ->
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
                    Toast.makeText(context, "Error loading services: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val list = snapshot?.documents?.map { doc ->
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

                val savedServiceIds = snapshot?.get("savedServices") as? List<String> ?: listOf()
                if (savedServiceIds.isNotEmpty()) {
                    firestore.collection("services")
                        .whereIn("__name__", savedServiceIds)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            val favoriteList = snapshot.documents.map { doc ->
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

    val filteredServices = remember(searchQuery, selectedCategory, allServices) {
        if (searchQuery.isBlank() && selectedCategory.isBlank()) {
            allServices.sortedByDescending { it.views }
        } else {
            allServices.filter { service ->
                val matchesName = searchQuery.isBlank() || service.name.contains(searchQuery, ignoreCase = true)
                val matchesCategorySearch = if (searchQuery.isNotBlank()) {
                    service.categories.any { category -> category.contains(searchQuery, ignoreCase = true) }
                } else { false }
                val matchesCategoryFilter = if (selectedCategory.isBlank()) {
                    true
                } else {
                    service.categories.any { it.equals(selectedCategory, ignoreCase = true) }
                }
                (matchesName || matchesCategorySearch) && matchesCategoryFilter
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
                            Icon(Icons.Default.Star, contentDescription = "Favorites", tint = Color(0xFFFFD700))
                        }
                    }

                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White)
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
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
                    },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    label = { Text("Search") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        showMessagesScreen = true
                        showFavorites = false
                    },
                    icon = { Icon(Icons.Default.MailOutline, contentDescription = "Messages") },
                    label = { Text("Messages") }
                )
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        showSearchBar = false
                        showMessagesScreen = false
                        showFavorites = false
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
                .background(Brush.verticalGradient(listOf(Color(0xFF0D324D), Color(0xFF7F5A83))))
                .padding(paddingValues)
        ) {
            when {
                showMessagesScreen -> {
                    MessagesScreen(
                        onOpenChat = { customerUid, customerName ->
                            val intent = Intent(context, ChatScreenActivity::class.java)
                            intent.putExtra("customerUid", customerUid)
                            intent.putExtra("customerName", customerName)
                            context.startActivity(intent)
                        },
                        onStartNewChat = {
                            val intent = Intent(context, NewChatActivity::class.java)
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
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back to Dashboard", tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Back to Dashboard", color = Color.White, fontSize = 16.sp)
                        }

                        Text("Your Favorites", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(bottom = 16.dp))

                        if (favoriteServices.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No favorite services yet", color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
                            }
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(favoriteServices) { service ->
                                    Card(
                                        modifier = Modifier
                                            .width(120.dp)
                                            .clickable {
                                                val intent = Intent(context, ServiceDetailActivity::class.java)
                                                intent.putExtra("serviceId", service.id)
                                                context.startActivity(intent)
                                            },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
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
                                                                listOf(Color(0xFF7F5A83), Color(0xFF0D324D))
                                                            )
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Storefront, contentDescription = "Service", tint = Color.White, modifier = Modifier.size(32.dp))
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
                }
                else -> {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (profilePicUrl.isNotEmpty()) {
                                Image(
                                    painter = rememberAsyncImagePainter(ImageRequest.Builder(LocalContext.current).data(profilePicUrl).build()),
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
                            Text("Welcome back, $hustlerName 👋", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Your Stats", color = Color.White, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Total Views: $totalViews", color = Color.White, fontSize = 16.sp)
                                Text("Active Services: ${services.size}", color = Color.White, fontSize = 16.sp)
                                Text("Favorites: ${favoriteServices.size}", color = Color.White, fontSize = 16.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

// Reviews Section
                        Text("Recent Reviews", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))

                        var reviews by remember { mutableStateOf<List<Review>>(emptyList()) }

// Fetch reviews for user's services
                        LaunchedEffect(services) {
                            if (services.isNotEmpty()) {
                                val reviewsList = mutableListOf<Review>()
                                services.forEach { service ->
                                    firestore.collection("services").document(service.id).collection("reviews")
                                        .get()
                                        .addOnSuccessListener { snapshot ->
                                            snapshot.documents.forEach { doc ->
                                                val review = Review(
                                                    id = doc.id,
                                                    userId = doc.getString("userId") ?: "",
                                                    userName = doc.getString("userName") ?: "Anonymous",
                                                    rating = doc.getLong("rating")?.toInt() ?: 0,
                                                    comment = doc.getString("comment") ?: "",
                                                    timestamp = doc.getLong("timestamp") ?: 0,
                                                    serviceId = service.id
                                                )
                                                reviewsList.add(review)
                                            }
                                            reviews = reviewsList.sortedByDescending { it.timestamp }.take(5)
                                        }
                                }
                            }
                        }

                        if (reviews.isNotEmpty()) {
                            Column {
                                reviews.forEach { review ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            // Rating stars
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("⭐".repeat(review.rating), color = Color(0xFFFFD700))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("${review.rating}/5", color = Color.White, fontSize = 14.sp)
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Review comment
                                            Text(
                                                review.comment,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Reviewer info and service
                                            Row(
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    "By ${review.userName}",
                                                    color = Color.White.copy(alpha = 0.7f),
                                                    fontSize = 12.sp
                                                )

                                                services.find { it.id == review.serviceId }?.let { service ->
                                                    Text(
                                                        "For: ${service.name}",
                                                        color = Color.White.copy(alpha = 0.7f),
                                                        fontSize = 12.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.width(100.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = "No reviews",
                                        tint = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "No reviews yet",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        "Reviews will appear here once customers rate your services",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        // Recent Activity Section
                        Text("Recent Activity", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                if (services.isNotEmpty()) {
                                    val recentServices = services.sortedByDescending { it.views }.take(3)
                                    recentServices.forEach { service ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        ) {
                                            if (service.bannerUrl.isNotEmpty()) {
                                                Image(
                                                    painter = rememberAsyncImagePainter(service.bannerUrl),
                                                    contentDescription = service.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0xFF7F5A83)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Storefront, contentDescription = "Service", tint = Color.White, modifier = Modifier.size(20.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(service.name, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text("${service.views} views", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                            }
                                        }
                                    }
                                } else {
                                    Text("No recent activity", color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(vertical = 16.dp))
                                }
                            }
                        }

                        // Your Services Section
                        Text("Your Services", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(services) { service ->
                                Card(
                                    modifier = Modifier
                                        .width(120.dp)
                                        .clickable {
                                            val intent = Intent(context, EditServiceActivity::class.java)
                                            intent.putExtra("serviceId", service.id)
                                            context.startActivity(intent)
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
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
                                                            listOf(Color(0xFF7F5A83), Color(0xFF0D324D))
                                                        )
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Storefront, contentDescription = "Service", tint = Color.White, modifier = Modifier.size(32.dp))
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
                                            val intent = Intent(context, EditServiceActivity::class.java)
                                            context.startActivity(intent)
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .height(120.dp)
                                            .fillMaxWidth(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add Service", tint = Color.White, modifier = Modifier.size(40.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Add Service", color = Color.White, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SEARCH SCREEN
            AnimatedVisibility(
                visible = showSearchBar,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                SearchScreen(
                    allServices = allServices,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    selectedCategory = selectedCategory,
                    onSelectedCategoryChange = { selectedCategory = it },
                    predefinedCategories = predefinedCategories,
                    onServiceClick = { serviceId ->
                        val intent = Intent(context, com.example.grindsphere.hustler.ServiceDetailActivity::class.java)
                        intent.putExtra("serviceId", serviceId)
                        context.startActivity(intent)
                    }
                )
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