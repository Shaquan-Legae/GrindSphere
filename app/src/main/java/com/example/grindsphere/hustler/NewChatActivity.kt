package com.example.grindsphere.hustler

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class ChatUser(
    val uid: String,
    val name: String,
    val profilePicUrl: String = ""
)

class NewChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NewChatScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatScreen() {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    var users by remember { mutableStateOf(listOf<ChatUser>()) }
    var filteredUsers by remember { mutableStateOf(listOf<ChatUser>()) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Fetch all users except current
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            firestore.collection("users")
                .whereNotEqualTo("uid", uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }
                    val list = snapshot?.documents?.mapNotNull { doc ->
                        val userUid = doc.getString("uid") ?: return@mapNotNull null
                        val name = doc.getString("name") ?: "Unknown"
                        val profilePic = doc.getString("profilePicUrl") ?: ""
                        ChatUser(userUid, name, profilePic)
                    } ?: listOf()
                    users = list
                    filteredUsers = list
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Start New Chat", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D324D))
            )
        },
        floatingActionButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Bottom-left Search button
                ExtendedFloatingActionButton(
                    text = { Text("Search") },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    onClick = { showSearchBar = true },
                    containerColor = Color(0xFF7F5A83),
                    contentColor = Color.White
                )
                // Bottom-right New Chat button (optional action)
                ExtendedFloatingActionButton(
                    text = { Text("New Chat") },
                    icon = { Icon(Icons.Default.Add, contentDescription = "New Chat") },
                    onClick = { Toast.makeText(context, "Create new chat clicked", Toast.LENGTH_SHORT).show() },
                    containerColor = Color(0xFF7F5A83),
                    contentColor = Color.White
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF0D324D), Color(0xFF7F5A83))))
                .padding(paddingValues)
        ) {

            // User List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                items(filteredUsers) { user ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable {
                                startConversation(context, currentUser!!.uid, user)
                            },
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
                                    .background(Color.Gray, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = user.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Search Overlay
            if (showSearchBar) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { showSearchBar = false } // dismiss when clicking outside
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                filteredUsers = if (it.isBlank()) users
                                else users.filter { user -> user.name.contains(it, ignoreCase = true) }
                            },
                            label = { Text("Search Users") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { /* optional: hide keyboard */ }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        )
                    }
                }
            }
        }
    }
}

// Top-level function
fun startConversation(context: android.content.Context, currentUid: String, user: ChatUser) {
    val firestore = FirebaseFirestore.getInstance()
    val convoRef = firestore.collection("messages")

    convoRef
        .whereArrayContains("participants", currentUid)
        .get()
        .addOnSuccessListener { snapshot ->
            val existing = snapshot.documents.firstOrNull { doc ->
                val participants = doc.get("participants") as? List<*>
                participants?.contains(user.uid) == true
            }
            if (existing != null) {
                val intent = Intent(context, ChatScreenActivity::class.java)
                intent.putExtra("conversationId", existing.id)
                intent.putExtra("customerUid", user.uid)
                intent.putExtra("customerName", user.name)
                context.startActivity(intent)
            } else {
                val newConvo = hashMapOf(
                    "participants" to listOf(currentUid, user.uid),
                    "timestamp" to System.currentTimeMillis(),
                    "lastMessage" to ""
                )
                convoRef.add(newConvo).addOnSuccessListener { docRef ->
                    val intent = Intent(context, ChatScreenActivity::class.java)
                    intent.putExtra("conversationId", docRef.id)
                    intent.putExtra("customerUid", user.uid)
                    intent.putExtra("customerName", user.name)
                    context.startActivity(intent)
                }.addOnFailureListener { e ->
                    Toast.makeText(
                        context,
                        "Failed to start chat: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}
