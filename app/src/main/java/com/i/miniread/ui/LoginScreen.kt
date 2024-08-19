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
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") }
        )
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation()
        )
        Button(onClick = {
            coroutineScope.launch {
                try {
                    // 假设这里使用了 Miniflux API 进行身份验证并获得 authToken
                    val authToken = viewModel.login(username, password) // 这个方法需要在 ViewModel 中实现
                    onLoginSuccess(authToken)
                } catch (e: Exception) {
                    errorMessage = "Login failed: ${e.message}"
                }
            }
        }) {
            Text("Login")
        }
        if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
        }
    }
}
