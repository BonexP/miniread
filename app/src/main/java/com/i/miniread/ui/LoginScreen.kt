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
            if (authToken.isNotEmpty()) {
                Log.d("LoginScreen", "Login button clicked, token: $authToken")
                viewModel.setAuthToken(authToken)
                onLoginSuccess(authToken)
            } else {
                Log.d("LoginScreen", "Login button clicked with empty token")
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
