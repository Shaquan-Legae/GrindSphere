package com.example.grindsphere.hustler

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = "",
    val senderUid: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)

@Composable
fun ChatScreen(navController: NavController, chatId: String, customerName: String) {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var newMessage by remember { mutableStateOf("") }

    // Listen to messages in real-time
    LaunchedEffect(chatId) {
        firestore.collection("messages")
            .whereEqualTo("chatId", chatId)
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = snapshot?.documents?.map { doc ->
                    ChatMessage(
                        id = doc.id,
                        senderUid = doc.getString("senderUid") ?: "",
                        text = doc.getString("text") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                } ?: emptyList()
                messages = list.sortedBy { it.timestamp }

                // Scroll to latest message
                scope.launch {
                    if (messages.isNotEmpty()) {
                        listState.animateScrollToItem(messages.lastIndex)
                    }
                }
            }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
    ) {
        Text(
            text = "Chat with $customerName",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(messages) { msg ->
                val isMe = msg.senderUid == currentUser?.uid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(
                            text = msg.text,
                            modifier = Modifier.padding(8.dp),
                            color = if (isMe) Color.White else Color.Black
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newMessage,
                onValueChange = { newMessage = it },
                placeholder = { Text("Type a message") },
                modifier = Modifier.weight(1f),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )
            Button(onClick = {
                if (newMessage.isNotBlank() && currentUser != null) {
                    val msgData = mapOf(
                        "senderUid" to currentUser.uid,
                        "text" to newMessage,
                        "timestamp" to System.currentTimeMillis(),
                        "chatId" to chatId
                    )
                    firestore.collection("messages").add(msgData)

                    // Update last message in conversation
                    firestore.collection("conversations").document(chatId)
                        .update(
                            mapOf(
                                "lastMessage" to newMessage,
                                "timestamp" to System.currentTimeMillis()
                            )
                        )
                    newMessage = ""
                    focusManager.clearFocus()
                }
            }) {
                Text("Send")
            }
        }
    }
}
