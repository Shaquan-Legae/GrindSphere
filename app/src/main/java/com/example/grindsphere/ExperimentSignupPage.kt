package com.example.grindsphere

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons // <-- Import Icons
import androidx.compose.material.icons.filled.Visibility // <-- Import Visibility icon
import androidx.compose.material.icons.filled.VisibilityOff // <-- Import VisibilityOff icon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation // <-- Import VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lint.kotlin.metadata.Visibility
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ExperimentSignupScreen(
    navController: NavController? = null,
    isPreview: Boolean = false
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var userRole by remember { mutableStateOf<UserRole?>(null) }
    var loading by remember { mutableStateOf(false) }

    // --- NEW: State for password visibility ---
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val auth: FirebaseAuth? = if (!isPreview) FirebaseAuth.getInstance() else null
    val firestore: FirebaseFirestore? = if (!isPreview) FirebaseFirestore.getInstance() else null

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF6A1B9A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Create an Account",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Role selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { userRole = UserRole.HUSTLER },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (userRole == UserRole.HUSTLER) Color(0xFF8E24AA) else Color.White
                    )
                ) {
                    Text(
                        "Hustler",
                        color = if (userRole == UserRole.HUSTLER) Color.White else Color.Black
                    )
                }
                Button(
                    onClick = { userRole = UserRole.CUSTOMER },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (userRole == UserRole.CUSTOMER) Color(0xFF8E24AA) else Color.White
                    )
                ) {
                    Text(
                        "Customer",
                        color = if (userRole == UserRole.CUSTOMER) Color.White else Color.Black
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Surname
            OutlinedTextField(
                value = surname,
                onValueChange = { surname = it },
                label = { Text("Surname") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // --- MODIFIED: Password field with visibility toggle ---
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val image = if (passwordVisible)
                        Icons.Filled.Visibility
                    else Icons.Filled.VisibilityOff

                    val description = if (passwordVisible) "Hide password" else "Show password"

                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, description)
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))

            // --- MODIFIED: Confirm password field with visibility toggle ---
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val image = if (confirmPasswordVisible)
                        Icons.Filled.Visibility
                    else Icons.Filled.VisibilityOff

                    val description = if (confirmPasswordVisible) "Hide password" else "Show password"

                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(imageVector = image, description)
                    }
                }
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Sign Up Button
            Button(
                onClick = {
                    if (!isPreview) {
                        if (name.isNotEmpty() && surname.isNotEmpty() && email.isNotEmpty()
                            && password.isNotEmpty() && confirmPassword.isNotEmpty()
                            && userRole != null
                        ) {
                            if (password == confirmPassword) {
                                loading = true
                                auth?.createUserWithEmailAndPassword(email, password)
                                    ?.addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            val user = auth.currentUser
                                            if (user != null) {
                                                val userMap = hashMapOf(
                                                    "name" to name,
                                                    "surname" to surname,
                                                    "email" to email,
                                                    "role" to userRole!!.name
                                                )
                                                firestore?.collection("users")
                                                    ?.document(user.uid)?.set(userMap)
                                                    ?.addOnSuccessListener {
                                                        loading = false
                                                        Toast.makeText(
                                                            context,
                                                            "Signup successful!",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        navController?.navigate("login")
                                                    }
                                                    ?.addOnFailureListener { e ->
                                                        loading = false
                                                        Toast.makeText(
                                                            context,
                                                            "Error: ${e.message}",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                            }
                                        } else {
                                            loading = false
                                            Toast.makeText(
                                                context,
                                                "Error: ${task.exception?.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                            } else {
                                Toast.makeText(
                                    context,
                                    "Passwords do not match",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                context,
                                "Please fill in all fields and select a role",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E24AA))
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text("Sign Up", fontSize = 18.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = {
                context.startActivity(Intent(context, LoginActivity::class.java))
            }) {
                Text("Have an account? Login.", color = Color.White)
            }
        }
    }
}

enum class UserRole {
    HUSTLER,
    CUSTOMER
}

@Preview(showBackground = true, apiLevel = 35)
@Composable
fun ExperimentSignupScreenPreview() {
    ExperimentSignupScreen(isPreview = true)
}
