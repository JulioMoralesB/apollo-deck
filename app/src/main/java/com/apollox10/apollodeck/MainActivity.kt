package com.apollox10.apollodeck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import com.apollox10.apollodeck.core.store.TokenStore
import com.apollox10.apollodeck.ui.login.LoginScreen
import com.apollox10.apollodeck.ui.theme.ApolloDeckTheme
import com.apollox10.apollodeck.ui.theme.MonospaceTextStyle

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
        DashboardPlaceholder(
            onLogout = {
                TokenStore(context).clear()
                loggedIn = false
            },
        )
    } else {
        LoginScreen(onLoginSuccess = { loggedIn = true })
    }
}

// No dashboard screen yet — widgets, tiles, and the service list come next.
@Composable
private fun DashboardPlaceholder(onLogout: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Logged in — dashboard coming soon", style = MonospaceTextStyle)
            Button(onClick = onLogout, modifier = Modifier.padding(top = 16.dp)) {
                Text("Log out", style = MonospaceTextStyle)
            }
        }
    }
}
