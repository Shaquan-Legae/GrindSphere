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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    var isFavorite by remember { mutableStateOf(false) }

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

                    // Load owner profile picture
                    firestore.collection("users").document(ownerUid).get()
                        .addOnSuccessListener { userDoc ->
                            profilePicUrl = userDoc.getString("profilePicUrl") ?: ""
                        }

                    // Check if current user has favorited this service
                    firestore.collection("users").document(auth.currentUser!!.uid).get()
                        .addOnSuccessListener { userDoc ->
                            val saved = userDoc.get("savedServices") as? List<String> ?: listOf()
                            isFavorite = saved.contains(serviceId)
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
            // Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                if (bannerUrl.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(bannerUrl),
                        contentDescription = "Service Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Gray))
                }

                // Profile Picture
                if (profilePicUrl.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(profilePicUrl),
                        contentDescription = "Hustler Profile",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .align(Alignment.BottomStart)
                            .offset(16.dp, 30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Service Name
            Text(
                text = serviceName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Categories
            Row(modifier = Modifier
                .padding(horizontal = 16.dp)
                .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    Text(
                        text = cat,
                        color = Color.White,
                        modifier = Modifier
                            .background(Color(0xFF7F5A83), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            Text(
                text = description,
                modifier = Modifier.padding(horizontal = 16.dp),
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Images Carousel
            Text("Gallery", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                items(images) { imgUrl ->
                    Image(
                        painter = rememberAsyncImagePainter(imgUrl),
                        contentDescription = null,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Buttons Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        // TODO: Book Now logic → create a message request
                        Toast.makeText(context, "Booking request sent!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Book Now")
                }

                Button(
                    onClick = {
                        // Toggle favorite
                        val userRef = firestore.collection("users").document(auth.currentUser!!.uid)
                        userRef.get().addOnSuccessListener { doc ->
                            val saved = doc.get("savedServices") as? MutableList<String> ?: mutableListOf()
                            if (isFavorite) saved.remove(serviceId)
                            else saved.add(serviceId!!)
                            userRef.update("savedServices", saved)
                            isFavorite = !isFavorite
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isFavorite) Color.Red else Color.Gray),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isFavorite) "Saved" else "Save")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
