package com.apollox10.apollodeck.wear.tile

import android.content.Context
import android.util.Log
import com.apollox10.apollodeck.core.sync.IconOverrideSelection
import com.apollox10.apollodeck.core.sync.IconOverrideSyncPaths
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private const val TAG = "IconOverrideSync"
private const val PULL_TIMEOUT_MS = 5_000L
private const val ICON_OVERRIDE_KEY_PAYLOAD = "payload"

// A bootstrap/catch-up pull against whatever the OS has already replicated
// — covers overrides set before this was ever installed, or a push
// IconOverrideDataListenerService missed. Never called from a Tile's
// render path (see MultiActionTileService's render-path rule) — only from
// GridTileRefreshWorker's decoupled background refresh.
suspend fun pullIconOverrides(context: Context): Map<String, String>? {
    return try {
        withTimeoutOrNull(PULL_TIMEOUT_MS) {
            val buffer = Wearable.getDataClient(context).dataItems.await()
            try {
                val item = (0 until buffer.count)
                    .map { buffer[it] }
                    .firstOrNull { it.uri.path == IconOverrideSyncPaths.ICON_OVERRIDE_DATA_PATH }
                    ?: return@withTimeoutOrNull null
                val payload = DataMapItem.fromDataItem(item).dataMap.getString(ICON_OVERRIDE_KEY_PAYLOAD) ?: return@withTimeoutOrNull null
                Json.decodeFromString<IconOverrideSelection>(payload).overrides
            } finally {
                buffer.release()
            }
        }
    } catch (e: Exception) {
        Log.w(TAG, "failed to pull synced icon overrides", e)
        null
    }
}
