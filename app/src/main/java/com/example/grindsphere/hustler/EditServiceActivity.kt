package com.example.grindsphere.hustler

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

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

    var serviceName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var serviceImages by remember { mutableStateOf(listOf<String>()) }
    var loading by remember { mutableStateOf(false) }

    // Load existing service
    LaunchedEffect(serviceId) {
        if (!serviceId.isNullOrEmpty()) {
            firestore.collection("services").document(serviceId).get()
                .addOnSuccessListener { doc ->
                    serviceName = doc.getString("name") ?: ""
                    description = doc.getString("description") ?: ""
                    location = doc.getString("location") ?: ""
                    val images = doc.get("images") as? List<*>
                    serviceImages = images?.mapNotNull { it as? String } ?: listOf()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to load service", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Image picker
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val ref = storage.reference.child("serviceImages/${System.currentTimeMillis()}")
            ref.putFile(uri).addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUri ->
                    serviceImages = serviceImages + downloadUri.toString()
                }
            }
        }
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
            // Banner (gradient placeholder)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF42A5F5), Color(0xFF1E88E5))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (serviceImages.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(serviceImages.first()),
                        contentDescription = "Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("Add Banner Image", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Service name
            TextField(
                value = serviceName,
                onValueChange = { serviceName = it },
                label = { Text("Service Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            TextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Location
            TextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Images", color = Color.Gray)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(serviceImages) { img ->
                    Box {
                        Image(
                            painter = rememberAsyncImagePainter(model = img),
                            contentDescription = null,
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    // Remove image on click
                                    serviceImages = serviceImages - img
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
                            .clickable { launcher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save button
            Button(
                onClick = {
                    loading = true
                    val data = hashMapOf(
                        "name" to serviceName,
                        "description" to description,
                        "location" to location,
                        "images" to serviceImages,
                        "ownerUid" to auth.currentUser!!.uid
                    )
                    val docRef = if (serviceId != null)
                        firestore.collection("services").document(serviceId)
                    else
                        firestore.collection("services").document()
                    docRef.set(data)
                        .addOnSuccessListener {
                            loading = false
                            Toast.makeText(context, "Service saved!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            loading = false
                            Toast.makeText(context, "Failed to save service", Toast.LENGTH_SHORT).show()
                        }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                else Text(if (serviceId != null) "Save Changes" else "Add Service", color = Color.White)
            }
        }
    }
}
