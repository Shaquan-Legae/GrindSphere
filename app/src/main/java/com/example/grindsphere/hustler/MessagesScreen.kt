package com.example.grindsphere.hustler

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
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
import com.example.grindsphere.models.Conversation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.Timestamp

private const val TAG = "MessagesScreen"

@Composable
fun MessagesScreen(
    onOpenChat: (conversationId: String, customerUid: String, customerName: String) -> Unit,
    onStartNewChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    var conversations by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // Load conversations only
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            Log.d(TAG, "Loading conversations for user: $uid")

            firestore.collection("conversations")
                .whereArrayContains("participants", uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error loading conversations: ${error.message}", error)
                        Toast.makeText(
                            context,
                            "Error loading conversations: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@addSnapshotListener
                    }

                    val convos = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            doc.toObject<Conversation>()?.copy(id = doc.id)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing conversation doc ${doc.id}: ${e.message}")
                            null
                        }
                    }?.sortedByDescending {
                        it.lastMessageTimestamp?.let { timestamp ->
                            when (timestamp) {
                                is Long -> timestamp
                                is Date -> timestamp.time
                                is Timestamp -> timestamp.toDate().time
                                else -> 0L
                            }
                        } ?: 0L
                    } ?: emptyList()

                    Log.d(TAG, "Loaded ${convos.size} conversations")
                    conversations = convos
                }
        } ?: run {
            Log.e(TAG, "No current user found")
            Toast.makeText(context, "Please log in first", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0D324D), Color(0xFF7F5A83))))
    ) {
        // Simple header without tabs
        Text(
            "Messages",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
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
fun ConversationsSection(
    conversations: List<Conversation>,
    currentUserId: String,
    onOpenChat: (conversationId: String, customerUid: String, customerName: String) -> Unit,
    onStartNewChat: () -> Unit
) {
    val context = LocalContext.current

    if (conversations.isEmpty()) {
        EmptyState(
            icon = Icons.AutoMirrored.Filled.Chat,
            title = "No messages yet",
            subtitle = "Start a conversation and your messages will appear here",
            actionText = "Start a Conversation",
            onAction = onStartNewChat
        )
    } else {
        LazyColumn(modifier = Modifier.padding(16.dp)) {
            items(conversations) { conversation ->
                val otherParticipantId = conversation.participants.find { it != currentUserId } ?: ""
                val otherUserName = conversation.participantNames[otherParticipantId] ?: "User"

                ConversationItem(
                    conversation = conversation,
                    userName = otherUserName,
                    onTap = {
                        Log.d(TAG, "Opening existing conversation: ${conversation.id}")
                        try {
                            onOpenChat(conversation.id, otherParticipantId, otherUserName)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error opening conversation: ${e.message}", e)
                            Toast.makeText(context, "Error opening chat: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
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
                    conversation.lastMessage ?: "No messages",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    maxLines = 1
                )
                conversation.lastMessageTimestamp?.let {
                    Text(
                        formatDateSafe(it),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

fun formatDateSafe(timestampObj: Any?): String {
    if (timestampObj == null) return ""
    return try {
        val millis = when (timestampObj) {
            is Long -> timestampObj
            is Date -> timestampObj.time
            is Timestamp -> timestampObj.toDate().time
            else -> {
                when (timestampObj) {
                    is Number -> timestampObj.toLong()
                    else -> return ""
                }
            }
        }
        val date = Date(millis)
        val format = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        format.format(date)
    } catch (e: Exception) {
        Log.e(TAG, "Error formatting date: ${e.message}")
        ""
    }
}

// Improved createOrFindConversation with better error handling
fun createOrFindConversation(
    firestore: FirebaseFirestore,
    participant1: String,
    participant2: String,
    serviceName: String,
    onSuccess: (String) -> Unit,
    onError: (Exception) -> Unit = {}
) {
    Log.d(TAG, "createOrFindConversation: $participant1, $participant2, $serviceName")

    if (participant1.isBlank() || participant2.isBlank()) {
        val error = Exception("Missing participant ID(s): p1='$participant1', p2='$participant2'")
        Log.e(TAG, error.message ?: "Missing participant IDs")
        onError(error)
        return
    }

    val participants = listOf(participant1, participant2).sorted()
    Log.d(TAG, "Looking for conversation with participants: $participants")

    firestore.collection("conversations")
        .whereArrayContains("participants", participant1)
        .get()
        .addOnSuccessListener { snapshot ->
            Log.d(TAG, "Found ${snapshot.documents.size} potential conversations")

            val existingConvo = snapshot.documents.firstOrNull { doc ->
                val convoParticipants = (doc.get("participants") as? List<*>)
                    ?.mapNotNull { it as? String }
                val containsAll = convoParticipants?.containsAll(participants) == true
                Log.d(TAG, "Doc ${doc.id} participants: $convoParticipants, matches: $containsAll")
                containsAll
            }

            if (existingConvo != null) {
                Log.d(TAG, "Found existing conversation: ${existingConvo.id}")
                // Just return the existing conversation ID
                onSuccess(existingConvo.id)
            } else {
                Log.d(TAG, "No existing conversation found, creating new one")
                // Create new conversation with minimal data first
                createNewConversation(firestore, participant1, participant2, serviceName, onSuccess, onError)
            }
        }
        .addOnFailureListener { e ->
            Log.e(TAG, "Error finding conversation: ${e.message}", e)
            onError(Exception("Failed to search for conversation: ${e.message}"))
        }
}

// Separate function to create new conversation
private fun createNewConversation(
    firestore: FirebaseFirestore,
    participant1: String,
    participant2: String,
    serviceName: String,
    onSuccess: (String) -> Unit,
    onError: (Exception) -> Unit
) {
    val participants = listOf(participant1, participant2).sorted()

    // Create conversation with basic data first
    val newConvo = hashMapOf(
        "participants" to participants,
        "lastMessage" to "Connection for: $serviceName",
        "lastMessageTimestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
        "serviceName" to serviceName,
        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
    )

    firestore.collection("conversations")
        .add(newConvo)
        .addOnSuccessListener { docRef ->
            Log.d(TAG, "Conversation created successfully: ${docRef.id}")
            // Now update with participant names
            updateParticipantNames(firestore, docRef.id, participant1, participant2, onSuccess, onError)
        }
        .addOnFailureListener { e ->
            Log.e(TAG, "Error creating conversation: ${e.message}", e)
            onError(Exception("Failed to create conversation: ${e.message}"))
        }
}

// Update conversation with participant names
private fun updateParticipantNames(
    firestore: FirebaseFirestore,
    conversationId: String,
    participant1: String,
    participant2: String,
    onSuccess: (String) -> Unit,
    onError: (Exception) -> Unit
) {
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

            firestore.collection("conversations")
                .document(conversationId)
                .update("participantNames", participantNames)
                .addOnSuccessListener {
                    Log.d(TAG, "Participant names updated for conversation: $conversationId")
                    onSuccess(conversationId)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error updating participant names: ${e.message}", e)
                    // Still return success since conversation was created
                    onSuccess(conversationId)
                }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Error loading customer data: ${e.message}", e)
            // Still return success since conversation was created
            onSuccess(conversationId)
        }
    }.addOnFailureListener { e ->
        Log.e(TAG, "Error loading hustler data: ${e.message}", e)
        // Still return success since conversation was created
        onSuccess(conversationId)
    }
}