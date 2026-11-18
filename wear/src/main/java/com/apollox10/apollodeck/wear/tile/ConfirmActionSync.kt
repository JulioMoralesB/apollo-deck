package com.apollox10.apollodeck.wear.tile

import android.content.Context
import android.util.Log
import com.apollox10.apollodeck.core.sync.ConfirmActionSelection
import com.apollox10.apollodeck.core.sync.ConfirmActionSyncPaths
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private const val TAG = "ConfirmActionSync"
private const val PULL_TIMEOUT_MS = 5_000L
private const val CONFIRM_ACTION_KEY_PAYLOAD = "payload"

// Catch-up pull of the phone's confirm-before-running set — same role as
// pullIconOverrides: covers a change published before this watch app was
// installed, or a push ConfirmActionDataListenerService missed. Only called
// from GridTileRefreshWorker, never a Tile's render path.
suspend fun pullConfirmActions(context: Context): Set<String>? {
    return try {
        withTimeoutOrNull(PULL_TIMEOUT_MS) {
            val buffer = Wearable.getDataClient(context).dataItems.await()
            try {
                val item = (0 until buffer.count)
                    .map { buffer[it] }
                    .firstOrNull { it.uri.path == ConfirmActionSyncPaths.CONFIRM_ACTION_DATA_PATH }
                    ?: return@withTimeoutOrNull null
                val payload = DataMapItem.fromDataItem(item).dataMap.getString(CONFIRM_ACTION_KEY_PAYLOAD) ?: return@withTimeoutOrNull null
                Json.decodeFromString<ConfirmActionSelection>(payload).entityIds
            } finally {
                buffer.release()
            }
        }
    } catch (e: Exception) {
        Log.w(TAG, "failed to pull synced confirm actions", e)
        null
    }
}
