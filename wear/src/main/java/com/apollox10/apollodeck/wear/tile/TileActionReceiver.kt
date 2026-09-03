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
// tracks success/error so ActionTileService can render a brief result
// before RevertTileStatusWorker clears it back to idle — the same
// execute-then-revert shape as the phone widget's ActionWidgetReceiver,
// adapted for Tiles: a Tile's Clickable can only launch an Activity or fire
// a LoadAction (re-request the tile), it can't hold a PendingIntent to an
// arbitrary broadcast the way RemoteViews can, so ActionTileService issues
// this broadcast itself from inside onTileRequest when it sees the
// "execute" click via requestParams.currentState.lastClickableId.
class TileActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val endpoint = intent.getStringExtra(EXTRA_ENDPOINT) ?: return
        val method = intent.getStringExtra(EXTRA_METHOD) ?: return
        val trackStatus = intent.getBooleanExtra(EXTRA_TRACK_STATUS, false)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val client = ApiClient.create(context, debugLogging = BuildConfig.DEBUG)
                val result = client.actionExecutor.execute(endpoint, method)
                if (trackStatus) {
                    val status = if (result.success) TileActionStatus.Success else TileActionStatus.Error
                    setTileActionStatus(context, status)
                    TileService.getUpdater(context).requestUpdate(ActionTileService::class.java)
                    scheduleTileStatusRevert(context)
                }
            } catch (e: Exception) {
                if (trackStatus) {
                    setTileActionStatus(context, TileActionStatus.Error)
                    TileService.getUpdater(context).requestUpdate(ActionTileService::class.java)
                    scheduleTileStatusRevert(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_EXECUTE = "com.apollox10.apollodeck.wear.tile.ACTION_EXECUTE"
        const val EXTRA_ENDPOINT = "endpoint"
        const val EXTRA_METHOD = "method"
        const val EXTRA_TRACK_STATUS = "track_status"
    }
}
