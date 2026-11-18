package com.apollox10.apollodeck.wear.tile

import android.content.Context
import android.util.Log
import com.apollox10.apollodeck.core.sync.ActionPrefsSelection
import com.apollox10.apollodeck.core.sync.ActionPrefsSyncPaths
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private const val TAG = "ActionPrefsSync"
private const val PULL_TIMEOUT_MS = 5_000L
private const val ACTION_PREFS_KEY_PAYLOAD = "payload"

// Catch-up pull of the phone's action order + confirm set — same role as
// pullIconOverrides: covers a change published before this watch app was
// installed, or a push ActionPrefsDataListenerService missed. Only called
// from GridTileRefreshWorker, never a Tile's render path.
suspend fun pullActionPrefs(context: Context): ActionPrefsSelection? {
    return try {
        withTimeoutOrNull(PULL_TIMEOUT_MS) {
            val buffer = Wearable.getDataClient(context).dataItems.await()
            try {
                val item = (0 until buffer.count)
                    .map { buffer[it] }
                    .firstOrNull { it.uri.path == ActionPrefsSyncPaths.ACTION_PREFS_DATA_PATH }
                    ?: return@withTimeoutOrNull null
                val payload = DataMapItem.fromDataItem(item).dataMap.getString(ACTION_PREFS_KEY_PAYLOAD) ?: return@withTimeoutOrNull null
                Json.decodeFromString<ActionPrefsSelection>(payload)
            } finally {
                buffer.release()
            }
        }
    } catch (e: Exception) {
        Log.w(TAG, "failed to pull synced action prefs", e)
        null
    }
}
