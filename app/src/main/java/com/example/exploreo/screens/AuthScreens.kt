package com.example.exploreo.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onRegister: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val auth = remember { FirebaseAuth.getInstance() }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Exploreo",
            fontSize = 42.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val label = if (showPassword) "Hide" else "Show"
                TextButton(onClick = { showPassword = !showPassword }) { Text(label) }
            }
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            error = null
            if (email.isBlank() || password.length < 6) {
                error = "Enter a valid email and 6+ char password"
            } else {
                auth.signInWithEmailAndPassword(email.trim(), password)
                    .addOnSuccessListener { onLoginSuccess() }
                    .addOnFailureListener { error = it.localizedMessage }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Login") }
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }
        TextButton(onClick = onRegister) { Text("Create account") }
    }
}

@Composable
fun RegisterScreen(onRegistered: () -> Unit, onBack: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val auth = remember { FirebaseAuth.getInstance() }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Create Account",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val label = if (showPassword) "Hide" else "Show"
                TextButton(onClick = { showPassword = !showPassword }) { Text(label) }
            }
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            error = null
            if (name.isBlank() || email.isBlank() || password.length < 6) {
                error = "Fill all fields. Password 6+ chars"
            } else {
                auth.createUserWithEmailAndPassword(email.trim(), password)
                    .addOnSuccessListener { result ->
                        val user = result.user
                        if (user != null && name.isNotBlank()) {
                            val req = UserProfileChangeRequest.Builder().setDisplayName(name).build()
                            user.updateProfile(req).addOnCompleteListener { onRegistered() }
                        } else {
                            onRegistered()
                        }
                    }
                    .addOnFailureListener { error = it.localizedMessage }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Register") }
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }
        TextButton(onClick = onBack) { Text("Back") }
    }
}


