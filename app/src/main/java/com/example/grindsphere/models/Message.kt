package com.example.grindsphere.models

import com.google.firebase.Timestamp

/**
 * A unified data class for a message.
 * This should be the single source of truth for messaging-related data.
 */
data class Message(
    val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now()
)