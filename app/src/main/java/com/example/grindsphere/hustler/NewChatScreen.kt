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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore

data class AppUser(
    val uid: String = "",
    val name: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatScreen(onStartChat: (customerUid: String, customerName: String) -> Unit) {
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var users by remember { mutableStateOf(listOf<AppUser>()) }
    var searchQuery by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }

    // Fetch users from Firestore
    LaunchedEffect(Unit) {
        firestore.collection("users")
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    AppUser(
                        uid = doc.id,
                        name = doc.getString("name") ?: doc.id
                    )
                }
                users = list
                loading = false
            }
            .addOnFailureListener { error ->
                Toast.makeText(context, "Failed to load users: ${error.message}", Toast.LENGTH_SHORT).show()
                loading = false
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0D324D), Color(0xFF7F5A83))))
            .padding(16.dp)
    ) {
        Text(
            text = "Start a New Chat",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search users", color = Color.White.copy(alpha = 0.7f)) },
            placeholder = { Text("Type a name", color = Color.White.copy(alpha = 0.5f)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent),
            textStyle = LocalTextStyle.current.copy(color = Color.White),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                cursorColor = Color.White,
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                focusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.5f)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            val filteredUsers = if (searchQuery.isBlank()) users
            else users.filter { it.name.contains(searchQuery, ignoreCase = true) }

            if (filteredUsers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No users found", color = Color.White)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredUsers) { user ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onStartChat(user.uid, user.name) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.2f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(user.name, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
