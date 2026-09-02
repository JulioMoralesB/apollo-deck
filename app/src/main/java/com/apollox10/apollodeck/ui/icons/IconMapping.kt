package com.apollox10.apollodeck.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.ui.graphics.vector.ImageVector

// Maps the Lucide icon names used in services.yaml (and rendered by the web
// dashboard's utils/icons.jsx) to Material Icons. Lucide itself is a
// JS/SVG-only library with no native Compose distribution, so this is a
// deliberate re-mapping rather than a reuse of the same icon set — backed
// by material-icons-extended (~2000 icons) instead of the ~10-icon base set,
// for close-enough coverage without a third-party icon dependency of
// uncertain upkeep. Extend this as new icon names show up in config.
fun iconFor(name: String?): ImageVector = when (name) {
    "refresh", "rotate-cw" -> Icons.Default.Refresh
    "player-play", "play" -> Icons.Default.PlayArrow
    "square-x", "stop" -> Icons.Default.Stop
    "external-link" -> Icons.AutoMirrored.Filled.OpenInNew
    "layout-dashboard" -> Icons.Default.Dashboard
    "folder-git-2" -> Icons.Default.Folder
    "shield-check" -> Icons.Default.VerifiedUser
    "shield-cog" -> Icons.Default.Security
    "send" -> Icons.AutoMirrored.Filled.Send
    "bell-ring" -> Icons.Default.NotificationsActive
    "gift" -> Icons.Default.CardGiftcard
    "hand-platter" -> Icons.Default.Restaurant
    "pickaxe" -> Icons.Default.Construction
    "activity" -> Icons.Default.Timeline
    "server" -> Icons.Default.Dns
    "container" -> Icons.Default.Inventory2
    "settings" -> Icons.Default.Settings
    "bar-chart" -> Icons.Default.BarChart
    "home" -> Icons.Default.Home
    else -> Icons.Default.Info
}
