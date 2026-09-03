package com.apollox10.apollodeck.wear.tile

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// What the single-action tile is pinned to. Unlike the phone's home-screen
// widget (one config per appWidgetId), a Wear tile of a given type is
// realistically added once, so this is one global config, not keyed per
// tile instance — simpler, and there's no per-instance configure API for
// Tiles to hang a per-instance key off anyway (see TileConfigActivity).
@Serializable
data class TileActionConfig(
    val serviceName: String,
    val label: String,
    val endpoint: String,
    val method: String,
    val confirm: Boolean,
)

enum class TileActionStatus { Success, Error }

private const val PREFS_NAME = "apollo_deck_tiles"
private const val KEY_CONFIG = "single_action_config"
private const val KEY_STATUS = "single_action_status"

fun saveTileActionConfig(context: Context, config: TileActionConfig) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_CONFIG, Json.encodeToString(config))
        .apply()
}

fun loadTileActionConfig(context: Context): TileActionConfig? {
    val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_CONFIG, null) ?: return null
    return try {
        Json.decodeFromString(raw)
    } catch (e: Exception) {
        null
    }
}

fun setTileActionStatus(context: Context, status: TileActionStatus?) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .apply {
            if (status == null) remove(KEY_STATUS) else putString(KEY_STATUS, status.name)
        }
        .apply()
}

fun loadTileActionStatus(context: Context): TileActionStatus? {
    val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_STATUS, null) ?: return null
    return try {
        TileActionStatus.valueOf(raw)
    } catch (e: Exception) {
        null
    }
}
