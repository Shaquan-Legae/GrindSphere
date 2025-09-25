package com.example.grindsphere.hustler

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NewChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NewChatScreen()
        }
    }
}


// Add this function to NewChatActivity.kt (before or after the NewChatScreen composable)
private fun findOrCreateConversationForChat(
    firestore: FirebaseFirestore,
    participant1: String,
    participant2: String,
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

            if (existingConvo != null) {
                onSuccess(existingConvo.id)
            } else {
                val newConvo = hashMapOf(
                    "participants" to participants,
                    "lastMessage" to "Connection for: $serviceName",
                    "timestamp" to System.currentTimeMillis(),
                    "type" to "booking"
                )
                firestore.collection("conversations").add(newConvo)
                    .addOnSuccessListener { docRef ->
                        onSuccess(docRef.id)
                    }
            }
        }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatScreen() {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    var users by remember { mutableStateOf(listOf<ChatUser>()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        currentUser?.uid?.let { currentUid ->
            firestore.collection("users")
                .whereNotEqualTo("uid", currentUid)
                .get()
                .addOnSuccessListener { snapshot ->
                    val userList = snapshot.documents.mapNotNull { doc ->
                        val uid = doc.getString("uid") ?: return@mapNotNull null
                        val name = doc.getString("name") ?: "Unknown User"
                        ChatUser(uid, name)
                    }
                    users = userList
                    loading = false
                }
                .addOnFailureListener {
                    loading = false
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Start New Chat") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn {
                    items(users) { user ->
                        ListItem(
                            headlineContent = { Text(user.name) },
                            modifier = Modifier
                                .clickable {
                                    // Create conversation and open chat
                                    findOrCreateConversationForChat(
                                        firestore = firestore,
                                        participant1 = currentUser?.uid ?: "",
                                        participant2 = user.uid,
                                        serviceName = "Direct Message",
                                        onSuccess = { conversationId ->
                                            val intent = Intent(context, ChatActivity::class.java).apply {
                                                putExtra("conversationId", conversationId)
                                                putExtra("customerUid", user.uid)
                                                putExtra("customerName", user.name)
                                            }
                                            context.startActivity(intent)
                                        }
                                    )
                                }
                                .fillMaxWidth()
                                .padding(8.dp)
                        )
                        Divider()
                    }
                }
            }
        }
    }
}

data class ChatUser(
    val uid: String,
    val name: String
)
