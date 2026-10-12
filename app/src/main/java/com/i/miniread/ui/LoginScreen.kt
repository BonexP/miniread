package com.i.miniread.ui

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.i.miniread.R
import com.i.miniread.network.RetrofitInstance
import com.i.miniread.viewmodel.MinifluxViewModel
@Composable
fun LoginScreen(viewModel: MinifluxViewModel, onLoginSuccess: (String, String) -> Unit) {
    var baseUrl by remember { mutableStateOf("") }
    var authToken by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    // 在 Composable 作用域提前取出：onClick lambda 内不能调用 stringResource
    val invalidInputMsg = stringResource(R.string.login_error_invalid_input)

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
            },
            label = { Text(stringResource(R.string.login_base_url)) }
        )
        TextField(
            value = authToken,
            onValueChange = {
                authToken = it
            },
            label = { Text(stringResource(R.string.login_api_token)) }
        )
        Button(onClick = {
            if (baseUrl.isNotEmpty() && authToken.isNotEmpty()) {
                onLoginSuccess(baseUrl, authToken)
            } else {
                errorMessage = invalidInputMsg
            }
        }) {
            Text(stringResource(R.string.login_action))
        }
        if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
        }
    }
}
