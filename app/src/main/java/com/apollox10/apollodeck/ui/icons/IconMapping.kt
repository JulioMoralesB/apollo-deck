package com.apollox10.apollodeck.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

// A partial mapping from the Lucide icon names used in services.yaml (and
// rendered by the web dashboard's utils/icons.jsx) to Material Icons — not
// a 1:1 set, just enough coverage for the action icons seen in this
// project's own config so far. Sticks to the base "material-icons-core" set
// (bundled with material3) rather than adding the much larger
// "material-icons-extended" dependency for a handful of names. Falls back
// to a generic icon rather than failing when a name isn't mapped.
fun iconFor(name: String?): ImageVector = when (name) {
    "refresh", "rotate-cw" -> Icons.Default.Refresh
    "player-play", "play", "shield-check", "shield-cog" -> Icons.Default.PlayArrow
    "square-x", "stop" -> Icons.Default.Close
    "external-link", "layout-dashboard", "folder-git-2" -> Icons.AutoMirrored.Filled.ArrowForward
    "send" -> Icons.AutoMirrored.Filled.Send
    "bell-ring" -> Icons.Default.Notifications
    "settings" -> Icons.Default.Settings
    "gift", "hand-platter", "pickaxe", "activity" -> Icons.Default.Star
    "server" -> Icons.Default.Build
    else -> Icons.Default.Info
}
