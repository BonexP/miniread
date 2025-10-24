package com.i.miniread.ui

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.i.miniread.network.RetrofitInstance
import com.i.miniread.viewmodel.MinifluxViewModel
@Composable
fun LoginScreen(viewModel: MinifluxViewModel, onLoginSuccess: (String, String) -> Unit) {
    var baseUrl by remember { mutableStateOf("") }
    var authToken by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = baseUrl,
            onValueChange = {
                baseUrl = it
                Log.d("LoginScreen", "LoginScreen: BaseURL: $baseUrl")
            },
            label = { Text("Instance Base URL") }
        )
        TextField(
            value = authToken,
            onValueChange = {
                authToken = it
                Log.d("LoginScreen", "API Token input: $authToken")
            },
            label = { Text("API Token") }
        )
        Button(onClick = {
            if (baseUrl.isNotEmpty() && authToken.isNotEmpty()) {
                onLoginSuccess(baseUrl, authToken)
            } else {
                errorMessage = "Please enter a valid Base URL and API token."
            }
        }) {
            Text("Login")
        }
        if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
        }
    }
}
