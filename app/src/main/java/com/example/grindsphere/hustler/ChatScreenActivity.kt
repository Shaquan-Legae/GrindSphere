package com.example.grindsphere.hustler

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

data class ChatScreen(
    val id: String = "",
    val senderUid: String = "",
    val receiverUid: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val conversationId: String = ""
)

class ChatScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val conversationId = intent.getStringExtra("conversationId") ?: ""
        val customerUid = intent.getStringExtra("customerUid") ?: ""
        val customerName = intent.getStringExtra("customerName") ?: ""

        setContent {
            ChatScreen(conversationId, customerUid, customerName)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(conversationId: String, customerUid: String, customerName: String) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    var messages by remember { mutableStateOf(listOf<Message>()) }
    var newMessage by remember { mutableStateOf("") }

    // Listen for messages
    LaunchedEffect(conversationId) {
        if (conversationId.isNotBlank()) {
            firestore.collection("messages")
                .whereEqualTo("conversationId", conversationId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }
                    val msgs = snapshot?.documents?.mapNotNull { doc ->
                        Message(
                            id = doc.id,
                            senderUid = doc.getString("senderUid") ?: "",
                            receiverUid = doc.getString("receiverUid") ?: "",
                            content = doc.getString("content") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            conversationId = doc.getString("conversationId") ?: ""
                        )
                    } ?: listOf()
                    messages = msgs
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(customerName, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D324D))
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF0D324D), Color(0xFF7F5A83))))
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Messages
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { msg ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (msg.senderUid == currentUser?.uid) Arrangement.End else Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (msg.senderUid == currentUser?.uid) Color(0xFF4CAF50) else Color.Gray,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Text(msg.content, color = Color.White)
                            }
                        }
                    }
                }

                // Input field
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newMessage,
                        onValueChange = { newMessage = it },
                        placeholder = { Text("Type a message") },
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.White, RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val content = newMessage.trim()
                            if (content.isNotEmpty() && currentUser != null) {
                                val msgMap = hashMapOf(
                                    "senderUid" to currentUser.uid,
                                    "receiverUid" to customerUid,
                                    "content" to content,
                                    "timestamp" to System.currentTimeMillis(),
                                    "conversationId" to conversationId
                                )
                                firestore.collection("messages")
                                    .add(msgMap)
                                    .addOnSuccessListener {
                                        newMessage = ""
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context, "Failed to send: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
            }
        }
    }
}
