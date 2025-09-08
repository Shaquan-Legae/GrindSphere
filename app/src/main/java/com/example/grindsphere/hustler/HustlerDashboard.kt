package com.example.grindsphere.hustler

import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.grindsphere.LoginActivity
import com.example.grindsphere.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class ServiceCard(
    val id: String,
    val name: String,
    val imageUrl: String,
    val views: Long = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HustlerDashboard() {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    var hustlerName by remember { mutableStateOf("Hustler") }
    var profilePicUrl by remember { mutableStateOf("") }
    var services by remember { mutableStateOf(listOf<ServiceCard>()) }
    var totalViews by remember { mutableStateOf(0L) }
    var showMenu by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(3) } // Default Home
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showMessagesScreen by remember { mutableStateOf(false) } // Track MessagesScreen visibility

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
                            imageUrl = doc.getString("imageUrl") ?: "",
                            views = doc.getLong("views") ?: 0L
                        )
                    } ?: listOf()
                    services = list
                    totalViews = list.sumOf { it.views }
                }
        }
    }

    val filteredServices = if (searchQuery.isBlank()) services else services.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard", color = Color.White) },
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
                    onClick = { selectedTab = 3; showSearchBar = false; showMessagesScreen = false },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2; showSearchBar = true; showMessagesScreen = false },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    label = { Text("Search") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1; showMessagesScreen = true },
                    icon = { Icon(Icons.Default.MailOutline, contentDescription = "Messages") },
                    label = { Text("Messages") }
                )
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0; showMessagesScreen = false },
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
            if (showMessagesScreen) {
                // MessagesScreen integration
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
            } else {
                // MAIN DASHBOARD CONTENT
                Column(modifier = Modifier.padding(16.dp)) {
                    // Welcome + Profile
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (profilePicUrl.isNotEmpty()) {
                            Image(
                                painter = painterResource(id = R.drawable.profilep),
                                contentDescription = "Profile Picture",
                                modifier = Modifier.size(60.dp).clip(CircleShape)
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.profilep),
                                contentDescription = "Profile Placeholder",
                                modifier = Modifier.size(60.dp).clip(CircleShape)
                            )
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

                    // Stats
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
                            Text("Total Views: $totalViews", color = Color.White, fontSize = 16.sp)
                            Text("Active Services: ${services.size}", color = Color.White, fontSize = 16.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Your Services", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(filteredServices) { service ->
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
                                    painter = painterResource(id = R.drawable.profilep),
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

            // MODERN SEARCH OVERLAY
            AnimatedVisibility(
                visible = showSearchBar,
                enter = slideInVertically(
                    initialOffsetY = { -200 },
                    animationSpec = tween(durationMillis = 400)
                ) + fadeIn(animationSpec = tween(400)),
                exit = slideOutVertically(
                    targetOffsetY = { -200 },
                    animationSpec = tween(durationMillis = 400)
                ) + fadeOut(animationSpec = tween(400))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable { showSearchBar = false } // dismiss when tapping outside
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search Services") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                            .align(Alignment.TopCenter)
                            .background(Color.White, RoundedCornerShape(24.dp))
                            .shadow(8.dp, RoundedCornerShape(24.dp)),
                        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                    )
                }
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
