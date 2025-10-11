package com.example.grindsphere.hustler

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.grindsphere.models.Message
import com.example.grindsphere.models.Conversation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.Timestamp

private const val TAG = "ChatScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    customerUid: String,
    customerName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }
    var conversation by remember { mutableStateOf<Conversation?>(null) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var customerProfilePic by remember { mutableStateOf("") }
    var currentUserProfilePic by remember { mutableStateOf("") }

    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Load current user's profile picture
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    currentUserProfilePic = doc.getString("profilePicUrl") ?: ""
                }
        }
    }

    // Load customer's profile picture
    LaunchedEffect(customerUid) {
        firestore.collection("users").document(customerUid).get()
            .addOnSuccessListener { doc ->
                customerProfilePic = doc.getString("profilePicUrl") ?: ""
            }
    }


    // Show error toast when errorMessage changes
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            errorMessage = null
        }
    }

    // Load conversation details to get participant names
    LaunchedEffect(conversationId) {
        try {
            firestore.collection("conversations").document(conversationId)
                .addSnapshotListener { snapshot, error ->
                    conversation = snapshot?.toObject<Conversation>()
                    loading = false
                }
        } catch (e: Exception) {
            errorMessage = "Error loading conversation: ${e.message}"
            loading = false
        }
    }

    // Load messages
    LaunchedEffect(conversationId) {
        try {
            firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error loading messages: ${error.message}")
                        return@addSnapshotListener
                    }

                    messages = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject<Message>()?.copy(id = doc.id)
                    } ?: emptyList()

                    // Auto-scroll to bottom when new messages arrive
                    if (messages.isNotEmpty()) {
                        coroutineScope.launch {
                            scrollState.animateScrollToItem(messages.size - 1)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up messages listener: ${e.message}")
        }
    }

    // In ChatScreen, add this function
    fun markMessagesAsRead(conversationId: String, currentUserId: String) {
        firestore.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .whereEqualTo("isRead", false)
            .whereNotEqualTo("senderId", currentUserId)
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.forEach { doc ->
                    doc.reference.update("isRead", true)
                }
            }
    }

// Call this function when the chat screen is opened/active
    LaunchedEffect(conversationId, currentUser?.uid) {
        if (conversationId.isNotEmpty() && currentUser?.uid != null) {
            markMessagesAsRead(conversationId, currentUser.uid)
        }
    }

    // Send message function
    fun sendMessage() {
        if (messageText.isBlank() || currentUser == null) return

        coroutineScope.launch {
            try {
                // Get user name from Firestore
                val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
                val userName = userDoc.getString("name") ?: "User"

                val messageData = hashMapOf(
                    "senderId" to currentUser.uid,
                    "senderName" to userName,
                    "senderProfilePicUrl" to currentUserProfilePic,
                    "text" to messageText,
                    "timestamp" to Timestamp.now(),
                    "type" to "text",
                    "isRead" to false // Add this line
                )

                // Add message to subcollection
                firestore.collection("conversations")
                    .document(conversationId)
                    .collection("messages")
                    .add(messageData)
                    .await()

                // Update conversation last message
                firestore.collection("conversations")
                    .document(conversationId)
                    .update(
                        mapOf(
                            "lastMessage" to messageText,
                            "lastMessageTimestamp" to FieldValue.serverTimestamp()
                        )
                    )
                    .addOnSuccessListener {
                        Log.d(TAG, "Conversation updated successfully")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to update conversation: ${e.message}")
                    }

                messageText = ""

            } catch (e: Exception) {
                errorMessage = "Failed to send message: ${e.message}"
                Log.e(TAG, "Failed to send message: ${e.message}", e)
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Customer profile picture in top bar
                        if (customerProfilePic.isNotEmpty()) {
                            Image(
                                painter = rememberAsyncImagePainter(customerProfilePic),
                                contentDescription = "Customer Profile",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF7F5A83)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    customerName.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(customerName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(
                                if (messages.isNotEmpty()) "Last seen recently" else "Say hello!",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D324D)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0D324D),
                            Color(0xFF7F5A83)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Messages list
                if (loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Loading messages...",
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else if (messages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "No messages",
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No messages yet",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Start the conversation!",
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        state = scrollState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(messages) { message ->
                            MessageBubble(
                                message = message,
                                isMe = message.senderId == currentUser?.uid,
                                currentUserProfilePic = currentUserProfilePic,
                                otherUserProfilePic = customerProfilePic


                            )
                        }
                    }
                }

                // Message input
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...", color = Color.Gray) },
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        keyboardActions = KeyboardActions(onSend = { sendMessage() }),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    IconButton(
                        onClick = { sendMessage() },
                        enabled = messageText.isNotBlank(),
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                if (messageText.isNotBlank()) Color(0xFFFFD700)
                                else Color.Gray.copy(alpha = 0.5f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (messageText.isNotBlank()) Color.Black else Color.White
                        )
                    }
                }
            }
        }
    }
}



@Composable
fun MessageBubble(
    message: Message,
    isMe: Boolean,
    currentUserProfilePic: String,
    otherUserProfilePic: String
) {
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val messageTime = timeFormat.format(message.timestamp.toDate())
    val profilePicUrl = if (isMe) currentUserProfilePic else otherUserProfilePic

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMe) {
            // Profile picture for other user's messages (left side)
            ProfilePicture(
                profilePicUrl = profilePicUrl,
                userName = message.senderName ?: "User",
                size = 32.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.weight(1f)
        ) {
            // Sender name (only show for other user's messages)
            if (!isMe) {
                Text(
                    text = message.senderName ?: "User",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            Row(
                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.Bottom
            ) {
                if (isMe) {
                    // Timestamp for my messages (on left side of bubble)
                    Text(
                        text = messageTime,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(bottom = 12.dp, end = 8.dp)
                    )
                }

                // Message bubble
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isMe) Color(0xFFFFD700) // Gold for current user
                        else Color(0xFF7F5A83) // Purple for other user
                    ),
                    shape = RoundedCornerShape(
                        topStart = if (isMe) 16.dp else 4.dp,
                        topEnd = if (isMe) 4.dp else 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    ),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // Message text
                        Text(
                            text = message.text,
                            color = if (isMe) Color.Black else Color.White,
                            fontSize = 16.sp,
                            lineHeight = 20.sp
                        )
                    }
                }

                if (!isMe) {
                    // Timestamp for other user's messages (on right side of bubble)
                    Text(
                        text = messageTime,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)
                    )
                }
            }
        }

        if (isMe) {
            // Profile picture for my messages (right side)
            Spacer(modifier = Modifier.width(8.dp))
            ProfilePicture(
                profilePicUrl = profilePicUrl,
                userName = message.senderName ?: "You",
                size = 32.dp
            )
        }
    }
}


        @Composable
        fun ProfilePicture(
            profilePicUrl: String,
            userName: String,
            size: Dp = 40.dp
        ) {
            if (profilePicUrl.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(profilePicUrl),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(Color(0xFF7F5A83)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        userName.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value / 2).sp
                    )
                }
            }
        }
