package com.apollox10.apollodeck.wearsync

import android.content.Context
import android.util.Log
import com.apollox10.apollodeck.core.store.ActionPrefsStore
import com.apollox10.apollodeck.core.sync.ActionPrefsSelection
import com.apollox10.apollodeck.core.sync.ActionPrefsSyncPaths
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "ActionPrefsPublisher"
private const val KEY_PAYLOAD = "payload"

// Publishes the action order + confirm set as a synced Wear Data Layer
// DataItem — same approach as IconOverridePublisher. Called on every change
// and on app open, so the watch's copy never drifts from the phone's.
object ActionPrefsPublisher {
    fun publish(context: Context) {
        val store = ActionPrefsStore(context)
        val selection = ActionPrefsSelection(order = store.getOrder(), confirmRequired = store.getConfirmRequired())
        val request = PutDataMapRequest.create(ActionPrefsSyncPaths.ACTION_PREFS_DATA_PATH).apply {
            dataMap.putString(KEY_PAYLOAD, Json.encodeToString(selection))
        }.asPutDataRequest().setUrgent()

        Wearable.getDataClient(context).putDataItem(request)
            .addOnSuccessListener { item -> Log.d(TAG, "published action prefs uri=${item.uri}") }
            .addOnFailureListener { e -> Log.w(TAG, "failed to publish action prefs", e) }
    }
}
