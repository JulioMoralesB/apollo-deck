package com.apollox10.apollodeck.wearsync

import android.content.Context
import android.util.Log
import com.apollox10.apollodeck.core.store.IconOverrideStore
import com.apollox10.apollodeck.core.sync.IconOverrideSelection
import com.apollox10.apollodeck.core.sync.IconOverrideSyncPaths
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "IconOverridePublisher"
private const val KEY_PAYLOAD = "payload"

// Publishes the phone's current icon overrides as a synced Wear Data Layer
// DataItem — same approach as PhoneSessionPublisher/TileGridPublisher.
// Called after every change (IconOverrideViewModel), not just once, so the
// watch's copy never drifts from what the phone app is showing.
object IconOverridePublisher {
    fun publish(context: Context) {
        val overrides = IconOverrideStore(context).getAll()
        val request = PutDataMapRequest.create(IconOverrideSyncPaths.ICON_OVERRIDE_DATA_PATH).apply {
            dataMap.putString(KEY_PAYLOAD, Json.encodeToString(IconOverrideSelection(overrides)))
        }.asPutDataRequest().setUrgent()

        Wearable.getDataClient(context).putDataItem(request)
            .addOnSuccessListener { item -> Log.d(TAG, "published icon overrides uri=${item.uri}") }
            .addOnFailureListener { e -> Log.w(TAG, "failed to publish icon overrides", e) }
    }
}
