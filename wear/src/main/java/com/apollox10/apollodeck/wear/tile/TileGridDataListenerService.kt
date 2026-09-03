package com.apollox10.apollodeck.wear.tile

import androidx.wear.tiles.TileService
import com.apollox10.apollodeck.core.sync.TileGridSelection
import com.apollox10.apollodeck.core.sync.TileGridSyncPaths
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

// Registered in the manifest against the DATA_CHANGED action, scoped to
// TileGridSyncPaths.TILE_GRID_DATA_PATH — the system can start this even if
// the app isn't running, so the watch reacts the moment the phone saves a
// new grid-tile selection (TileGridPublisher.save) rather than waiting for
// the grid tile's own decoupled background refresh to eventually notice
// (see TileGridSync/MultiActionTileService).
class TileGridDataListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        try {
            for (event in dataEvents) {
                if (event.type != DataEvent.TYPE_CHANGED) continue
                if (event.dataItem.uri.path != TileGridSyncPaths.TILE_GRID_DATA_PATH) continue

                val payload = DataMapItem.fromDataItem(event.dataItem).dataMap.getString(KEY_PAYLOAD) ?: continue
                val selection = try {
                    Json.decodeFromString<TileGridSelection>(payload)
                } catch (e: Exception) {
                    continue
                }

                cacheTileGridSelection(applicationContext, selection)
                TileService.getUpdater(applicationContext).requestUpdate(MultiActionTileService::class.java)
            }
        } finally {
            dataEvents.release()
        }
    }
}
