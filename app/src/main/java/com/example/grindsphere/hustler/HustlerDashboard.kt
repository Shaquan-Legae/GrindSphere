package com.example.grindsphere.hustler

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.grindsphere.LoginActivity
import com.example.grindsphere.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HustlerDashboard() {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val currentUser = auth.currentUser

    var hustlerName by remember { mutableStateOf("Hustler") }
    var profilePicUrl by remember { mutableStateOf("") }
    var services by remember { mutableStateOf(listOf<ServiceCard>()) }
    var totalBookings by remember { mutableStateOf(0L) }
    var showMenu by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(3) } // Start with Home
    var showMessagesScreen by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) } // Control search bar visibility

    // Permission launcher for storage access
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Storage permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Image picker launcher for profile picture
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val userId = currentUser?.uid ?: return@let
            val storageRef = storage.reference.child("profile_pics/$userId.jpg")
            storageRef.putFile(uri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        firestore.collection("users").document(userId)
                            .update("profilePicUrl", downloadUrl.toString())
                            .addOnSuccessListener {
                                profilePicUrl = downloadUrl.toString()
                                Toast.makeText(context, "Profile picture updated", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Fetch user profile
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    hustlerName = doc.getString("name") ?: "Hustler"
                    profilePicUrl = doc.getString("profilePicUrl") ?: ""
                }
        }
    }

    // Fetch services
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
                        ServiceCard(
                            id = doc.id,
                            name = doc.getString("name") ?: "Service",
                            bannerUrl = doc.getString("banner") ?: "",
                            bookings = doc.getLong("bookings") ?: 0L,
                            categories = doc.get("categories") as? List<String> ?: listOf()
                        )
                    } ?: listOf()
                    services = list
                    totalBookings = list.sumOf { it.bookings }
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
                            else -> "Dashboard"
                        },
                        color = Color.White
                    )
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
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
                        showMessagesScreen = false
                        showSearchBar = false // Hide search bar on Home
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        showMessagesScreen = false
                        showSearchBar = true // Show search bar on Search
                    },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    label = { Text("Search") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        showMessagesScreen = true
                        showSearchBar = false // Hide search bar on Messages
                    },
                    icon = { Icon(Icons.Default.MailOutline, contentDescription = "Messages") },
                    label = { Text("Messages") }
                )
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        showMessagesScreen = false
                        showSearchBar = false // Hide search bar on Profile
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
                selectedTab == 2 || selectedTab == 3 -> {
                    HomePageScreen(showSearchBar = showSearchBar)
                }
                else -> {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        permissionLauncher.launch(
                                            if (android.os.Build.VERSION.SDK_INT >= 33)
                                                Manifest.permission.READ_MEDIA_IMAGES
                                            else
                                                Manifest.permission.READ_EXTERNAL_STORAGE
                                        )
                                        imagePickerLauncher.launch("image/*")
                                    }
                            ) {
                                if (profilePicUrl.isNotEmpty()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(
                                            model = profilePicUrl,
                                            error = painterResource(id = R.drawable.profilep)
                                        ),
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = R.drawable.profilep),
                                        contentDescription = "Profile Placeholder",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Welcome back, $hustlerName 👋",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Your Stats", color = Color.White, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Total Bookings: $totalBookings", color = Color.White, fontSize = 16.sp)
                                Text("Active Services: ${services.size}", color = Color.White, fontSize = 16.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text("Your Services", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(services) { service ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .padding(12.dp)
                                        .width(100.dp)
                                        .clickable {
                                            val intent = Intent(context, EditServiceActivity::class.java)
                                            intent.putExtra("serviceId", service.id)
                                            context.startActivity(intent)
                                        }
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(
                                            model = service.bannerUrl,
                                            error = painterResource(id = R.drawable.profilep)
                                        ),
                                        contentDescription = service.name,
                                        modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp))
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(service.name, color = Color.White, fontSize = 14.sp)
                                }
                            }

                            item {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .padding(12.dp)
                                        .width(100.dp)
                                        .clickable {
                                            val intent = Intent(context, EditServiceActivity::class.java)
                                            context.startActivity(intent)
                                        }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Service", tint = Color.White, modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Add", color = Color.White, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}