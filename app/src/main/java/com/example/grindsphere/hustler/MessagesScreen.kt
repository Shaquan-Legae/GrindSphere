package com.example.grindsphere.hustler

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

// Data classes
data class Message(
    val id: String = "",
    val senderUid: String = "",
    val receiverUid: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val conversationId: String = ""
)

data class Conversation(
    val customerUid: String,
    val customerName: String,
    val lastMessage: String,
    val lastTimestamp: Long
)

data class User(
    val uid: String = "",
    val name: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    onOpenChat: (customerUid: String, customerName: String) -> Unit,
    onStartNewChat: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser
    var conversations by remember { mutableStateOf(listOf<Conversation>()) }

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            firestore.collection("messages")
                .whereArrayContains("participants", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
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

                    val grouped = msgs.groupBy { it.conversationId }
                    val convList = mutableListOf<Conversation>()
                    for ((_, msgList) in grouped) {
                        val lastMsg = msgList.maxByOrNull { it.timestamp } ?: continue
                        val otherUid = if (lastMsg.senderUid == uid) lastMsg.receiverUid else lastMsg.senderUid

                        var customerName = otherUid
                        firestore.collection("users").document(otherUid).get()
                            .addOnSuccessListener { doc ->
                                doc.getString("name")?.let { name ->
                                    customerName = name
                                    conversations = convList.sortedByDescending { it.lastTimestamp }
                                }
                            }

                        convList.add(
                            Conversation(
                                customerUid = otherUid,
                                customerName = customerName,
                                lastMessage = lastMsg.content,
                                lastTimestamp = lastMsg.timestamp
                            )
                        )
                    }
                    conversations = convList.sortedByDescending { it.lastTimestamp }
                }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0D324D), Color(0xFF7F5A83))))
            .padding(16.dp)
    ) {
        if (conversations.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Welcome to Messages!",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Start a conversation and your messages will appear here.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { onStartNewChat() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Start a Conversation", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        items(conversations) { convo ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onOpenChat(convo.customerUid, convo.customerName) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(convo.customerName, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            convo.lastMessage,
                            color = Color.White,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}


