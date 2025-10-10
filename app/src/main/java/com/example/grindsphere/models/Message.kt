package com.example.grindsphere.models

import com.google.firebase.Timestamp

data class Message(
    val id: String = "",
    val senderId: String = "",
    val senderName: String? = null, // ADD THIS FIELD
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val type: String = "text", // text, system, booking_request, etc.
    val bookingId: String? = null,
    val senderProfilePicUrl: String? = null
)