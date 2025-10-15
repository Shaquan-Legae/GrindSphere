package com.example.grindsphere.models

import com.google.firebase.Timestamp

data class Message(
    val id: String = "",
    val senderId: String = "",
    val senderName: String? = "",
    val senderProfilePicUrl: String? = "",
    val text: String? = "",
    val attachmentUrl: String? = "",
    val fileType: String? = "", // ✅ Add this
    val timestamp: Timestamp = Timestamp.now(),
    val type: String = "text",
    val isRead: Boolean = false
)
