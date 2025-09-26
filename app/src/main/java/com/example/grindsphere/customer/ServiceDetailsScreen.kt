package com.example.grindsphere.customer

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceDetailsScreen(
    serviceId: String,
    navController: NavHostController
) {
    var service by remember { mutableStateOf<Service?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var bookingMessage by remember { mutableStateOf("") }
    var bookingDate by remember { mutableStateOf(Date()) }
    val context = LocalContext.current

    // Fetch service from Firestore
    LaunchedEffect(serviceId) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("services")
            .document(serviceId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    service = Service(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        location = doc.getString("location") ?: "",
                        images = doc.get("images") as? List<String> ?: listOf(),
                        ownerUid = doc.getString("ownerUid") ?: "",
                        ownerName = doc.getString("ownerName") ?: "",
                        category = doc.getString("category") ?: "Other",
                        price = doc.getDouble("price") ?: 0.0,
                        rating = doc.getDouble("rating") ?: 0.0
                    )
                }
                isLoading = false
            }
            .addOnFailureListener { isLoading = false }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    service?.let { srv ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)) {
            if (srv.images.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(srv.images.first()),
                    contentDescription = "Service image for ${srv.name}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.LightGray.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "No image available",
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Text(srv.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(srv.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Category: ${srv.category}", style = MaterialTheme.typography.bodySmall)
            Text("Location: ${srv.location}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Provider: ${srv.ownerName}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Price: R${String.format(Locale.US, "%.2f", srv.price)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Rating",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(String.format(Locale.US, "%.1f", srv.rating), style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Booking Form
            OutlinedTextField(
                value = bookingMessage,
                onValueChange = { bookingMessage = it },
                label = { Text("Message to provider") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val auth = FirebaseAuth.getInstance()
                    val customerId = auth.currentUser?.uid ?: return@Button
                    val booking = hashMapOf(
                        "serviceId" to srv.id,
                        "serviceName" to srv.name,
                        "customerId" to customerId,
                        "customerName" to (auth.currentUser?.displayName ?: ""),
                        "hustlerId" to srv.ownerUid,
                        "hustlerName" to srv.ownerName,
                        "status" to "pending",
                        "date" to bookingDate,
                        "message" to bookingMessage,
                        "price" to srv.price
                    )
                    FirebaseFirestore.getInstance()
                        .collection("bookingRequests")
                        .add(booking)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Booking requested!", Toast.LENGTH_SHORT).show()
                            navController.navigate("bookings")
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed to book.", Toast.LENGTH_SHORT).show()
                        }
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Book Now")
            }
        }
    } ?: Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Service not found.")
    }
}
