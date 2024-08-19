package com.i.miniread.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.i.miniread.viewmodel.MinifluxViewModel

@Composable
fun LoginScreen(viewModel: MinifluxViewModel, onLoginSuccess: () -> Unit) {
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
            onValueChange = { authToken = it },
            label = { Text("API Token") }
        )
        Button(onClick = {
            if (authToken.isNotEmpty()) {
                viewModel.setAuthToken(authToken)
                viewModel.fetchFeeds()
                onLoginSuccess()
            } else {
                errorMessage = "Please enter a valid API token."
            }
        }) {
            Text("Login")
        }
        if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
        }
    }
}
