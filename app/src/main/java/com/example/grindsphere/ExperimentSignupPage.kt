package com.example.grindsphere

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
// It's good practice to add password visibility toggling
// import androidx.compose.ui.text.input.PasswordVisualTransformation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperimentSignupScreen() {
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var userRole by remember { mutableStateOf<UserRole?>(null) } // null, HUSTLER, or CUSTOMER

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Create Account", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        Text("I am a:", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { userRole = UserRole.HUSTLER }) {
                Text("Hustler")
            }
            Button(onClick = { userRole = UserRole.CUSTOMER }) {
                Text("Customer")
            }
        }

        if (userRole != null) {
            Text("Selected role: ${userRole!!.name}", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = surname,
            onValueChange = { surname = it },
            label = { Text("Surname") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
            // visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            modifier = Modifier.fillMaxWidth()
            // visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                // TODO: Implement signup logic
                // 1. Validate input (passwords match, email format, role selected, name/surname not empty)
                //    Example using !isNullOrBlank() for validation:
                //    if (name.isNullOrBlank() || surname.isNullOrBlank() || ...) {
                //        // Show error
                //        return@Button
                //    }
                // 2. Call Firebase Auth to create user
                // 3. Potentially save user role, name, surname to Firestore/Realtime Database
            },
            modifier = Modifier.fillMaxWidth(),
            //enabled = userRole != null &&
                   /* !name.isNullOrBlank() &&
                    !surname.isNullOrBlank() &&
                    !email.isNullOrBlank() &&
                    !password.isNullOrBlank() &&
                    password == confirmPassword // confirmPassword will also be not blank if it matches a non-blank password*/
        ) {
            Text("Sign Up")
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
