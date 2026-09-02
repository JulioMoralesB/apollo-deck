package com.apollox10.apollodeck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.apollox10.apollodeck.core.store.TokenStore
import com.apollox10.apollodeck.ui.dashboard.DashboardScreen
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
        DashboardScreen(
            onLogout = {
                TokenStore(context).clear()
                loggedIn = false
            },
        )
    } else {
        LoginScreen(onLoginSuccess = { loggedIn = true })
    }
}
