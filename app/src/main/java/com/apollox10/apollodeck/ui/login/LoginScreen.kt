package com.apollox10.apollodeck.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.view.autofill.AutofillManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apollox10.apollodeck.ui.theme.BorderColor
import com.apollox10.apollodeck.ui.theme.ErrorRed
import com.apollox10.apollodeck.ui.theme.MonospaceTextStyle

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val isLoading = uiState is LoginUiState.Loading
    val context = LocalContext.current
    val autofillManager = remember { context.getSystemService(AutofillManager::class.java) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 360.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Apollo Deck",
                style = MonospaceTextStyle,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Column(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                    .border(0.5.dp, BorderColor, RoundedCornerShape(8.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = viewModel.serverUrl,
                    onValueChange = { viewModel.serverUrl = it },
                    label = { Text("Server URL", style = MonospaceTextStyle) },
                    placeholder = { Text("https://dashboard.example.com", style = MonospaceTextStyle) },
                    singleLine = true,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    colors = apolloTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = viewModel.username,
                    onValueChange = { viewModel.username = it },
                    label = { Text("Username", style = MonospaceTextStyle) },
                    singleLine = true,
                    enabled = !isLoading,
                    colors = apolloTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .autofill(listOf(AutofillType.Username)) { viewModel.username = it },
                )

                OutlinedTextField(
                    value = viewModel.password,
                    onValueChange = { viewModel.password = it },
                    label = { Text("Password", style = MonospaceTextStyle) },
                    singleLine = true,
                    enabled = !isLoading,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = apolloTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .autofill(listOf(AutofillType.Password)) { viewModel.password = it },
                )

                TextButton(
                    onClick = { viewModel.showCloudflareAccessFields = !viewModel.showCloudflareAccessFields },
                ) {
                    Text(
                        text = if (viewModel.showCloudflareAccessFields) {
                            "Hide Cloudflare Access"
                        } else {
                            "Behind Cloudflare Access?"
                        },
                        style = MonospaceTextStyle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }

                if (viewModel.showCloudflareAccessFields) {
                    OutlinedTextField(
                        value = viewModel.cloudflareAccessClientId,
                        onValueChange = { viewModel.cloudflareAccessClientId = it },
                        label = { Text("Access Client ID", style = MonospaceTextStyle) },
                        singleLine = true,
                        enabled = !isLoading,
                        colors = apolloTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = viewModel.cloudflareAccessClientSecret,
                        onValueChange = { viewModel.cloudflareAccessClientSecret = it },
                        label = { Text("Access Client Secret", style = MonospaceTextStyle) },
                        singleLine = true,
                        enabled = !isLoading,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = apolloTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Button(
                    onClick = {
                        viewModel.login {
                            autofillManager?.commit()
                            onLoginSuccess()
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Enter", style = MonospaceTextStyle)
                    }
                }

                if (uiState is LoginUiState.Error) {
                    Text(
                        text = (uiState as LoginUiState.Error).message,
                        style = MonospaceTextStyle,
                        color = ErrorRed,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

// The stable, non-experimental Compose autofill API — AutofillNode +
// LocalAutofill — rather than the semantics-based ContentType API, which is
// still internal at this project's Compose BOM version.
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Modifier.autofill(
    autofillTypes: List<AutofillType>,
    onFill: (String) -> Unit,
): Modifier {
    val autofillNode = remember { AutofillNode(autofillTypes = autofillTypes, onFill = onFill) }
    val autofill = LocalAutofill.current
    LocalAutofillTree.current += autofillNode

    return this
        .onGloballyPositioned { autofillNode.boundingBox = it.boundsInWindow() }
        .onFocusChanged { state ->
            autofill?.run {
                if (state.isFocused) requestAutofillForNode(autofillNode) else cancelAutofillForNode(autofillNode)
            }
        }
}

@Composable
private fun apolloTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.onBackground,
    unfocusedBorderColor = BorderColor,
    focusedTextColor = MaterialTheme.colorScheme.onBackground,
    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
    cursorColor = MaterialTheme.colorScheme.onBackground,
)
