package com.apollox10.apollodeck.wear.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material.MaterialTheme
import com.apollox10.apollodeck.core.store.TokenStore
import com.apollox10.apollodeck.wear.presentation.actions.ActionsScreen
import com.apollox10.apollodeck.wear.presentation.login.LoginScreen

// Talks to a self-hosted apollo-server-dashboard backend. Standalone —
// android:standalone=true in the manifest — so this never depends on a
// paired phone: its own login, its own stored session (core's TokenStore).
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ApolloDeckWearApp()
            }
        }
    }
}

@Composable
fun ApolloDeckWearApp() {
    val context = LocalContext.current
    var loggedIn by remember { mutableStateOf(TokenStore(context).isLoggedIn()) }

    if (loggedIn) {
        ActionsScreen(
            onLogout = {
                TokenStore(context).clear()
                loggedIn = false
            },
        )
    } else {
        LoginScreen(onLoginSuccess = { loggedIn = true })
    }
}
