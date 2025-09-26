package com.example.grindsphere.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale


@Composable
fun BookingCard(booking: BookingRequest) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(booking.serviceName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                StatusBadge(status = booking.status)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Provider: ${booking.hustlerName}", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Date: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(booking.date)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text("Price: R${String.format(Locale.US, "%.2f", booking.price)}", style = MaterialTheme.typography.bodyMedium)
            if (booking.message.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Message: ${booking.message}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (backgroundColor, textColor) = when (status.lowercase(Locale.ROOT)) {
        "accepted" -> Color.Green.copy(alpha = 0.2f) to Color.Green.copy(alpha = 0.9f)
        "rejected" -> Color.Red.copy(alpha = 0.2f) to Color.Red.copy(alpha = 0.9f)
        "completed" -> Color.Blue.copy(alpha = 0.2f) to Color.Blue.copy(alpha = 0.9f)
        "pending" -> Color.Yellow.copy(alpha = 0.3f) to Color(0xFFB8860B)
        else -> Color.Gray.copy(alpha = 0.2f) to Color.DarkGray
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun CustomerProfileScreen(navController: NavHostController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Customer Profile", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                FirebaseAuth.getInstance().signOut()
                // Navigate to login screen
            }) {
                Text("Sign Out")
            }
        }
    }
}
