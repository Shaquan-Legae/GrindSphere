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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    var reviews by remember { mutableStateOf(listOf<Review>()) }
    var showReviewDialog by remember { mutableStateOf(false) }
    var userRating by remember { mutableStateOf(0f) }
    var userComment by remember { mutableStateOf("") }
    var averageRating by remember { mutableStateOf(0f) }
    var totalReviews by remember { mutableStateOf(0) }

    // Load service
    LaunchedEffect(serviceId) {
        if (!serviceId.isNullOrEmpty()) {
            firestore.collection("services").document(serviceId).get()
                .addOnSuccessListener { doc ->
                    serviceName = doc.getString("name") ?: ""
                    description = doc.getString("description") ?: ""
                    bannerUrl = doc.getString("bannerUrl") ?: ""
                    categories = doc.get("categories") as? List<String> ?: listOf()
                    images = doc.get("images") as? List<String> ?: listOf()
                    ownerUid = doc.getString("ownerUid") ?: ""
                    location = doc.getString("location") ?: ""
                    profilePicUrl = doc.getString("profilePicUrl") ?: ""

                    // Load owner profile picture and name
                    firestore.collection("users").document(ownerUid).get()
                        .addOnSuccessListener { userDoc ->
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

    // Load reviews
    LaunchedEffect(serviceId) {
        if (!serviceId.isNullOrEmpty()) {
            firestore.collection("reviews")
                .whereEqualTo("serviceId", serviceId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener

                    reviews = snapshot?.documents?.map { doc ->
                        Review(
                            id = doc.id,
                            serviceId = doc.getString("serviceId") ?: "",
                            userId = doc.getString("userId") ?: "",
                            userName = doc.getString("userName") ?: "",
                            rating = (doc.getLong("rating") ?: 0.0).toInt(),
                            comment = doc.getString("comment") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L
                        )
                    } ?: listOf()

                    // Calculate average rating
                    if (reviews.isNotEmpty()) {
                        averageRating = reviews.map { it.rating }.average().toFloat()
                        totalReviews = reviews.size
                    } else {
                        averageRating = 0f
                        totalReviews = 0
                    }
                }
        }
    }

    // Increment view count when service is viewed
    LaunchedEffect(serviceId) {
        if (!serviceId.isNullOrEmpty()) {
            try {
                firestore.collection("services").document(serviceId)
                    .update("views", FieldValue.increment(1))
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(serviceName) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            // Go back to previous screen
                            if (context is ComponentActivity) {
                                (context as ComponentActivity).finish()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
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

                // Rating and Reviews
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    StarRating(
                        rating = averageRating,
                        onRatingChange = {},
                        interactive = false,
                        starSize = 16.dp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "(${totalReviews} reviews)",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

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

            // Reviews Section
            Text(
                text = "Reviews",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Add Review Button
            Button(
                onClick = { showReviewDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F5A83))
            ) {
                Text("Add Review")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reviews List
            if (reviews.isEmpty()) {
                Text(
                    text = "No reviews yet. Be the first to review!",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
            } else {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    reviews.forEach { review ->
                        ReviewItem(review = review)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons Row (YouTube-style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        // Implement proper booking logic
                        val currentUserId = auth.currentUser?.uid
                        if (currentUserId != null && serviceId != null) {
                            val booking = hashMapOf(
                                "serviceId" to serviceId,
                                "customerId" to currentUserId,
                                "serviceOwnerId" to ownerUid,
                                "serviceName" to serviceName,
                                "status" to "pending",
                                "timestamp" to System.currentTimeMillis(),
                                "customerName" to (auth.currentUser?.displayName ?: "Customer")
                            )
                            
                            firestore.collection("bookings").add(booking)
                                .addOnSuccessListener {
                                    // Update booking count
                                    firestore.collection("services").document(serviceId)
                                        .update("bookings", FieldValue.increment(1))
                                    Toast.makeText(context, "Booking request sent successfully!", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Failed to send booking request", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(context, "Please log in to book this service", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F5A83))
                ) {
                    Text("Book Now")
                }

                // Message Button
                Button(
                    onClick = {
                        val currentUserId = auth.currentUser?.uid
                        if (currentUserId != null && ownerUid.isNotEmpty() && currentUserId != ownerUid) {
                            // Create conversation ID (sorted to ensure consistency)
                            val participants = listOf(currentUserId, ownerUid).sorted()
                            val conversationId = participants.joinToString("_")
                            
                            // Navigate to chat screen
                            val intent = Intent(context, ChatScreenActivity::class.java).apply {
                                putExtra("conversationId", conversationId)
                                putExtra("customerUid", ownerUid)
                                putExtra("customerName", ownerName)
                            }
                            context.startActivity(intent)
                        } else if (currentUserId == ownerUid) {
                            Toast.makeText(context, "You can't message yourself", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Please log in to send messages", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D324D))
                ) {
                    Text("Message")
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

    // Review Dialog
    if (showReviewDialog) {
        AlertDialog(
            onDismissRequest = { showReviewDialog = false },
            title = { Text("Add Review") },
            text = {
                Column {
                    // Star Rating
                    Text("Rating:", modifier = Modifier.padding(bottom = 8.dp))
                    StarRating(
                        rating = userRating,
                        onRatingChange = { userRating = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Comment
                    TextField(
                        value = userComment,
                        onValueChange = { userComment = it },
                        label = { Text("Comment") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (userRating > 0 && serviceId != null) {
                            val currentUser = auth.currentUser
                            if (currentUser != null) {
                                // Get user name
                                firestore.collection("users").document(currentUser.uid).get()
                                    .addOnSuccessListener { userDoc ->
                                        val userName = userDoc.getString("name") ?: "User"

                                        val review = hashMapOf(
                                            "serviceId" to serviceId,
                                            "userId" to currentUser.uid,
                                            "userName" to userName,
                                            "rating" to userRating.toLong(),
                                            "comment" to userComment,
                                            "timestamp" to System.currentTimeMillis()
                                        )

                                        firestore.collection("reviews").add(review)
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "Review added!", Toast.LENGTH_SHORT).show()
                                                showReviewDialog = false
                                                userRating = 0f
                                                userComment = ""
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(context, "Failed to add review", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                            }
                        } else {
                            Toast.makeText(context, "Please add a rating", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Submit")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showReviewDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ReviewItem(review: Review) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f)), // Increased opacity
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // User name and rating
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = review.userName,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                StarRating(
                    rating = review.rating,
                    onRatingChange = {},
                    interactive = false,
                    starSize = 16.dp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Comment - show full comment without truncation
            if (review.comment.isNotEmpty()) {
                Text(
                    text = review.comment,
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Timestamp
            Text(
                text = formatTimestamp(review.timestamp),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
    }
}

// Add this helper function to format timestamp
fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return format.format(date)
}

@Composable
fun StarRating(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    interactive: Boolean = true,
    starSize: Dp = 24.dp
) {
    Row {
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarOutline,
                contentDescription = "Star $i",
                tint = if (i <= rating) Color(0xFFFFD700) else Color.Gray,
                modifier = Modifier
                    .size(starSize)
                    .clickable(enabled = interactive) {
                        onRatingChange(i)
                    }
            )
        }
    }
}
