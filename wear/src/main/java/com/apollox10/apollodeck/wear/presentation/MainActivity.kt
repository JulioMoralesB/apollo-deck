package com.apollox10.apollodeck.wear.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

// Scaffold placeholder. The real Wear surface is a Tile (androidx.wear.tiles),
// not this activity — this is just the app entry point shown when launched
// from the watch face's app list. Talks to a self-hosted
// apollo-server-dashboard backend once implemented; see README.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ApolloDeckWearPlaceholder()
            }
        }
    }
}

@Composable
fun ApolloDeckWearPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Apollo Deck")
    }
}
