package com.apollox10.apollodeck.wear.tile

import android.content.Context
import com.apollox10.apollodeck.core.model.Action
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val PREFS_NAME = "apollo_deck_tiles"
private const val KEY_CACHED_ACTIONS = "grid_actions_cache"

@Serializable
private data class CachedAction(val serviceName: String, val action: Action)

// Caches the last successfully fetched flat action list for the
// multi-action grid tile — the live `GET /services` call inside
// MultiActionTileService's render path is what was getting cancelled by
// the system on real hardware (a Tile is expected to respond in roughly a
// second or two; the network call has no such guarantee), leaving the tile
// with nothing to show. Falling back to whatever was fetched last keeps
// the grid populated even when a given render's live fetch doesn't make it
// in time.
fun cacheGridActions(context: Context, actions: List<Pair<String, Action>>) {
    val cached = actions.map { (serviceName, action) -> CachedAction(serviceName, action) }
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_CACHED_ACTIONS, Json.encodeToString(cached))
        .apply()
}

fun loadCachedGridActions(context: Context): List<Pair<String, Action>> {
    val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_CACHED_ACTIONS, null) ?: return emptyList()
    return try {
        Json.decodeFromString<List<CachedAction>>(raw).map { it.serviceName to it.action }
    } catch (e: Exception) {
        emptyList()
    }
}
