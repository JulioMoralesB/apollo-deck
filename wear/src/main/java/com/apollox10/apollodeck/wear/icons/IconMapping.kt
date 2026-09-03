package com.apollox10.apollodeck.wear.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.ui.graphics.vector.ImageVector

// Same Lucide-name -> Material Icon dictionary as the phone app's
// ui/icons/IconMapping.kt, duplicated rather than shared because it depends
// on Compose Material Icons Extended, which only the app/ and wear/ modules
// pull in (core/ deliberately has no Android UI deps). Keep in sync when
// new icon names show up in services.yaml.
fun iconFor(name: String?): ImageVector = when (name) {
    "refresh", "rotate-cw" -> Icons.Default.Refresh
    "player-play", "play" -> Icons.Default.PlayArrow
    "square-x", "stop" -> Icons.Default.Stop
    "external-link" -> Icons.AutoMirrored.Filled.OpenInNew
    "layout-dashboard", "dashboard" -> Icons.Default.Dashboard
    "folder-git-2", "git-branch" -> Icons.Default.AccountTree
    "shield-check" -> Icons.Default.VerifiedUser
    "shield-cog" -> Icons.Default.Security
    "send", "send-horizontal" -> Icons.AutoMirrored.Filled.Send
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
    "cloud" -> Icons.Default.Cloud
    "bookmark" -> Icons.Default.Bookmark
    "zap" -> Icons.Default.Bolt
    "network" -> Icons.Default.Hub
    "gamepad" -> Icons.Default.SportsEsports
    "github" -> Icons.Default.Code
    "music4" -> Icons.Default.MusicNote
    "box" -> Icons.Default.Inventory2
    else -> Icons.Default.Info
}
