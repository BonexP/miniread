package com.i.miniread.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.i.miniread.viewmodel.MinifluxViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(viewModel: MinifluxViewModel, onLoginSuccess: (String) -> Unit) {
    var authToken by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

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
                onLoginSuccess(authToken)
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
