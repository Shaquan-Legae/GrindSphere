package com.example.grindsphere.customer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

@Composable
fun BookingsScreen() {
    var bookings by remember { mutableStateOf<List<BookingRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    LaunchedEffect(Unit) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            isLoading = false; return@LaunchedEffect
        }
        firestore.collection("bookingRequests")
            .whereEqualTo("customerId", currentUserId)
            .get()
            .addOnSuccessListener { result ->
                bookings = result.documents.mapNotNull { doc ->
                    BookingRequest(
                        id = doc.id,
                        serviceId = doc.getString("serviceId") ?: "",
                        serviceName = doc.getString("serviceName") ?: "",
                        customerId = doc.getString("customerId") ?: "",
                        customerName = doc.getString("customerName") ?: "",
                        hustlerId = doc.getString("hustlerId") ?: "",
                        hustlerName = doc.getString("hustlerName") ?: "",
                        status = doc.getString("status") ?: "pending",
                        date = doc.getDate("date") ?: Date(),
                        message = doc.getString("message") ?: "",
                        price = doc.getDouble("price") ?: 0.0
                    )
                }
                isLoading = false
            }
            .addOnFailureListener { isLoading = false }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("My Bookings", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (bookings.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("You have no bookings yet.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(bookings, key = { it.id }) { booking ->
                    BookingCard(booking = booking)
                }
            }
        }
    }
}
