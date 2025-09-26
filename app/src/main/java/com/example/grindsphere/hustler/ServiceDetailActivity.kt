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
import kotlin.math.roundToInt

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
    val currentUser = auth.currentUser

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

    var canReview by remember { mutableStateOf(false) }
    var userReview by remember { mutableStateOf<Review?>(null) }
    var showAllReviews by remember { mutableStateOf(false) }
    val displayedReviews = if (showAllReviews) reviews else reviews.take(3)

    // Load service and check if user can review
    LaunchedEffect(serviceId, currentUser?.uid) {
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
                    profilePicUrl = doc.getString("profilePicUrl") ?: ""

                    // Check if current user is NOT the service owner
                    canReview = currentUser?.uid != ownerUid && currentUser != null

                    // Load owner profile picture and name
                    firestore.collection("users").document(ownerUid).get()
                        .addOnSuccessListener { userDoc ->
                            ownerName = userDoc.getString("name") ?: "Service Provider"
                        }

                    // Check if current user has favorited this service
                    if (currentUser != null) {
                        firestore.collection("users").document(currentUser.uid).get()
                            .addOnSuccessListener { userDoc ->
                                val saved = userDoc.get("savedServices") as? List<String> ?: listOf()
                                isFavorite = saved.contains(serviceId)
                            }

                        // Check if user already has a review
                        checkUserReview(serviceId, currentUser.uid, firestore) { review ->
                            userReview = review
                            // Pre-fill the dialog if user has an existing review
                            review?.let {
                                userRating = it.rating.toFloat()
                                userComment = it.comment
                            }
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
                            rating = (doc.getLong("rating") ?: 0L).toInt(),
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
                        rating = averageRating.roundToInt(),
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

            // Reviews Section - UPDATED with pagination
            Text(
                text = "Reviews ($totalReviews)",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (canReview && userReview == null) {
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
            } else if (userReview != null && canReview) {
                // Show edit button if user has a review
                Button(
                    onClick = { showReviewDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Edit Your Review")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Reviews List - UPDATED with pagination
            if (reviews.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.StarOutline,
                            contentDescription = "No reviews",
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No reviews yet",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Be the first to review this service!",
                            color = Color.Gray.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    // Display limited reviews
                    displayedReviews.forEach { review ->
                        ReviewItem(
                            review = review,
                            isUserReview = review.userId == currentUser?.uid,
                            onDeleteReview = {
                                deleteReview(review.id, firestore, context)
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // "View All" or "Show Less" button
                    if (reviews.size > 3) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showAllReviews = !showAllReviews },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color(0xFF7F5A83)
                            ),
                            elevation = ButtonDefaults.buttonElevation(0.dp)
                        ) {
                            Text(
                                text = if (showAllReviews) "Show Less" else "View All Reviews ($totalReviews)",
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Average rating summary when showing all reviews
                    if (showAllReviews && reviews.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF7F5A83).copy(alpha = 0.1f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Overall Rating",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF7F5A83)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StarRating(
                                        rating = averageRating.roundToInt(),
                                        onRatingChange = {},
                                        interactive = false,
                                        starSize = 20.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = String.format(Locale.US, "%.1f", averageRating),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color(0xFF7F5A83)
                                    )
                                }
                                Text(
                                    text = "Based on $totalReviews reviews",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
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
                        // Create booking request
                        val currentUser = auth.currentUser
                        if (serviceId != null && currentUser != null) {
                            // Get customer name from Firestore
                            firestore.collection("users").document(currentUser.uid).get()
                                .addOnSuccessListener { userDoc ->
                                    val customerName = userDoc.getString("name") ?: "Customer"

                                    val bookingData = hashMapOf(
                                        "serviceId" to serviceId,
                                        "serviceName" to serviceName,
                                        "customerUid" to currentUser.uid,
                                        "customerName" to customerName,
                                        "hustlerUid" to ownerUid,
                                        "status" to "pending",
                                        "timestamp" to System.currentTimeMillis(),
                                        "message" to "I'm interested in your service!"
                                    )

                                    firestore.collection("bookingRequests").add(bookingData)
                                        .addOnSuccessListener { docRef ->
                                            Toast.makeText(context, "Connection request sent!", Toast.LENGTH_SHORT).show()

                                            // Also create a conversation for messaging
                                            val convoData = hashMapOf(
                                                "participants" to listOf(currentUser.uid, ownerUid),
                                                "timestamp" to System.currentTimeMillis(),
                                                "lastMessage" to "Connection request: $serviceName",
                                                "type" to "booking",
                                                "bookingId" to docRef.id,
                                                "serviceId" to serviceId
                                            )
                                            firestore.collection("conversations").add(convoData)
                                                .addOnSuccessListener { convoDoc ->
                                                    Toast.makeText(context, "You can now chat with the service provider", Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(context, "Failed to send request: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                        } else {
                            Toast.makeText(context, "Please log in to connect", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F5A83))
                ) {
                    Text("Connect")
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
            title = {
                Text(if (userReview == null) "Add Review" else "Edit Review")
            },
            text = {
                Column {
                    // Star Rating
                    Text("Rating:", modifier = Modifier.padding(bottom = 8.dp))
                    StarRating(
                        rating = userRating.roundToInt(),
                        onRatingChange = { userRating = it.toFloat() }
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

                                        val reviewData = hashMapOf(
                                            "serviceId" to serviceId,
                                            "userId" to currentUser.uid,
                                            "userName" to userName,
                                            "rating" to userRating.toInt(),
                                            "comment" to userComment,
                                            "timestamp" to System.currentTimeMillis()
                                        )

                                        if (userReview == null) {
                                            // Add new review
                                            firestore.collection("reviews").add(reviewData)
                                                .addOnSuccessListener {
                                                    Toast.makeText(context, "Review added!", Toast.LENGTH_SHORT).show()
                                                    showReviewDialog = false
                                                    userRating = 0f
                                                    userComment = ""
                                                }
                                                .addOnFailureListener {
                                                    Toast.makeText(context, "Failed to add review", Toast.LENGTH_SHORT).show()
                                                }
                                        } else {
                                            // Update existing review
                                            firestore.collection("reviews").document(userReview!!.id)
                                                .set(reviewData)
                                                .addOnSuccessListener {
                                                    Toast.makeText(context, "Review updated!", Toast.LENGTH_SHORT).show()
                                                    showReviewDialog = false
                                                }
                                                .addOnFailureListener {
                                                    Toast.makeText(context, "Failed to update review", Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                    }
                            }
                        } else {
                            Toast.makeText(context, "Please add a rating", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text(if (userReview == null) "Submit" else "Update")
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
fun ReviewItem(
    review: Review,
    isUserReview: Boolean = false,
    onDeleteReview: () -> Unit = {}
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with user info and rating
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // User avatar
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF7F5A83).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = review.userName.take(1).uppercase(),
                                color = Color(0xFF7F5A83),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = review.userName,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Rating and date
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StarRating(
                            rating = review.rating,
                            onRatingChange = {},
                            interactive = false,
                            starSize = 14.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatTimestamp(review.timestamp),
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }

                // Delete button (only for user's own reviews)
                if (isUserReview) {
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Comment
            Text(
                text = review.comment,
                color = Color.Black.copy(alpha = 0.8f),
                fontSize = 14.sp,
                lineHeight = 18.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },
            title = {
                Text("Delete Review")
            },
            text = {
                Text("Are you sure you want to delete your review? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteReview()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
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

// Add this helper function to format timestamp
fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return format.format(date)
}

// Helper function to check if user already has a review
private fun checkUserReview(
    serviceId: String,
    userId: String,
    firestore: FirebaseFirestore,
    onResult: (Review?) -> Unit
) {
    firestore.collection("reviews")
        .whereEqualTo("serviceId", serviceId)
        .whereEqualTo("userId", userId)
        .get()
        .addOnSuccessListener { snapshot ->
            val review = snapshot.documents.firstOrNull()?.let { doc ->
                Review(
                    id = doc.id,
                    serviceId = doc.getString("serviceId") ?: "",
                    userId = doc.getString("userId") ?: "",
                    userName = doc.getString("userName") ?: "",
                    rating = (doc.getLong("rating") ?: 0L).toInt(),
                    comment = doc.getString("comment") ?: "",
                    timestamp = doc.getLong("timestamp") ?: 0L
                )
            }
            onResult(review)
        }
        .addOnFailureListener {
            onResult(null)
        }
}

// Function to delete a review
private fun deleteReview(reviewId: String, firestore: FirebaseFirestore, context: android.content.Context) {
    firestore.collection("reviews").document(reviewId)
        .delete()
        .addOnSuccessListener {
            Toast.makeText(context, "Review deleted successfully", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Failed to delete review: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}