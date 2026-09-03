package com.apollox10.apollodeck.wear.tile

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// What one pinned single-action tile is bound to. Keyed by the Tile's own
// numeric tileId (androidx.wear.tiles.RequestBuilders.TileRequest#getTileId,
// available since tiles 1.6.0) so the tile can be pinned more than once,
// each instance independently configured — the same shape as the phone
// widget's per-appWidgetId config, just with a different id source (a Tile
// has no per-instance configure Activity extras of its own; tileId is
// threaded through as a LaunchAction intent extra instead, see
// ActionTileService/TileConfigActivity).
@Serializable
data class TileActionConfig(
    val serviceName: String,
    val label: String,
    val endpoint: String,
    val method: String,
    val confirm: Boolean,
    // The action's own dashboard icon at configure time — see
    // wear/.../icons/IconMapping.kt. No per-tile icon picker like the
    // widget has; a Tile's config screen only has room to pick the action.
    val iconName: String,
    // Chosen from TILE_ACCENT_COLORS in the same configure flow. Defaults
    // to the palette's first entry (the old hardcoded blue) so a config
    // saved before this field existed keeps rendering the same way.
    val accentColor: Int = TileColors.primary,
)

enum class TileActionStatus { Success, Error }

private const val PREFS_NAME = "apollo_deck_tiles"
private fun configKey(tileId: Int) = "action_config_$tileId"
private fun statusKey(tileId: Int) = "action_status_$tileId"

fun saveTileActionConfig(context: Context, tileId: Int, config: TileActionConfig) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(configKey(tileId), Json.encodeToString(config))
        .apply()
}

fun loadTileActionConfig(context: Context, tileId: Int): TileActionConfig? {
    val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(configKey(tileId), null) ?: return null
    return try {
        Json.decodeFromString(raw)
    } catch (e: Exception) {
        null
    }
}

fun setTileActionStatus(context: Context, tileId: Int, status: TileActionStatus?) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .apply {
            if (status == null) remove(statusKey(tileId)) else putString(statusKey(tileId), status.name)
        }
        .apply()
}

fun loadTileActionStatus(context: Context, tileId: Int): TileActionStatus? {
    val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(statusKey(tileId), null) ?: return null
    return try {
        TileActionStatus.valueOf(raw)
    } catch (e: Exception) {
        null
    }
}
