package com.apollox10.apollodeck

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.apollox10.apollodeck.wearsync.PhoneSessionPublisher

// Set by a summary widget row's tap target (see widget/SummaryWidgetRemoteViews.kt
// and widget/CombinedSummaryWidgetRemoteViews.kt) to deep-link straight into
// that service's own panel instead of just opening the dashboard's home grid.
const val EXTRA_OPEN_SERVICE_NAME = "com.apollox10.apollodeck.EXTRA_OPEN_SERVICE_NAME"

class MainActivity : ComponentActivity() {

    private var openServiceName by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openServiceName = intent?.getStringExtra(EXTRA_OPEN_SERVICE_NAME)
        setContent {
            ApolloDeckTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ApolloDeckApp(initialServiceName = openServiceName)
                }
            }
        }
    }

    // MainActivity is launchMode="singleTop" — a widget tap while the app is
    // already running (foreground or backgrounded) reuses this instance and
    // arrives here instead of a fresh onCreate.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_OPEN_SERVICE_NAME)?.let { openServiceName = it }
    }
}

@Composable
fun ApolloDeckApp(initialServiceName: String? = null) {
    val context = LocalContext.current
    var loggedIn by remember { mutableStateOf(TokenStore(context).isLoggedIn()) }

    // Re-publish on every app open while logged in, not just at the moment
    // of login — covers a session that predates this feature, and keeps a
    // paired watch's synced copy fresh if the access token got silently
    // refreshed since the last publish.
    LaunchedEffect(loggedIn) {
        if (loggedIn) PhoneSessionPublisher.publishCurrentSession(context)
    }

    if (loggedIn) {
        DashboardScreen(
            onLogout = {
                TokenStore(context).clear()
                PhoneSessionPublisher.clear(context)
                loggedIn = false
            },
            initialServiceName = initialServiceName,
        )
    } else {
        LoginScreen(onLoginSuccess = { loggedIn = true })
    }
}
