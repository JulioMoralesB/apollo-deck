package com.apollox10.apollodeck.wearsync

import android.content.Context
import android.util.Log
import com.apollox10.apollodeck.core.store.ConfirmActionStore
import com.apollox10.apollodeck.core.sync.ConfirmActionSelection
import com.apollox10.apollodeck.core.sync.ConfirmActionSyncPaths
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "ConfirmActionPublisher"
private const val KEY_PAYLOAD = "payload"

// Publishes the confirm-before-running set as a synced Wear Data Layer
// DataItem — same approach as IconOverridePublisher. Called on every change
// and on app open, so the watch's copy never drifts from the phone's.
object ConfirmActionPublisher {
    fun publish(context: Context) {
        val selection = ConfirmActionSelection(ConfirmActionStore(context).getAll())
        val request = PutDataMapRequest.create(ConfirmActionSyncPaths.CONFIRM_ACTION_DATA_PATH).apply {
            dataMap.putString(KEY_PAYLOAD, Json.encodeToString(selection))
        }.asPutDataRequest().setUrgent()

        Wearable.getDataClient(context).putDataItem(request)
            .addOnSuccessListener { item -> Log.d(TAG, "published confirm actions uri=${item.uri}") }
            .addOnFailureListener { e -> Log.w(TAG, "failed to publish confirm actions", e) }
    }
}
