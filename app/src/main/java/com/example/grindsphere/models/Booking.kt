package com.example.grindsphere.models

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * A unified data class for a booking request.
 * This should be the single source of truth for booking-related data.
 */
data class Booking(
    val id: String = "",
    val serviceId: String = "",
    val serviceName: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerEmail: String = "", // ADD THIS if missing
    val hustlerId: String = "",
    val hustlerName: String = "",
    val status: String = "pending", // pending, accepted, declined, completed
    val timestamp: Date? = null,
    val message: String = "",
    val price: Double = 0.0
)