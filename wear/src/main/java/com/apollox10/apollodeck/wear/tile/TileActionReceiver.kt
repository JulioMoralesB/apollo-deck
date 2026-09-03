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

// Executes a tile action in the background and, for the single-action tile,
// tracks success/error (keyed by the triggering tile instance's tileId) so
// ActionTileService can render a brief result before RevertTileStatusWorker
// clears it back to idle — the same execute-then-revert shape as the phone
// widget's ActionWidgetReceiver, adapted for Tiles: a Tile's Clickable can
// only launch an Activity or fire a LoadAction (re-request the tile), it
// can't hold a PendingIntent to an arbitrary broadcast the way RemoteViews
// can, so ActionTileService issues this broadcast itself from inside
// onTileRequest when it sees the "execute" click via
// requestParams.currentState.lastClickableId.
class TileActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val endpoint = intent.getStringExtra(EXTRA_ENDPOINT) ?: return
        val method = intent.getStringExtra(EXTRA_METHOD) ?: return
        val trackStatusTileId = intent.getIntExtra(EXTRA_TRACK_STATUS_TILE_ID, NO_TILE_ID)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val client = ApiClient.create(context, debugLogging = BuildConfig.DEBUG)
                val result = client.actionExecutor.execute(endpoint, method)
                if (trackStatusTileId != NO_TILE_ID) {
                    val status = if (result.success) TileActionStatus.Success else TileActionStatus.Error
                    reportStatus(context, trackStatusTileId, status)
                }
            } catch (e: Exception) {
                if (trackStatusTileId != NO_TILE_ID) {
                    reportStatus(context, trackStatusTileId, TileActionStatus.Error)
                }
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

    companion object {
        const val ACTION_EXECUTE = "com.apollox10.apollodeck.wear.tile.ACTION_EXECUTE"
        const val EXTRA_ENDPOINT = "endpoint"
        const val EXTRA_METHOD = "method"
        // The tileId to report success/error status back to, or NO_TILE_ID
        // for a fire-and-forget execution (the multi-action grid tile,
        // which has no single result slot to show a status in).
        const val EXTRA_TRACK_STATUS_TILE_ID = "track_status_tile_id"
        const val NO_TILE_ID = -1
    }
}
