package com.apollox10.apollodeck.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Folder
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

// Maps the Lucide icon names used in services.yaml (and rendered by the web
// dashboard's utils/icons.jsx) to Material Icons. The web resolves ANY
// Lucide name dynamically (kebab-case -> PascalCase, `LucideIcons[name]`) —
// that's not reproducible here since Lucide has no native Compose
// distribution and Material's names don't correspond 1:1 to Lucide's
// (Lucide "zap" has no Material "Zap"; the equivalent is "Bolt"). So this
// stays a maintained dictionary, backed by material-icons-extended
// (~2000 icons) rather than the ~10-icon base set. Extend as new icon
// names show up in config — grep services.yaml for `icon:` to find them.
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
    // Lucide has no brand logos either — the web's own ALIASES table maps
    // "github" to a generic folder-git icon rather than a real GitHub mark,
    // so a generic "code" icon here is consistent with that, not a downgrade.
    "github" -> Icons.Default.Code
    "music4" -> Icons.Default.MusicNote // Spotify
    "box" -> Icons.Default.Inventory2 // Dockge — same box/package concept as "container"
    else -> Icons.Default.Info
}
