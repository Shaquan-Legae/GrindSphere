package com.example.grindsphere.hustler

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
class ServiceDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val serviceId = intent.getStringExtra("serviceId")
        setContent {
            ServiceDetailScreen(serviceId)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceDetailScreen(serviceId: String?) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    var serviceName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var bannerUrl by remember { mutableStateOf("") }
    var profilePicUrl by remember { mutableStateOf("") }
    var categories by remember { mutableStateOf(listOf<String>()) }
    var images by remember { mutableStateOf(listOf<String>()) }
    var ownerUid by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var isFavorite by remember { mutableStateOf(false) }
    var ownerName by remember { mutableStateOf("") }

    // Load service
    LaunchedEffect(serviceId) {
        if (!serviceId.isNullOrEmpty()) {
            firestore.collection("services").document(serviceId).get()
                .addOnSuccessListener { doc ->
                    serviceName = doc.getString("name") ?: ""
                    description = doc.getString("description") ?: ""
                    bannerUrl = doc.getString("banner") ?: ""
                    categories = doc.get("categories") as? List<String> ?: listOf()
                    images = doc.get("images") as? List<String> ?: listOf()
                    ownerUid = doc.getString("ownerUid") ?: ""
                    location = doc.getString("location") ?: ""

                    // Load owner profile picture and name
                    firestore.collection("users").document(ownerUid).get()
                        .addOnSuccessListener { userDoc ->
                            profilePicUrl = userDoc.getString("profilePicUrl") ?: ""
                            ownerName = userDoc.getString("name") ?: "Service Provider"
                        }

                    // Check if current user has favorited this service
                    val currentUser = auth.currentUser
                    if (currentUser != null) {
                        firestore.collection("users").document(currentUser.uid).get()
                            .addOnSuccessListener { userDoc ->
                                val saved = userDoc.get("savedServices") as? List<String> ?: listOf()
                                isFavorite = saved.contains(serviceId)
                            }
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(serviceName) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF7F5A83)),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // YouTube-style Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                if (bannerUrl.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(bannerUrl),
                        contentDescription = "Service Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF7F5A83)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = serviceName,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Profile Picture (YouTube channel style - centered over banner)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 40.dp) // Half outside the banner
                ) {
                    if (profilePicUrl.isNotEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(profilePicUrl),
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color.White, CircleShape)
                                .padding(4.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color.Gray, CircleShape)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile Placeholder",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp)) // Space for the profile picture

            // Service Name and Owner Info
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = serviceName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "by $ownerName",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Location
                if (location.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = Color(0xFF7F5A83),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = location,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Categories
            if (categories.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        Text(
                            text = "#$cat",
                            color = Color(0xFF7F5A83),
                            fontSize = 12.sp,
                            modifier = Modifier
                                .background(Color(0xFF7F5A83).copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Description
            Text(
                text = "About",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description.ifEmpty { "No description provided." },
                modifier = Modifier.padding(horizontal = 16.dp),
                fontSize = 16.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Images Carousel
            if (images.isNotEmpty()) {
                Text(
                    text = "Gallery",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(images) { imgUrl ->
                        Image(
                            painter = rememberAsyncImagePainter(imgUrl),
                            contentDescription = null,
                            modifier = Modifier
                                .size(150.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Action Buttons Row (YouTube-style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        // Book Now logic
                        Toast.makeText(context, "Booking request sent!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F5A83))
                ) {
                    Text("Book Now")
                }

                // Favorite Button with Star Icon
                IconButton(
                    onClick = {
                        // Toggle favorite
                        val userRef = firestore.collection("users").document(auth.currentUser!!.uid)
                        userRef.get().addOnSuccessListener { doc ->
                            val saved = doc.get("savedServices") as? MutableList<String> ?: mutableListOf()
                            if (isFavorite) {
                                saved.remove(serviceId)
                            } else {
                                if (serviceId != null) {
                                    saved.add(serviceId)
                                }
                            }
                            userRef.update("savedServices", saved)
                            isFavorite = !isFavorite

                            val message = if (isFavorite) "Added to favorites" else "Removed from favorites"
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            if (isFavorite) Color(0xFFFFD700).copy(alpha = 0.2f)
                            else Color.Gray.copy(alpha = 0.2f),
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarOutline,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color(0xFFFFD700) else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}