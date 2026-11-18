package com.apollox10.apollodeck.wear.tile

import androidx.wear.tiles.TileService
import com.apollox10.apollodeck.core.store.ConfirmActionStore
import com.apollox10.apollodeck.core.sync.ConfirmActionSelection
import com.apollox10.apollodeck.core.sync.ConfirmActionSyncPaths
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private const val CONFIRM_ACTION_KEY_PAYLOAD = "payload"

// Applies the phone's confirm-before-running set as soon as it changes —
// mirrors IconOverrideDataListenerService.
class ConfirmActionDataListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        try {
            for (event in dataEvents) {
                if (event.type != DataEvent.TYPE_CHANGED) continue
                if (event.dataItem.uri.path != ConfirmActionSyncPaths.CONFIRM_ACTION_DATA_PATH) continue

                val payload = DataMapItem.fromDataItem(event.dataItem).dataMap.getString(CONFIRM_ACTION_KEY_PAYLOAD) ?: continue
                val entityIds = try {
                    Json.decodeFromString<ConfirmActionSelection>(payload).entityIds
                } catch (e: Exception) {
                    continue
                }

                ConfirmActionStore(applicationContext).replaceAll(entityIds)
                TileService.getUpdater(applicationContext).requestUpdate(MultiActionTileService::class.java)
            }
        } finally {
            dataEvents.release()
        }
    }
}
