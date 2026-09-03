package com.apollox10.apollodeck.wear.icons

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

// Same id namespace as the phone app's ui/icons/IconMapping.kt
// availableIcons — an icon override chosen on the phone (see
// core/.../store/IconOverrideStore.kt) stores one of these ids, so both
// platforms need to resolve it to the same Material icon.
private val overridableIcons: List<Pair<String, ImageVector>> = listOf(
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

private fun overridableIconFor(id: String): ImageVector =
    overridableIcons.firstOrNull { it.first == id }?.second ?: Icons.Default.Info

// What every icon call site here should use instead of iconFor directly
// (both tiles) — checks IconOverrideStore first, since a phone-set
// override for a dashboard icon name takes precedence over iconFor's
// static dictionary.
fun resolvedIconFor(context: Context, name: String?): ImageVector {
    val overrideId = name?.let { IconOverrideStore(context).getOverride(it) }
    return if (overrideId != null) overridableIconFor(overrideId) else iconFor(name)
}
