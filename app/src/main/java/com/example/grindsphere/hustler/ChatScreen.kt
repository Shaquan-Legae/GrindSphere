package com.example.grindsphere.hustler

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api


data class ChatMessage(
    val id: String = "",
    val senderUid: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    customerUid: String,
    customerName: String,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val scope = rememberCoroutineScope()

    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var newMessage by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Listen to messages
    LaunchedEffect(conversationId) {
        firestore.collection("messages")
            .whereEqualTo("conversationId", conversationId)
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(context, "Error loading messages: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val list = snapshot?.documents?.map { doc ->
                    ChatMessage(
                        id = doc.id,
                        senderUid = doc.getString("senderUid") ?: "",
                        text = doc.getString("text") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                } ?: emptyList()
                messages = list

                // Scroll to bottom
                if (messages.isNotEmpty()) {
                    scope.launch {
                        listState.animateScrollToItem(messages.size - 1)
                    }
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat with $customerName") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Messages list
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(messages) { message ->
                    val isMe = message.senderUid == currentUser?.uid
                    MessageBubble(message = message, isMe = isMe)
                }
            }

            // Input area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newMessage,
                    onValueChange = { newMessage = it },
                    placeholder = { Text("Type a message...") },
                    modifier = Modifier.weight(1f),
                    keyboardActions = KeyboardActions {
                        if (newMessage.isNotBlank()) {
                            sendMessage(newMessage, currentUser, conversationId, customerUid, firestore)
                            newMessage = ""
                        }
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        sendMessage(newMessage, currentUser, conversationId, customerUid, firestore)
                        newMessage = ""
                    },
                    enabled = newMessage.isNotBlank()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage, isMe: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(16.dp),
                color = if (isMe) Color.White else Color.Black
            )
        }
    }
}

private fun sendMessage(
    text: String,
    currentUser: com.google.firebase.auth.FirebaseUser?,
    conversationId: String,
    customerUid: String,
    firestore: FirebaseFirestore
) {
    if (text.isNotBlank() && currentUser != null) {
        val messageData = hashMapOf(
            "senderUid" to currentUser.uid,
            "receiverUid" to customerUid,
            "text" to text,
            "timestamp" to System.currentTimeMillis(),
            "conversationId" to conversationId
        )

        firestore.collection("messages").add(messageData)

        // Update conversation last message
        firestore.collection("conversations").document(conversationId)
            .update(mapOf(
                "lastMessage" to text,
                "timestamp" to System.currentTimeMillis()
            ))
    }
}
