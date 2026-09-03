package com.apollox10.apollodeck.wear.tile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.wear.tiles.TileService
import com.apollox10.apollodeck.core.net.ApiClient
import com.apollox10.apollodeck.wear.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Executes a tile action in the background and reports success/error back
// to whichever tile triggered it — ActionTileService (keyed by its
// tileId, brief result then RevertTileStatusWorker clears it) or
// MultiActionTileService (keyed by grid index, brief result then
// TileGridActionStatus's own age-based backstop clears it) — the same
// execute-then-revert shape as the phone widget's ActionWidgetReceiver,
// adapted for Tiles: a Tile's Clickable can only launch an Activity or
// fire a LoadAction (re-request the tile), it can't hold a PendingIntent
// to an arbitrary broadcast the way RemoteViews can, so the triggering
// tile issues this broadcast itself from inside onTileRequest when it
// sees the "execute" click via requestParams.currentState.lastClickableId.
class TileActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val endpoint = intent.getStringExtra(EXTRA_ENDPOINT) ?: return
        val method = intent.getStringExtra(EXTRA_METHOD) ?: return
        val trackStatusTileId = intent.getIntExtra(EXTRA_TRACK_STATUS_TILE_ID, NO_TILE_ID)
        val trackGridIndex = intent.getIntExtra(EXTRA_TRACK_GRID_INDEX, NO_GRID_INDEX)
        // Not context itself — a BroadcastReceiver's context actively
        // refuses bindService() (ReceiverCallNotAllowedException), which is
        // exactly what TileService.getUpdater(...).requestUpdate() does
        // internally. Confirmed on real hardware: this crashed the whole
        // process on every single execute, success or failure, right after
        // the status was written but before the revert could be scheduled
        // — which is what actually left the tile stuck on its last result
        // (not, as first suspected, WorkManager deferring the revert job).
        val appContext = context.applicationContext

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val client = ApiClient.create(appContext, debugLogging = BuildConfig.DEBUG)
                val result = client.actionExecutor.execute(endpoint, method)
                val status = if (result.success) TileActionStatus.Success else TileActionStatus.Error
                if (trackStatusTileId != NO_TILE_ID) reportStatus(appContext, trackStatusTileId, status)
                if (trackGridIndex != NO_GRID_INDEX) reportGridStatus(appContext, trackGridIndex, status)
            } catch (e: Exception) {
                if (trackStatusTileId != NO_TILE_ID) reportStatus(appContext, trackStatusTileId, TileActionStatus.Error)
                if (trackGridIndex != NO_GRID_INDEX) reportGridStatus(appContext, trackGridIndex, TileActionStatus.Error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun reportStatus(context: Context, tileId: Int, status: TileActionStatus) {
        setTileActionStatus(context, tileId, status)
        TileService.getUpdater(context).requestUpdate(ActionTileService::class.java)
        scheduleTileStatusRevert(context, tileId)
    }

    private fun reportGridStatus(context: Context, index: Int, status: TileActionStatus) {
        setGridActionStatus(context, index, status)
        TileService.getUpdater(context).requestUpdate(MultiActionTileService::class.java)
    }

    companion object {
        const val ACTION_EXECUTE = "com.apollox10.apollodeck.wear.tile.ACTION_EXECUTE"
        const val EXTRA_ENDPOINT = "endpoint"
        const val EXTRA_METHOD = "method"
        // The tileId to report success/error status back to (ActionTileService),
        // or NO_TILE_ID if this execution isn't from that tile.
        const val EXTRA_TRACK_STATUS_TILE_ID = "track_status_tile_id"
        const val NO_TILE_ID = -1
        // The grid index to report success/error status back to
        // (MultiActionTileService), or NO_GRID_INDEX if this execution
        // isn't from that tile.
        const val EXTRA_TRACK_GRID_INDEX = "track_grid_index"
        const val NO_GRID_INDEX = -1
    }
}
