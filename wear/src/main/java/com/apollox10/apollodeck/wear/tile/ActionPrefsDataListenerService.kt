package com.apollox10.apollodeck.wear.tile

import androidx.wear.tiles.TileService
import com.apollox10.apollodeck.core.store.ActionPrefsStore
import com.apollox10.apollodeck.core.sync.ActionPrefsSelection
import com.apollox10.apollodeck.core.sync.ActionPrefsSyncPaths
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private const val ACTION_PREFS_KEY_PAYLOAD = "payload"

// Applies the phone's action order + confirm set as soon as they change —
// mirrors IconOverrideDataListenerService.
class ActionPrefsDataListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        try {
            for (event in dataEvents) {
                if (event.type != DataEvent.TYPE_CHANGED) continue
                if (event.dataItem.uri.path != ActionPrefsSyncPaths.ACTION_PREFS_DATA_PATH) continue

                val payload = DataMapItem.fromDataItem(event.dataItem).dataMap.getString(ACTION_PREFS_KEY_PAYLOAD) ?: continue
                val selection = try {
                    Json.decodeFromString<ActionPrefsSelection>(payload)
                } catch (e: Exception) {
                    continue
                }

                ActionPrefsStore(applicationContext).replaceAll(selection.order, selection.confirmRequired)
                TileService.getUpdater(applicationContext).requestUpdate(MultiActionTileService::class.java)
            }
        } finally {
            dataEvents.release()
        }
    }
}
