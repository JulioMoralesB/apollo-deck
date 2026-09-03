package com.apollox10.apollodeck.ui.icons

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.vector.ImageVector
import com.apollox10.apollodeck.core.store.IconOverrideStore

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

// The widget's icon picker isn't limited to icons the dashboard happens to
// use — it's a self-contained namespace (ids here don't need to be Lucide
// names) resolved only by widgetIconFor below, never by iconFor. Starts
// with one entry per distinct icon from the dictionary above (same ids, so
// an action's own dashboard icon is always a valid default selection), then
// adds a broader general-purpose set for anything else worth pinning.
val availableIcons: List<Pair<String, ImageVector>> = listOf(
    "refresh" to Icons.Default.Refresh,
    "play" to Icons.Default.PlayArrow,
    "stop" to Icons.Default.Stop,
    "external-link" to Icons.AutoMirrored.Filled.OpenInNew,
    "dashboard" to Icons.Default.Dashboard,
    "git-branch" to Icons.Default.AccountTree,
    "shield-check" to Icons.Default.VerifiedUser,
    "shield-cog" to Icons.Default.Security,
    "send" to Icons.AutoMirrored.Filled.Send,
    "bell-ring" to Icons.Default.NotificationsActive,
    "gift" to Icons.Default.CardGiftcard,
    "hand-platter" to Icons.Default.Restaurant,
    "pickaxe" to Icons.Default.Construction,
    "activity" to Icons.Default.Timeline,
    "server" to Icons.Default.Dns,
    "container" to Icons.Default.Inventory2,
    "settings" to Icons.Default.Settings,
    "bar-chart" to Icons.Default.BarChart,
    "home" to Icons.Default.Home,
    "cloud" to Icons.Default.Cloud,
    "bookmark" to Icons.Default.Bookmark,
    "zap" to Icons.Default.Bolt,
    "network" to Icons.Default.Hub,
    "gamepad" to Icons.Default.SportsEsports,
    "github" to Icons.Default.Code,
    "music4" to Icons.Default.MusicNote,
    // General-purpose additions — not tied to any current dashboard icon.
    "wifi" to Icons.Default.Wifi,
    "battery" to Icons.Default.BatteryFull,
    "lock" to Icons.Default.Lock,
    "unlock" to Icons.Default.LockOpen,
    "power" to Icons.Default.PowerSettingsNew,
    "lightbulb" to Icons.Default.Lightbulb,
    "thermostat" to Icons.Default.Thermostat,
    "download" to Icons.Default.Download,
    "upload" to Icons.Default.Upload,
    "database" to Icons.Default.Storage,
    "terminal" to Icons.Default.Terminal,
    "mail" to Icons.Default.Email,
    "phone" to Icons.Default.Phone,
    "calendar" to Icons.Default.CalendarMonth,
    "clock" to Icons.Default.Schedule,
    "star" to Icons.Default.Star,
    "heart" to Icons.Default.Favorite,
    "trash" to Icons.Default.Delete,
    "printer" to Icons.Default.Print,
    "router" to Icons.Default.Router,
    "speaker" to Icons.Default.Speaker,
    "tv" to Icons.Default.Tv,
    "camera" to Icons.Default.Videocam,
    "fan" to Icons.Default.Air,
    "door" to Icons.Default.MeetingRoom,
    "car" to Icons.Default.DirectionsCar,
    "map" to Icons.Default.Map,
    "search" to Icons.Default.Search,
    "link" to Icons.Default.Link,
    "wrench" to Icons.Default.Build,
)

// Resolves an id from availableIcons — the widget's own namespace, separate
// from iconFor's Lucide dictionary — falling back to a generic icon for an
// id that's no longer in the list (e.g. a dashboard icon set removed since
// a widget was configured).
fun widgetIconFor(name: String): ImageVector =
    availableIcons.firstOrNull { it.first == name }?.second ?: Icons.Default.Info

// What every dashboard-facing icon call site should use instead of iconFor
// directly (the dashboard grid, the widget/tile action pickers) — checks
// IconOverrideStore first, since a user override for a dashboard icon name
// (see ui/icons/IconOverrideScreen.kt) takes precedence over iconFor's
// static dictionary. The override's value is itself an id from
// availableIcons, the same namespace the widget's own icon picker uses.
fun resolvedIconFor(context: Context, name: String?): ImageVector {
    val overrideId = name?.let { IconOverrideStore(context).getOverride(it) }
    return if (overrideId != null) widgetIconFor(overrideId) else iconFor(name)
}
