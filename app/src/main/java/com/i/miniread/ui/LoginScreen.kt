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
fun LoginScreen(viewModel: MinifluxViewModel, onLoginSuccess:  () -> Unit) {
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
            label = { Text("用户名") }
        )
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("密码") },
            visualTransformation = PasswordVisualTransformation()
        )
        Button(onClick = {
            coroutineScope.launch {
                viewModel.login(username, password) { error ->
                    if (error != null) {
                        errorMessage = error
                    } else {
                        viewModel.fetchFeeds()
                        onLoginSuccess()
                    }
                }
            }
        }) {
            Text("登录")
        }
        if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
        }
    }
}
