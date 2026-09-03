package com.apollox10.apollodeck.wear.presentation.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val isLoading = uiState is LoginUiState.Loading

    // ScalingLazyColumn, not a plain Column, so content gets the round-screen-
    // aware top/bottom insets Wear needs — a fillMaxWidth Chip near either
    // edge of a plain Column gets clipped by the physical bezel curve.
    ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
        item(key = "title") {
            Text(
                text = "Apollo Deck",
                style = MaterialTheme.typography.title3,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            )
        }

        item(key = "server") {
            WearTextField(
                label = "Server",
                value = viewModel.serverUrl,
                onValueChange = { viewModel.serverUrl = it },
                keyboardType = KeyboardType.Uri,
                enabled = !isLoading,
            )
        }
        item(key = "username") {
            WearTextField(
                label = "Username",
                value = viewModel.username,
                onValueChange = { viewModel.username = it },
                enabled = !isLoading,
            )
        }
        item(key = "password") {
            WearTextField(
                label = "Password",
                value = viewModel.password,
                onValueChange = { viewModel.password = it },
                isPassword = true,
                imeAction = ImeAction.Done,
                enabled = !isLoading,
            )
        }

        item(key = "sign_in") {
            Chip(
                onClick = { viewModel.login(onLoginSuccess) },
                enabled = !isLoading,
                label = { Text(if (isLoading) "Signing in…" else "Sign In") },
                colors = ChipDefaults.primaryChipColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (uiState is LoginUiState.Error) {
            item(key = "error") {
                Text(
                    text = (uiState as LoginUiState.Error).message,
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.caption2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                )
            }
        }
    }
}

// Wear Compose Material has no text field of its own (Chip/Card are for
// actions, not input) — this hand-rolls one from a plain BasicTextField so
// it matches the rest of the screen's styling.
@Composable
private fun WearTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.surface, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.caption3, color = MaterialTheme.colors.onSurfaceVariant)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = TextStyle(color = MaterialTheme.colors.onSurface, fontSize = 14.sp),
            cursorBrush = SolidColor(MaterialTheme.colors.primary),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
