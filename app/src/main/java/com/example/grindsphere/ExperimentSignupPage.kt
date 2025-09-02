package com.example.grindsphere

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlin.text.isBlank

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperimentSignupScreen() {
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var userRole by remember { mutableStateOf<UserRole?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val auth: FirebaseAuth = Firebase.auth
    val firestore = Firebase.firestore

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Create an Account", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(24.dp))

            Text("I am a:", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { userRole = UserRole.HUSTLER }, enabled = !isLoading) {
                    Text("Hustler")
                }
                Button(onClick = { userRole = UserRole.CUSTOMER }, enabled = !isLoading) {
                    Text("Customer")
                }
            }

            if (userRole != null) {
                Text(
                    "Selected role: ${userRole!!.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier
                .background(color=Color.Black)
                .height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = surname,
                onValueChange = { surname = it },
                label = { Text("Surname") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                enabled = !isLoading
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                enabled = !isLoading
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        // 1. Validate Input
                        if (userRole == null) {
                            Toast.makeText(context, "Please select a role", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        if (name.isBlank() || surname.isBlank() || email.isBlank() || password.isBlank()) {
                            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        if (password != confirmPassword) {
                            Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        isLoading = true // Show loading spinner

                        // 2. Call Firebase Auth
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val firebaseUser = auth.currentUser
                                    firebaseUser?.let {
                                        // 3. Save user details to Firestore
                                        val userMap = kotlin.collections.hashMapOf(
                                            "name" to name,
                                            "surname" to surname,
                                            "email" to email,
                                            "role" to userRole!!.name
                                        )
                                        firestore.collection("users").document(it.uid).set(userMap)
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "Signup successful! Welcome!", Toast.LENGTH_LONG).show()
                                                // Clear fields
                                                name = ""
                                                surname = ""
                                                email = ""
                                                password = ""
                                                confirmPassword = ""
                                                userRole = null
                                                isLoading = false
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(context, "Error saving user data: ${e.message}", Toast.LENGTH_LONG).show()
                                                isLoading = false
                                            }
                                    }
                                } else {
                                    val exception = task.exception
                                    Toast.makeText(context, "Authentication failed: ${exception?.message}", Toast.LENGTH_LONG).show()
                                    isLoading = false // Hide loading spinner
                                }
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text("Sign Up")
            }
        }

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

enum class UserRole {
    HUSTLER,
    CUSTOMER
}

@Preview(showBackground = true)
@Composable
fun ExperimentSignupScreenPreview() {
    ExperimentSignupScreen()
}