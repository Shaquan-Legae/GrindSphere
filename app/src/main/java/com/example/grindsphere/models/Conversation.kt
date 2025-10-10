package com.example.grindsphere.models

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * A unified data class for a conversation.
 * This should be the single source of truth for conversation-related data.
 */
data class Conversation(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(), // This should exist
    val lastMessage: String = "",
    val lastMessageTimestamp: Any? = null, // Could be Timestamp, Date, or Long
    val serviceName: String = "",
    val bookingId: String? = null, // ADD THIS if missing
    val createdAt: Any? = null
)