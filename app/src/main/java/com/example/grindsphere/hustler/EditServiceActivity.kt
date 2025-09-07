package com.example.grindsphere.hustler

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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await // Import for await

class EditServiceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val serviceId = intent.getStringExtra("serviceId")
        setContent {
            EditServiceScreen(serviceId)
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
    val coroutineScope = rememberCoroutineScope() // For launching coroutines

    var serviceName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    // Store URIs for images to be uploaded, and existing URLs
    var imageUris by remember { mutableStateOf<List<Uri>>(listOf()) }
    var existingImageUrls by remember { mutableStateOf<List<String>>(listOf()) }
    var loading by remember { mutableStateOf(false) }
    var imagesUploading by remember { mutableStateOf(false) } // To track upload state

    // Combined list for display purposes
    val displayImages = existingImageUrls + imageUris.map { it.toString() }

    // Load existing service
    LaunchedEffect(serviceId) {
        if (!serviceId.isNullOrEmpty()) {
            loading = true
            firestore.collection("services").document(serviceId).get()
                .addOnSuccessListener { doc ->
                    serviceName = doc.getString("name") ?: ""
                    description = doc.getString("description") ?: ""
                    location = doc.getString("location") ?: ""
                    val images = doc.get("images") as? List<*>
                    existingImageUrls = images?.mapNotNull { it as? String } ?: listOf()
                    loading = false
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to load service: ${it.message}", Toast.LENGTH_SHORT).show()
                    loading = false
                }
        }
    }

    // Image picker
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        // Add new URIs to the list of URIs to be uploaded
        imageUris = imageUris + uris
    }

    // Function to upload images and get their download URLs
    suspend fun uploadImagesAndGetUrls(uris: List<Uri>): List<String> {
        val uploadedUrls = mutableListOf<String>()
        if (uris.isEmpty()) return existingImageUrls // Return existing if no new images

        imagesUploading = true
        try {
            for (uri in uris) {
                val ref = storage.reference.child("serviceImages/${auth.currentUser!!.uid}/${System.currentTimeMillis()}")
                // Await the putFile operation
                ref.putFile(uri).await()
                // Await the getDownloadUrl operation
                val downloadUrl = ref.downloadUrl.await().toString()
                uploadedUrls.add(downloadUrl)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Image upload failed: ${e.message}", Toast.LENGTH_LONG).show()
            // Optionally, you might want to throw the exception or handle it more gracefully
            // For now, we'll return the URLs that were successfully uploaded up to this point
        } finally {
            imagesUploading = false
        }
        return existingImageUrls + uploadedUrls // Combine with existing URLs
    }


    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (serviceId != null) "Edit Service" else "Add Service") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF42A5F5), Color(0xFF1E88E5))
                        )
                    )
                    .clickable {
                        // Allow changing the banner by picking a new first image
                        launcher.launch("image/*")
                    },
                contentAlignment = Alignment.Center
            ) {
                if (displayImages.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(displayImages.first()), // Display first image as banner
                        contentDescription = "Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("Add Banner Image", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextField(
                value = serviceName,
                onValueChange = { serviceName = it },
                label = { Text("Service Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text("Images", color = Color.Gray)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(displayImages) { imageUrlOrUri ->
                    Box {
                        Image(
                            painter = rememberAsyncImagePainter(model = imageUrlOrUri),
                            contentDescription = null,
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    // Remove image
                                    // Check if it's an existing URL or a new URI
                                    if (existingImageUrls.contains(imageUrlOrUri)) {
                                        existingImageUrls = existingImageUrls - imageUrlOrUri
                                        // Optional: Delete from Firebase Storage if it's an existing image
                                        // val imageRef = storage.getReferenceFromUrl(imageUrlOrUri)
                                        // imageRef.delete().addOnSuccessListener { Log.d("EditService", "Image deleted from storage") }
                                    } else {
                                        // It's a URI, find and remove it from imageUris
                                        val uriToRemove = imageUris.find { it.toString() == imageUrlOrUri }
                                        uriToRemove?.let { imageUris = imageUris - it }
                                    }
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                item {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray)
                            .clickable { launcher.launch("image/*") }, // Allow multiple selections
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        loading = true
                        try {
                            // 1. Upload new images and get their URLs
                            val uploadedNewImageUrls = uploadImagesAndGetUrls(imageUris) // This function is now suspend

                            // 2. Combine with existing URLs that weren't removed
                            val finalImageUrls = uploadedNewImageUrls // uploadImagesAndGetUrls already combines them

                            val data = hashMapOf(
                                "name" to serviceName,
                                "description" to description,
                                "location" to location,
                                "images" to finalImageUrls, // Save the final list of URLs
                                "ownerUid" to auth.currentUser!!.uid
                            )
                            val docRef = if (serviceId != null) {
                                firestore.collection("services").document(serviceId)
                            } else {
                                firestore.collection("services").document()
                            }

                            docRef.set(data).await() // Use await for Firestore operation

                            Toast.makeText(context, "Service saved!", Toast.LENGTH_SHORT).show()
                            // Clear new image URIs after successful save
                            imageUris = listOf()
                            // If it's a new service, you might want to fetch the updated existingImageUrls
                            // or navigate away, etc.
                            if (serviceId == null) {
                                // Potentially navigate back or clear the form
                                (context as? EditServiceActivity)?.finish() // Example: Finish activity
                            } else {
                                // For existing service, update the existingImageUrls to reflect the saved state
                                existingImageUrls = finalImageUrls
                            }

                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to save service: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            loading = false
                        }
                    }
                },
                enabled = !imagesUploading, // Disable button while images are uploading
                modifier = Modifier.fillMaxWidth()
            ) {
                if (loading || imagesUploading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                else Text(if (serviceId != null) "Save Changes" else "Add Service", color = Color.White)
            }
        }
    }
}
