package com.apollox10.apollodeck.wearsync

import android.content.Context
import android.net.Uri
import android.util.Log
import com.apollox10.apollodeck.core.sync.TileGridSelection
import com.apollox10.apollodeck.core.sync.TileGridSyncPaths
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "TileGridPublisher"
private const val PREFS_NAME = "apollo_deck_wear_tile_grid"
private const val KEY_SELECTION = "selection"
private const val KEY_PAYLOAD = "payload"

// Which actions the watch's multi-action grid tile should show, chosen here
// on the phone (see ui/wearsync/TileGridConfigScreen.kt) and synced to the
// watch over the Wear Data Layer — the same DataItem-based approach as
// PhoneSessionPublisher, for the same reason: the watch just reads whatever
// the OS has already replicated (wear/.../tile/TileGridSync), it doesn't
// need the phone reachable at the exact moment it renders. This app's own
// SharedPreferences copy is the source of truth for what the config screen
// shows as "currently selected" — the published DataItem is a one-way
// mirror of it, not read back from here.
object TileGridPublisher {

    fun save(context: Context, selection: TileGridSelection) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SELECTION, Json.encodeToString(selection))
            .apply()
        publish(context, selection)
    }

    fun load(context: Context): TileGridSelection {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SELECTION, null) ?: return TileGridSelection(emptyList())
        return try {
            Json.decodeFromString(raw)
        } catch (e: Exception) {
            TileGridSelection(emptyList())
        }
    }

    private fun publish(context: Context, selection: TileGridSelection) {
        val request = PutDataMapRequest.create(TileGridSyncPaths.TILE_GRID_DATA_PATH).apply {
            dataMap.putString(KEY_PAYLOAD, Json.encodeToString(selection))
        }.asPutDataRequest().setUrgent()

        Wearable.getDataClient(context).putDataItem(request)
            .addOnSuccessListener { item -> Log.d(TAG, "published tile grid selection uri=${item.uri}") }
            .addOnFailureListener { e -> Log.w(TAG, "failed to publish tile grid selection", e) }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().remove(KEY_SELECTION).apply()
        val uri = Uri.Builder()
            .scheme(PutDataRequest.WEAR_URI_SCHEME)
            .path(TileGridSyncPaths.TILE_GRID_DATA_PATH)
            .build()
        Wearable.getDataClient(context).deleteDataItems(uri)
            .addOnSuccessListener { count -> Log.d(TAG, "cleared $count published tile grid item(s)") }
            .addOnFailureListener { e -> Log.w(TAG, "failed to clear published tile grid", e) }
    }
}
