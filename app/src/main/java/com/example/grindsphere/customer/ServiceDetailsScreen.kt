package com.example.grindsphere.customer

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.grindsphere.models.Review
import com.example.grindsphere.models.Service
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceDetailsScreen(
    serviceId: String,
    navController: NavHostController
) {
    var service by remember { mutableStateOf<Service?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var reviews by remember { mutableStateOf<List<Review>>(emptyList()) }
    var averageRating by remember { mutableStateOf(0f) }
    var showReviewDialog by remember { mutableStateOf(false) }
    var userRating by remember { mutableStateOf(0) }
    var userComment by remember { mutableStateOf("") }
    var userReview by remember { mutableStateOf<Review?>(null) }
    var showAllReviews by remember { mutableStateOf(false) }
    var currentUserName by remember { mutableStateOf("") }
    var canReview by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = auth.currentUser?.uid

    // Debug info
    LaunchedEffect(currentUserId, service, userReview) {
        println("DEBUG: currentUserId = $currentUserId")
        println("DEBUG: service ownerUid = ${service?.ownerUid}")
        println("DEBUG: userReview = $userReview")
        println("DEBUG: canReview = $canReview")

        // Update canReview condition
        canReview = currentUserId != null &&
                currentUserId != service?.ownerUid &&
                userReview == null
        println("DEBUG: Updated canReview = $canReview")
    }

    // Fetch current user's name from Firestore
    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            firestore.collection("users").document(currentUserId).get()
                .addOnSuccessListener { doc ->
                    currentUserName = doc.getString("name") ?: "Customer"
                    println("DEBUG: Loaded user name: $currentUserName")
                }
                .addOnFailureListener {
                    currentUserName = "Customer"
                }
        }
    }

    // Fetch service
    LaunchedEffect(serviceId) {
        firestore.collection("services")
            .document(serviceId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    service = doc.toObject<Service>()?.copy(id = doc.id)
                    println("DEBUG: Loaded service: ${service?.name}")
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    // Set up real-time listener for reviews
    LaunchedEffect(serviceId) {
        val reviewsListener = firestore.collection("reviews")
            .whereEqualTo("serviceId", serviceId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("DEBUG: Error loading reviews: ${error.message}")
                    return@addSnapshotListener
                }

                val updatedReviews = snapshot?.documents?.mapNotNull { doc ->
                    Review(
                        id = doc.id,
                        serviceId = doc.getString("serviceId") ?: "",
                        userId = doc.getString("userId") ?: "",
                        userName = doc.getString("userName") ?: "",
                        rating = (doc.getLong("rating") ?: 0L).toInt(),
                        comment = doc.getString("comment") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                } ?: emptyList()

                println("DEBUG: Loaded ${updatedReviews.size} reviews")
                reviews = updatedReviews

                // Calculate average rating
                if (updatedReviews.isNotEmpty()) {
                    averageRating = updatedReviews.map { it.rating }.average().toFloat()
                } else {
                    averageRating = 0f
                }

                // Check if current user has a review
                if (currentUserId != null) {
                    userReview = updatedReviews.find { it.userId == currentUserId }
                    userReview?.let {
                        userRating = it.rating
                        userComment = it.comment
                        println("DEBUG: Found user review with rating: ${it.rating}")
                    }

                    // Update canReview after loading reviews
                    canReview = currentUserId != service?.ownerUid && userReview == null
                    println("DEBUG: Final canReview after reviews loaded = $canReview")
                }
            }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    service?.let { srv ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(srv.name, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF7F5A83))
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .background(Brush.verticalGradient(listOf(Color(0xFF0D324D), Color(0xFF7F5A83))))
            ) {
                // Banner image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                ) {
                    if (srv.banner.isNotBlank()) {
                        Image(
                            painter = rememberAsyncImagePainter(srv.banner),
                            contentDescription = "Service banner",
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
                            Icon(
                                Icons.Default.Storefront,
                                contentDescription = "Service",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    // Profile picture overlay (YouTube style)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 40.dp)
                    ) {
                        if (srv.profilePicUrl.isNotBlank()) {
                            Image(
                                painter = rememberAsyncImagePainter(srv.profilePicUrl),
                                contentDescription = "Provider profile",
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
                                    Icons.Default.Person,
                                    contentDescription = "Profile",
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Service info
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        srv.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "by ${srv.ownerName}",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Rating and reviews
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StarRating(
                            rating = averageRating.roundToInt(),
                            onRatingChange = {},
                            interactive = false,
                            starSize = 20.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "${reviews.size} reviews",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Location
                    if (srv.location.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = "Location",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(srv.location, color = Color.White.copy(alpha = 0.8f))
                        }
                    }

                    // Price
                    if (srv.price > 0.0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "R${String.format(Locale.US, "%.2f", srv.price)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }
                }

                // Categories
                if (srv.categories.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        srv.categories.forEach { category ->
                            Text(
                                text = "#$category",
                                color = Color(0xFFFFD700),
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .background(Color(0xFFFFD700).copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Description
                Text(
                    "About",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    srv.description,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Gallery Images
                if (srv.images.isNotEmpty() && srv.images.any { it.isNotBlank() }) {
                    Text(
                        "Gallery",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(srv.images.filter { it.isNotBlank() }) { imageUrl ->
                            Image(
                                painter = rememberAsyncImagePainter(imageUrl),
                                contentDescription = "Service image",
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
                    "Reviews (${reviews.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // SIMPLIFIED: Always show Add Review button if user is logged in and not the owner
                // We'll handle the "already reviewed" case in the dialog
                if (currentUserId != null && currentUserId != srv.ownerUid) {
                    Button(
                        onClick = {
                            if (userReview != null) {
                                // User already has a review - show edit option
                                Toast.makeText(context, "You've already reviewed this service", Toast.LENGTH_SHORT).show()
                                // Optionally, you could open the dialog with their existing review pre-filled
                                userRating = userReview!!.rating
                                userComment = userReview!!.comment
                                showReviewDialog = true
                            } else {
                                // New review
                                userRating = 0
                                userComment = ""
                                showReviewDialog = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))
                    ) {
                        Text(
                            if (userReview != null) "Edit Your Review" else "Add Review",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                } else if (currentUserId == null) {
                    // User not logged in
                    Button(
                        onClick = {
                            Toast.makeText(context, "Please log in to leave a review", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F5A83))
                    ) {
                        Text("Log In to Review", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Reviews List
                val displayedReviews = if (showAllReviews) reviews else reviews.take(3)

                if (reviews.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.StarOutline,
                                contentDescription = "No reviews",
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No reviews yet",
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                "Be the first to review this service!",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        displayedReviews.forEach { review ->
                            ReviewItem(review = review)
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        if (reviews.size > 3) {
                            Button(
                                onClick = { showAllReviews = !showAllReviews },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color(0xFFFFD700)
                                ),
                                elevation = ButtonDefaults.buttonElevation(0.dp)
                            ) {
                                Text(if (showAllReviews) "Show Less" else "View All Reviews")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Booking Section
                Text(
                    "Book This Service",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (currentUserId == null) {
                                Toast.makeText(context, "Please log in to book services", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val booking = com.example.grindsphere.models.Booking(
                                serviceId = srv.id,
                                serviceName = srv.name,
                                customerId = currentUserId,
                                customerName = currentUserName,
                                hustlerId = srv.ownerUid,
                                hustlerName = srv.ownerName,
                                message = "I'm interested in your service!",
                                price = srv.price
                            )
                            firestore.collection("bookingRequests")
                                .add(booking)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Booking requested!", Toast.LENGTH_SHORT).show()
                                    navController.navigate("bookings")
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Failed to book: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))
                    ) {
                        Text("Book Now", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            if (currentUserId == null) {
                                Toast.makeText(context, "Please log in to message providers", Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }

                            if (srv.ownerUid.isEmpty()) return@OutlinedButton

                            firestore.collection("conversations")
                                .whereEqualTo("serviceId", srv.id)
                                .whereArrayContains("participants", currentUserId)
                                .get()
                                .addOnSuccessListener { querySnapshot ->
                                    if (!querySnapshot.isEmpty) {
                                        val conversationId = querySnapshot.documents.first().id
                                        navController.navigate("chat/$conversationId")
                                    } else {
                                        val newConversation = com.example.grindsphere.models.Conversation(
                                            participants = listOf(currentUserId, srv.ownerUid),
                                            participantNames = mapOf(
                                                currentUserId to currentUserName,
                                                srv.ownerUid to srv.ownerName
                                            ),
                                            serviceId = srv.id,
                                            serviceName = srv.name,
                                            lastMessage = "Chat started..."
                                        )
                                        firestore.collection("conversations")
                                            .add(newConversation)
                                            .addOnSuccessListener { docRef ->
                                                navController.navigate("chat/${docRef.id}")
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(context, "Failed to start conversation: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Failed to find conversation: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFFD700)
                        )
                    ) {
                        Text("Message")
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Review Dialog
        if (showReviewDialog) {
            AlertDialog(
                onDismissRequest = {
                    showReviewDialog = false
                    userRating = 0
                    userComment = ""
                },
                title = {
                    Text(
                        if (userReview != null) "Edit Your Review" else "Add Review",
                        color = Color.Black
                    )
                },
                text = {
                    Column {
                        Text("Rating:", color = Color.Black, modifier = Modifier.padding(bottom = 8.dp))
                        StarRating(
                            rating = userRating,
                            onRatingChange = { newRating ->
                                userRating = newRating
                            },
                            interactive = true,
                            starSize = 32.dp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "Comment:",
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        TextField(
                            value = userComment,
                            onValueChange = { userComment = it },
                            placeholder = { Text("Share your experience...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            maxLines = 4
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (userRating == 0) {
                                Toast.makeText(context, "Please select a rating", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            if (currentUserId == null) {
                                Toast.makeText(context, "Please log in to leave a review", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val reviewData = hashMapOf(
                                "serviceId" to serviceId,
                                "userId" to currentUserId,
                                "userName" to currentUserName,
                                "rating" to userRating,
                                "comment" to userComment,
                                "timestamp" to System.currentTimeMillis()
                            )

                            if (userReview != null) {
                                // Update existing review
                                firestore.collection("reviews").document(userReview!!.id)
                                    .set(reviewData)
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Review updated successfully!", Toast.LENGTH_SHORT).show()
                                        showReviewDialog = false
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context, "Failed to update review: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                // Add new review
                                firestore.collection("reviews").add(reviewData)
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Review added successfully!", Toast.LENGTH_SHORT).show()
                                        showReviewDialog = false
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context, "Failed to add review: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F5A83))
                    ) {
                        Text(
                            if (userReview != null) "Update Review" else "Submit Review",
                            color = Color.White
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showReviewDialog = false
                            userRating = 0
                            userComment = ""
                        }
                    ) {
                        Text("Cancel", color = Color(0xFF7F5A83))
                    }
                }
            )
        }
    } ?: Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Service not found.", color = Color.White)
    }
}

@Composable
fun ReviewItem(review: Review) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    review.userName,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )

                StarRating(
                    rating = review.rating,
                    onRatingChange = {},
                    interactive = false,
                    starSize = 16.dp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                review.comment,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                formatTimestamp(review.timestamp),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
    }
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
                tint = if (i <= rating) Color(0xFFFFD700) else Color.Gray.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(starSize)
                    .clickable(enabled = interactive) {
                        onRatingChange(i)
                    }
            )
        }
    }
}

// Helper function to format timestamp
fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return format.format(date)
}