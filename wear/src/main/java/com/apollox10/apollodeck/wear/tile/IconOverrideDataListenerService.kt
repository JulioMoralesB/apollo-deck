package com.apollox10.apollodeck.wear.tile

import androidx.wear.tiles.TileService
import com.apollox10.apollodeck.core.store.IconOverrideStore
import com.apollox10.apollodeck.core.sync.IconOverrideSelection
import com.apollox10.apollodeck.core.sync.IconOverrideSyncPaths
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private const val ICON_OVERRIDE_KEY_PAYLOAD = "payload"

// Registered in the manifest against the DATA_CHANGED action, scoped to
// IconOverrideSyncPaths.ICON_OVERRIDE_DATA_PATH — mirrors
// TileGridDataListenerService. The system can start this even if the app
// isn't running, so both tiles pick up a phone-set icon override right
// away instead of waiting for GridTileRefreshWorker's next decoupled
// refresh to catch up on it.
class IconOverrideDataListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        try {
            for (event in dataEvents) {
                if (event.type != DataEvent.TYPE_CHANGED) continue
                if (event.dataItem.uri.path != IconOverrideSyncPaths.ICON_OVERRIDE_DATA_PATH) continue

                val payload = DataMapItem.fromDataItem(event.dataItem).dataMap.getString(ICON_OVERRIDE_KEY_PAYLOAD) ?: continue
                val overrides = try {
                    Json.decodeFromString<IconOverrideSelection>(payload).overrides
                } catch (e: Exception) {
                    continue
                }

                IconOverrideStore(applicationContext).replaceAll(overrides)
                TileService.getUpdater(applicationContext).requestUpdate(ActionTileService::class.java)
                TileService.getUpdater(applicationContext).requestUpdate(MultiActionTileService::class.java)
            }
        } finally {
            dataEvents.release()
        }
    }
}
