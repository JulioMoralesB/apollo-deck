package com.apollox10.apollodeck.wear.tile

import android.content.Context
import android.util.Log
import com.apollox10.apollodeck.core.sync.TileGridSelection
import com.apollox10.apollodeck.core.sync.TileGridSyncPaths
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "TileGridSync"
private const val PULL_TIMEOUT_MS = 5_000L
private const val PREFS_NAME = "apollo_deck_tiles"
private const val KEY_CACHED_SELECTION = "grid_selection_cache"
internal const val KEY_PAYLOAD = "payload"

// Local, synchronous read of whatever selection was last synced — safe to
// call from a Tile's render path (no network, no Data Layer call).
// MultiActionTileService renders from this plus TileActionsCache alone;
// all the actual Data Layer/network work happens in a decoupled background
// refresh instead (see resolveDisplayedActionsFast/refreshInBackground
// there). Real hardware testing found the render path itself getting
// cancelled by the system before a live DataClient read could ever
// complete — a Tile is expected to respond in roughly a second or two, well
// under what a cold app process plus a network+Data Layer round trip needs.
//
// Kept fresh two ways: pushed to immediately by
// TileGridDataListenerService the moment the phone saves a new selection,
// and refreshed by pullTileGridSelection below as a periodic/bootstrap
// catch-up (covers a selection published before this was ever installed,
// or a missed push).
fun loadCachedTileGridSelection(context: Context): TileGridSelection? {
    val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_CACHED_SELECTION, null) ?: return null
    return try {
        Json.decodeFromString(raw)
    } catch (e: Exception) {
        null
    }
}

fun cacheTileGridSelection(context: Context, selection: TileGridSelection) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_CACHED_SELECTION, Json.encodeToString(selection))
        .apply()
}

// A bootstrap/catch-up pull against whatever the OS has already replicated.
// Never called from a Tile's render path — only from a decoupled background
// refresh — so a slow or failed pull here can't hold up a render the way it
// did before.
suspend fun pullTileGridSelection(context: Context): TileGridSelection? {
    return try {
        withTimeoutOrNull(PULL_TIMEOUT_MS) {
            val buffer = Wearable.getDataClient(context).dataItems.await()
            try {
                val item = (0 until buffer.count)
                    .map { buffer[it] }
                    .firstOrNull { it.uri.path == TileGridSyncPaths.TILE_GRID_DATA_PATH }
                    ?: return@withTimeoutOrNull null
                val payload = DataMapItem.fromDataItem(item).dataMap.getString(KEY_PAYLOAD) ?: return@withTimeoutOrNull null
                Json.decodeFromString<TileGridSelection>(payload)
            } finally {
                buffer.release()
            }
        }
    } catch (e: Exception) {
        Log.w(TAG, "failed to pull synced grid selection", e)
        null
    }
}
