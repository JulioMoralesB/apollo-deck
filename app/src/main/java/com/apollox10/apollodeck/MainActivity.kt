package com.apollox10.apollodeck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.apollox10.apollodeck.core.store.TokenStore
import com.apollox10.apollodeck.ui.login.LoginScreen
import com.apollox10.apollodeck.ui.theme.ApolloDeckTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ApolloDeckTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ApolloDeckApp()
                }
            }
        }
    }
}

@Composable
fun ApolloDeckApp() {
    val context = LocalContext.current
    var loggedIn by remember { mutableStateOf(TokenStore(context).isLoggedIn()) }

    if (loggedIn) {
        DashboardPlaceholder()
    } else {
        LoginScreen(onLoginSuccess = { loggedIn = true })
    }
}

// No dashboard screen yet — widgets, tiles, and the service list come next.
@Composable
private fun DashboardPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Logged in — dashboard coming soon")
    }
}
