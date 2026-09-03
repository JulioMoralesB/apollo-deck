package com.apollox10.apollodeck.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apollox10.apollodeck.ui.theme.BorderColor
import com.apollox10.apollodeck.ui.theme.ErrorRed
import com.apollox10.apollodeck.ui.theme.MonospaceTextStyle
import com.apollox10.apollodeck.ui.theme.apolloTextFieldColors

@Composable
fun SettingsScreen(
    onSaved: () -> Unit,
    onConfigureWatchTile: () -> Unit,
    onIconOverrides: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                .border(0.5.dp, BorderColor, RoundedCornerShape(8.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Connection",
                style = MonospaceTextStyle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )

            OutlinedTextField(
                value = viewModel.serverUrl,
                onValueChange = { viewModel.serverUrl = it },
                label = { Text("Server URL", style = MonospaceTextStyle) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                colors = apolloTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                "Cloudflare Access (optional)",
                style = MonospaceTextStyle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 6.dp),
            )

            OutlinedTextField(
                value = viewModel.cloudflareAccessClientId,
                onValueChange = { viewModel.cloudflareAccessClientId = it },
                label = { Text("Access Client ID", style = MonospaceTextStyle) },
                singleLine = true,
                colors = apolloTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = viewModel.cloudflareAccessClientSecret,
                onValueChange = { viewModel.cloudflareAccessClientSecret = it },
                label = { Text("Access Client Secret", style = MonospaceTextStyle) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                colors = apolloTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                "Saving requires logging in again.",
                style = MonospaceTextStyle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Button(
                onClick = { viewModel.save(onSaved) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save", style = MonospaceTextStyle)
            }

            viewModel.errorMessage?.let {
                Text(it, style = MonospaceTextStyle, color = ErrorRed, fontSize = 12.sp)
            }

            Text(
                "Watch",
                style = MonospaceTextStyle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 10.dp),
            )
            Button(
                onClick = onConfigureWatchTile,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Configure watch tile", style = MonospaceTextStyle)
            }

            Text(
                "Icons",
                style = MonospaceTextStyle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 10.dp),
            )
            Button(
                onClick = onIconOverrides,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Icon overrides", style = MonospaceTextStyle)
            }
        }
    }
}
