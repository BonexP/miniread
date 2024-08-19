package com.i.miniread.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.i.miniread.viewmodel.MinifluxViewModel

@Composable
fun LoginScreen(viewModel: MinifluxViewModel, onLoginSuccess: (String) -> Unit) {
    var authToken by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = authToken,
            onValueChange = {
                authToken = it
                Log.d("LoginScreen", "API Token input: $authToken")
            },
            label = { Text("API Token") }
        )
        Button(onClick = {
            Log.d("LoginScreen", "Login button clicked")
            if (authToken.isNotEmpty()) {
                try {
                    Log.d("LoginScreen", "Valid token: $authToken")
                    onLoginSuccess(authToken)
                } catch (e: Exception) {
                    Log.e("LoginScreen", "Error during login success handling", e)
                }
            } else {
                Log.d("LoginScreen", "Empty token input")
                errorMessage = "Please enter a valid API token."
            }
        }) {
            Text("Login")
        }
        if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
            Log.d("LoginScreen", "Error message displayed: $errorMessage")
        }
    }
}
