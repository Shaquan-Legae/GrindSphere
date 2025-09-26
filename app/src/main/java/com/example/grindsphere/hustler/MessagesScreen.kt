package com.example.grindsphere.hustler

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import com.google.firebase.firestore.FieldValue
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.grindsphere.models.Booking
import com.example.grindsphere.models.Conversation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MessagesScreen(
    onOpenChat: (conversationId: String, customerUid: String, customerName: String) -> Unit,
    onStartNewChat: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    var conversations by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var bookingRequests by remember { mutableStateOf<List<Booking>>(emptyList()) }
    var showBookingRequests by remember { mutableStateOf(true) }

    // Load conversations
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            firestore.collection("conversations")
                .whereArrayContains("participants", uid)
                .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Toast.makeText(context, "Error loading conversations: ${error.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    val convos = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject<Conversation>()?.copy(id = doc.id)
                    } ?: emptyList()
                    conversations = convos
                }

            // Load booking requests
            firestore.collection("bookingRequests")
                .whereEqualTo("hustlerId", uid)
                .whereEqualTo("status", "pending")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener

                    val requests = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject<Booking>()?.copy(id = doc.id)
                    } ?: emptyList()
                    bookingRequests = requests
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0D324D), Color(0xFF7F5A83))))
    ) {
        // Header with tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(
                onClick = { showBookingRequests = true },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "Connection Requests (${bookingRequests.size})",
                    color = if (showBookingRequests) Color(0xFFFFD700) else Color.White
                )
            }

            TextButton(
                onClick = { showBookingRequests = false },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "Messages (${conversations.size})",
                    color = if (!showBookingRequests) Color(0xFFFFD700) else Color.White
                )
            }
        }

        if (showBookingRequests) {
            BookingRequestsSection(
                bookingRequests = bookingRequests,
                onAccept = { request ->
                    currentUser?.uid?.let { uid ->
                        // Update booking status
                        firestore.collection("bookingRequests").document(request.id)
                            .update("status", "accepted")
                            .addOnSuccessListener {
                                // Create conversation
                                createOrFindConversation(
                                    firestore = firestore,
                                    participant1 = uid,
                                    participant2 = request.customerId,
                                    serviceName = request.serviceName,
                                    onSuccess = {
                                        Toast.makeText(context, "Connection accepted!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                    }
                },
                onDecline = { request ->
                    firestore.collection("bookingRequests").document(request.id)
                        .update("status", "declined")
                        .addOnSuccessListener {
                            Toast.makeText(context, "Connection declined", Toast.LENGTH_SHORT).show()
                        }
                },
                onChat = { request ->
                    currentUser?.uid?.let { uid ->
                        createOrFindConversation(
                            firestore = firestore,
                            participant1 = uid,
                            participant2 = request.customerId,
                            serviceName = request.serviceName,
                            onSuccess = { conversationId ->
                                onOpenChat(conversationId, request.customerId, request.customerName)
                            }
                        )
                    }
                }
            )
        } else {
            ConversationsSection(
                conversations = conversations,
                currentUserId = currentUser?.uid ?: "",
                onOpenChat = onOpenChat,
                onStartNewChat = onStartNewChat
            )
        }
    }
}

@Composable
fun BookingRequestsSection(
    bookingRequests: List<Booking>,
    onAccept: (Booking) -> Unit,
    onDecline: (Booking) -> Unit,
    onChat: (Booking) -> Unit
) {
    if (bookingRequests.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Person,
            title = "No connection requests yet",
            subtitle = "When customers connect with your services, requests will appear here"
        )
    } else {
        LazyColumn(modifier = Modifier.padding(16.dp)) {
            items(bookingRequests) { request ->
                BookingRequestItem(
                    request = request,
                    onAccept = { onAccept(request) },
                    onDecline = { onDecline(request) },
                    onChat = { onChat(request) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ConversationsSection(
    conversations: List<Conversation>,
    currentUserId: String,
    onOpenChat: (conversationId: String, customerUid: String, customerName: String) -> Unit,
    onStartNewChat: () -> Unit
) {
    if (conversations.isEmpty()) {
        EmptyState(
            icon = Icons.AutoMirrored.Filled.Chat,
            title = "No messages yet",
            subtitle = "Start a conversation and your messages will appear here",
            actionText = "Start a Conversation",
            onAction = onStartNewChat
        )
    } else {
        val firestore = FirebaseFirestore.getInstance()
        LazyColumn(modifier = Modifier.padding(16.dp)) {
            items(conversations) { conversation ->
                val otherParticipantId = conversation.participants.find { it != currentUserId } ?: ""
                val otherUserName = conversation.participantNames[otherParticipantId] ?: "User"

                ConversationItem(
                    conversation = conversation,
                    userName = otherUserName,
                    onTap = {
                        onOpenChat(conversation.id, otherParticipantId, otherUserName)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    actionText: String? = null,
    onAction: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(title, color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                subtitle,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            actionText?.let {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text(it)
                }
            }
        }
    }
}

@Composable
fun BookingRequestItem(
    request: Booking,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onChat: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChat() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD700).copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        request.customerName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        "Service: ${request.serviceName}",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                    Text(
                        "Message: ${request.message}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        maxLines = 2
                    )
                    request.timestamp?.let {
                        Text(
                            "Received: ${formatDate(it.time)}",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 10.sp
                        )
                    }
                }

                // Action buttons
                Row {
                    IconButton(
                        onClick = { onAccept() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Accept",
                            tint = Color.Green
                        )
                    }
                    IconButton(
                        onClick = { onDecline() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Decline",
                            tint = Color.Red
                        )
                    }
                }
            }

            // Chat button
            Button(
                onClick = { onChat() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F5A83))
            ) {
                Text("Open Chat")
            }
        }
    }
}

@Composable
fun ConversationItem(
    conversation: Conversation,
    userName: String,
    onTap: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(userName, color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    conversation.lastMessage,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    maxLines = 1
                )
                conversation.lastMessageTimestamp?.let {
                    Text(
                        formatDate(it.time),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return format.format(date)
}

// Helper function to create or find conversation
fun createOrFindConversation(
    firestore: FirebaseFirestore,
    participant1: String, // Hustler UID
    participant2: String, // Customer UID
    serviceName: String,
    onSuccess: (String) -> Unit
) {
    val participants = listOf(participant1, participant2).sorted()

    firestore.collection("conversations")
        .whereArrayContains("participants", participant1)
        .get()
        .addOnSuccessListener { snapshot ->
            val existingConvo = snapshot.documents.firstOrNull { doc ->
                val convoParticipants = doc.get("participants") as? List<*>
                convoParticipants?.containsAll(participants) == true
            }

            // Fetch hustler and customer names from the 'users' collection
            val hustlerDocRef = firestore.collection("users").document(participant1)
            val customerDocRef = firestore.collection("users").document(participant2)

            hustlerDocRef.get().addOnSuccessListener { hustlerDoc ->
                val hustlerName = hustlerDoc.getString("name") ?: "Hustler"
                customerDocRef.get().addOnSuccessListener { customerDoc ->
                    val customerName = customerDoc.getString("name") ?: "Customer"

                    val participantNames = mapOf(
                        participant1 to hustlerName,
                        participant2 to customerName
                    )

                    if (existingConvo != null) {
                        // If conversation exists, just update the names and call success
                        firestore.collection("conversations").document(existingConvo.id)
                            .update("participantNames", participantNames)
                            .addOnSuccessListener {
                                onSuccess(existingConvo.id)
                            }
                    } else {
                        // If conversation doesn't exist, create it with all the correct info
                        val newConvo = hashMapOf(
                            "participants" to participants,
                            "participantNames" to participantNames,
                            "lastMessage" to "Connection for: $serviceName",
                            "lastMessageTimestamp" to FieldValue.serverTimestamp(),
                            "serviceName" to serviceName
                        )
                        firestore.collection("conversations").add(newConvo)
                            .addOnSuccessListener { docRef ->
                                onSuccess(docRef.id)
                            }
                    }
                }
            }
        }
}