package com.example.grindsphere.customer

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AttachFile
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.grindsphere.hustler.MessageBubble
import com.example.grindsphere.models.Conversation
import com.example.grindsphere.models.Message
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerChatScreen(
    conversationId: String,
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
    var hustlerName by remember { mutableStateOf("Service Provider") }
    var serviceName by remember { mutableStateOf("Service") }
    var hustlerProfilePic by remember { mutableStateOf("") }
    var currentUserProfilePic by remember { mutableStateOf("") }
    var hustlerId by remember { mutableStateOf("") }
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // ------------------- Attachment Picker -------------------
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedFileUri = uri
    }

    // ------------------- Attachment Dropdown -------------------
    var showAttachmentMenu by remember { mutableStateOf(false) }

    // ------------------ Send Attachments --------------------
    fun sendAttachment(uri: Uri) {
        if (currentUser == null) return
        coroutineScope.launch {
            try {
                val fileName = uri.lastPathSegment ?: "Attachment"
                val storageRef = FirebaseStorage.getInstance().reference
                val fileRef = storageRef.child("attachments/${currentUser.uid}/$fileName")

                fileRef.putFile(uri)
                    .addOnSuccessListener {
                        fileRef.downloadUrl.addOnSuccessListener { downloadUri ->
                            coroutineScope.launch {
                                val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
                                val userName = userDoc.getString("name") ?: "Customer"

                                val mimeType = context.contentResolver.getType(uri) ?: "*/*"

                                val messageData = hashMapOf(
                                    "senderId" to currentUser.uid,
                                    "senderName" to userName,
                                    "senderProfilePicUrl" to currentUserProfilePic,
                                    "text" to "", // optional text
                                    "attachmentUrl" to downloadUri.toString(),
                                    "fileType" to mimeType, // ✅ save MIME type
                                    "timestamp" to Timestamp.now(),
                                    "type" to "attachment",
                                    "isRead" to false
                                )

                                firestore.collection("conversations")
                                    .document(conversationId)
                                    .collection("messages")
                                    .add(messageData)
                                    .await()

                                firestore.collection("conversations")
                                    .document(conversationId)
                                    .update(
                                        mapOf(
                                            "lastMessage" to "[Attachment] $fileName",
                                            "lastMessageTimestamp" to FieldValue.serverTimestamp()
                                        )
                                    )

                                selectedFileUri = null
                            }
                        }.addOnFailureListener { errorMessage = "Failed to get download URL" }
                    }
                    .addOnFailureListener { errorMessage = "Failed to upload attachment" }
            } catch (e: Exception) {
                errorMessage = "Failed to send attachment: ${e.message}"
            }
        }
    }

    // ------------------- Open attachment -------------------
    fun openAttachment(uri: String, type: String?) {
        try {
            if (type?.startsWith("image") == true || type?.startsWith("video") == true) {
                // Open in-app for images/videos
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(uri), type)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(intent)
            } else {
                // Ask system to open file using external app (PDF, DOC, etc.)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(uri), type ?: "*/*")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(Intent.createChooser(intent, "Open with"))
            }
        } catch (e: Exception) {
            Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show()
        }
    }

    // ------------------- Send text message -------------------
    fun sendMessage() {
        if (messageText.isBlank() || currentUser == null) return
        coroutineScope.launch {
            try {
                val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
                val userName = userDoc.getString("name") ?: "Customer"

                val messageData = hashMapOf(
                    "senderId" to currentUser.uid,
                    "senderName" to userName,
                    "senderProfilePicUrl" to currentUserProfilePic,
                    "text" to messageText,
                    "timestamp" to Timestamp.now(),
                    "type" to "text",
                    "isRead" to false
                )

                firestore.collection("conversations")
                    .document(conversationId)
                    .collection("messages")
                    .add(messageData)
                    .await()

                firestore.collection("conversations")
                    .document(conversationId)
                    .update(
                        mapOf(
                            "lastMessage" to messageText,
                            "lastMessageTimestamp" to FieldValue.serverTimestamp()
                        )
                    )

                messageText = ""
            } catch (e: Exception) {
                errorMessage = "Failed to send message: ${e.message}"
                Log.e("CustomerChatScreen", "Failed to send message", e)
            }
        }
    }

    // ------------------- Mark messages as read -------------------
    fun markMessagesAsRead(conversationId: String, currentUserId: String) {
        firestore.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .whereEqualTo("isRead", false)
            .whereNotEqualTo("senderId", currentUserId)
            .get()
            .addOnSuccessListener { snapshot -> snapshot.documents.forEach { it.reference.update("isRead", true) } }
    }

    // ------------------- Effects -------------------
    LaunchedEffect(conversationId, currentUser?.uid) {
        if (conversationId.isNotEmpty() && currentUser?.uid != null) markMessagesAsRead(conversationId, currentUser.uid)
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            errorMessage = null
        }
    }

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { doc -> currentUserProfilePic = doc.getString("profilePicUrl") ?: "" }
        }
    }

    LaunchedEffect(conversationId) {
        try {
            firestore.collection("conversations").document(conversationId)
                .addSnapshotListener { snapshot, _ ->
                    conversation = snapshot?.toObject<Conversation>()
                    val currentUserId = currentUser?.uid
                    if (currentUserId != null && conversation != null) {
                        hustlerId = conversation!!.participants.find { it != currentUserId } ?: ""
                        hustlerName = conversation!!.participantNames[hustlerId] ?: "Service Provider"
                        serviceName = conversation!!.serviceName ?: "Service"
                        if (hustlerId.isNotEmpty()) {
                            firestore.collection("users").document(hustlerId).get()
                                .addOnSuccessListener { doc -> hustlerProfilePic = doc.getString("profilePicUrl") ?: "" }
                        }
                    }
                    loading = false
                }
        } catch (e: Exception) {
            errorMessage = "Error loading conversation: ${e.message}"
            loading = false
        }
    }

    LaunchedEffect(conversationId) {
        try {
            firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("CustomerChatScreen", "Error loading messages: ${error.message}")
                        return@addSnapshotListener
                    }
                    messages = snapshot?.documents?.mapNotNull { it.toObject<Message>()?.copy(id = it.id) } ?: emptyList()
                    if (messages.isNotEmpty()) {
                        coroutineScope.launch { scrollState.animateScrollToItem(messages.size - 1) }
                    }
                }
        } catch (e: Exception) {
            Log.e("CustomerChatScreen", "Error setting up messages listener: ${e.message}")
        }
    }

    // ------------------- UI -------------------
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (hustlerProfilePic.isNotEmpty()) {
                            Image(
                                painter = rememberAsyncImagePainter(hustlerProfilePic),
                                contentDescription = "Hustler Profile",
                                modifier = Modifier.size(40.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF7F5A83)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(hustlerName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(hustlerName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(serviceName, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D324D))
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues).background(
                Brush.verticalGradient(listOf(Color(0xFF0D324D), Color(0xFF7F5A83)))
            )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Messages list
                if (loading) {
                    Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Loading messages...", color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                } else if (messages.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "No messages", tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No messages yet", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                            Text("Start the conversation with $hustlerName!", color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Service: $serviceName", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                        state = scrollState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(messages) { message ->
                            MessageBubble(
                                message = message,
                                isMe = message.senderId == currentUser?.uid,
                                currentUserProfilePic = currentUserProfilePic,
                                otherUserProfilePic = hustlerProfilePic,
                                onAttachmentClick = { url, type -> openAttachment(url, type) } // <-- Added callback
                            )
                        }
                    }
                }

                // ------------------- File preview -------------------
                selectedFileUri?.let { uri ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = "Attachment Preview",
                                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(uri.lastPathSegment ?: "Attachment", color = Color.White, fontWeight = FontWeight.Medium, maxLines = 1)
                                Text("Tap send to upload", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            }
                            IconButton(onClick = { selectedFileUri = null }) {
                                Icon(Icons.Default.AttachFile, contentDescription = "Remove Attachment", tint = Color.White)
                            }
                        }
                    }
                }

                // Message input row
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { filePickerLauncher.launch("*/*") }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.AttachFile, contentDescription = "Attach File", tint = Color.White)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

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
                        keyboardActions = KeyboardActions(onSend = { if (selectedFileUri != null) sendAttachment(selectedFileUri!!) else sendMessage() }),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    IconButton(
                        onClick = { if (selectedFileUri != null) sendAttachment(selectedFileUri!!) else sendMessage() },
                        enabled = messageText.isNotBlank() || selectedFileUri != null,
                        modifier = Modifier.size(56.dp).background(
                            if (messageText.isNotBlank() || selectedFileUri != null) Color(0xFFFFD700) else Color.Gray.copy(alpha = 0.5f),
                            CircleShape
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = if (messageText.isNotBlank() || selectedFileUri != null) Color.Black else Color.White)
                    }
                }
            }
        }
    }
}
