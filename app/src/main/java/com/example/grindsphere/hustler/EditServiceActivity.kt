package com.example.grindsphere.hustler

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

class EditServiceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val serviceId = intent.getStringExtra("serviceId")
        setContent {
            EditServiceScreen(serviceId = serviceId)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditServiceScreen(serviceId: String?) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val coroutineScope = rememberCoroutineScope()

    // Form state
    var serviceName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    // Images state
    var existingImageUrls by remember { mutableStateOf(listOf<String>()) } // stored urls
    var imageUris by remember { mutableStateOf(listOf<Uri>()) } // newly picked URIs
    var existingProfilePicUrl by remember { mutableStateOf("") }
    var selectedProfilePicUri by remember { mutableStateOf<Uri?>(null) }

    // categories
    val categoriesList = listOf(
        "Tutoring", "Design", "Tech Support", "Photography",
        "Fashion", "Food", "Music", "Fitness", "Transport", "Nails", "Hair", "Beauty", "Cake"
    )
    var selectedCategories by remember { mutableStateOf(listOf<String>()) }

    // other states
    var loading by remember { mutableStateOf(false) }
    var imagesUploading by remember { mutableStateOf(false) }
    var existingBookings by remember { mutableStateOf(0L) }
    var existingViews by remember { mutableStateOf(0L) }

    // Combined display images (strings) for preview
    val displayImages: List<String> = remember(existingImageUrls, imageUris) {
        existingImageUrls + imageUris.map { it.toString() }
    }

    // --- Load existing service if editing ---
    LaunchedEffect(serviceId) {
        if (!serviceId.isNullOrEmpty()) {
            loading = true
            try {
                val docSnap = firestore.collection("services").document(serviceId).get().await()
                if (docSnap.exists()) {
                    serviceName = docSnap.getString("name") ?: ""
                    description = docSnap.getString("description") ?: ""
                    location = docSnap.getString("location") ?: ""
                    existingImageUrls = (docSnap.get("images") as? List<*>)?.mapNotNull { it as? String } ?: listOf()
                    existingProfilePicUrl = docSnap.getString("profilePicUrl") ?: ""
                    selectedCategories = (docSnap.get("categories") as? List<*>)?.mapNotNull { it as? String } ?: listOf()
                    existingBookings = docSnap.getLong("bookings") ?: 0L
                    existingViews = docSnap.getLong("views") ?: 0L
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load service: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                loading = false
            }
        }
    }

    // --- Pickers ---
    val pickProfileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { selectedProfilePicUri = it }
    }

    val pickImagesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            imageUris = imageUris + uris
        }
    }

    // --- Helpers for uploading ---
    suspend fun uploadFileAndGetUrl(path: String, uri: Uri): String {
        val ref = storage.reference.child(path)
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun uploadProfilePicIfNeeded(): String {
        // If user picked a new profile pic, upload it; otherwise keep existing URL (could be empty)
        return if (selectedProfilePicUri != null) {
            val path = "profilePics/${auth.currentUser!!.uid}/${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
            uploadFileAndGetUrl(path, selectedProfilePicUri!!)
        } else {
            existingProfilePicUrl
        }
    }

    suspend fun uploadImagesAndGetUrls(uris: List<Uri>): List<String> {
        if (uris.isEmpty()) return listOf()
        imagesUploading = true
        val uploaded = mutableListOf<String>()
        try {
            for (uri in uris) {
                val path = "serviceImages/${auth.currentUser!!.uid}/${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
                val url = uploadFileAndGetUrl(path, uri)
                uploaded.add(url)
            }
        } finally {
            imagesUploading = false
        }
        return uploaded
    }

    // --- UI ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (serviceId != null) "Edit Service" else "Add Service") },
                navigationIcon = {
                    IconButton(onClick = {
                        // go back to Hustler Dashboard
                        val intent = Intent(context, HustlerDashboardActivity::class.java)
                        context.startActivity(intent)
                        if (context is EditServiceActivity) context.finish()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                return@Box
            }

            Column(modifier = Modifier.fillMaxSize()) {
                // Banner preview (first image)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            // tapping banner can open image picker to add images (the first image becomes banner)
                            pickImagesLauncher.launch("image/*")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (displayImages.isNotEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(displayImages[0]),
                            contentDescription = "Banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text("Tap to add banner (pick images)", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Profile picture row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(36.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { pickProfileLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            selectedProfilePicUri != null -> {
                                Image(
                                    painter = rememberAsyncImagePainter(selectedProfilePicUri),
                                    contentDescription = "Profile",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            existingProfilePicUrl.isNotEmpty() -> {
                                Image(
                                    painter = rememberAsyncImagePainter(existingProfilePicUrl),
                                    contentDescription = "Profile",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            else -> {
                                Text("Upload\nProfile", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        TextField(
                            value = serviceName,
                            onValueChange = { serviceName = it },
                            label = { Text("Service Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text("Location") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Images row (thumbnails)
                Text("Images (tap to remove)", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(displayImages) { img ->
                        Box(modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                // If it's an existing URL, remove from existingImageUrls
                                if (existingImageUrls.contains(img)) {
                                    existingImageUrls = existingImageUrls - img
                                } else {
                                    // else it represents a new picked URI string -> remove from imageUris
                                    val toRemove = imageUris.find { it.toString() == img }
                                    toRemove?.let { imageUris = imageUris - it }
                                }
                            }
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(img),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { pickImagesLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Categories chips (horizontal scroll)
                Text("Categories", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (cat in categoriesList) {
                        val selected = selectedCategories.contains(cat)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            tonalElevation = if (selected) 6.dp else 0.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .clickable {
                                    selectedCategories = if (selected) selectedCategories - cat else selectedCategories + cat
                                }
                        ) {
                            Text(
                                text = cat,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Save button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            if (serviceName.isBlank() || description.isBlank()) {
                                Toast.makeText(context, "Please fill name & description", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            loading = true
                            try {
                                // 1) Upload profile pic if replaced
                                val profileUrl = uploadProfilePicIfNeeded()

                                // 2) Upload new service images and combine with existing
                                val newUploaded = uploadImagesAndGetUrls(imageUris)
                                val finalImageUrls = existingImageUrls + newUploaded
                                val bannerUrl = finalImageUrls.firstOrNull() ?: ""

                                // 3) Prepare data
                                val data = hashMapOf(
                                    "name" to serviceName,
                                    "description" to description,
                                    "location" to location,
                                    "bannerUrl" to bannerUrl,
                                    "profilePicUrl" to profileUrl,
                                    "images" to finalImageUrls,
                                    "categories" to selectedCategories,
                                    "ownerUid" to auth.currentUser!!.uid,
                                    "bookings" to existingBookings,
                                    "views" to existingViews
                                ) as MutableMap<String, Any?>

                                // 4) Write to Firestore
                                if (!serviceId.isNullOrEmpty()) {
                                    // Update existing doc (set will overwrite - it's fine since we included all fields)
                                    firestore.collection("services").document(serviceId).set(data).await()
                                    Toast.makeText(context, "Service updated", Toast.LENGTH_SHORT).show()
                                } else {
                                    firestore.collection("services").add(data).await()
                                    Toast.makeText(context, "Service added", Toast.LENGTH_SHORT).show()
                                }

                                // clear new picks
                                imageUris = listOf()
                                selectedProfilePicUri = null
                                // if editing - update local existingImageUrls to reflect final state
                                existingImageUrls = finalImageUrls
                                existingProfilePicUrl = profileUrl

                                // navigate back to Hustler dashboard
                                val intent = Intent(context, HustlerDashboardActivity::class.java)
                                context.startActivity(intent)
                                if (context is EditServiceActivity) context.finish()

                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                loading = false
                            }
                        }
                    },
                    enabled = !imagesUploading && !loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (loading || imagesUploading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (serviceId != null) "Save Changes" else "Add Service")
                }
            }
        }
    }
}
