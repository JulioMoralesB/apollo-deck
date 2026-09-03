package com.apollox10.apollodeck.wear.tile

import android.content.Context
import androidx.wear.tiles.TileService
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

private const val REVERT_DELAY_SECONDS = 3L
private const val KEY_TILE_ID = "tile_id"

// Mirrors the phone widget's RevertWidgetStatusWorker: reverting one tile
// instance back to idle after showing a tap result runs as its own
// independent WorkManager job, decoupled from the broadcast that triggered
// the action, rather than a delay() held inside it. Which instance to
// revert is passed through inputData since a tile type can now be pinned
// more than once (see TileActionConfig).
class RevertTileStatusWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val tileId = inputData.getInt(KEY_TILE_ID, -1)
        if (tileId == -1) return Result.failure()
        setTileActionStatus(applicationContext, tileId, null)
        TileService.getUpdater(applicationContext).requestUpdate(ActionTileService::class.java)
        return Result.success()
    }
}

fun scheduleTileStatusRevert(context: Context, tileId: Int) {
    val request = OneTimeWorkRequestBuilder<RevertTileStatusWorker>()
        .setInputData(Data.Builder().putInt(KEY_TILE_ID, tileId).build())
        .setInitialDelay(REVERT_DELAY_SECONDS, TimeUnit.SECONDS)
        .build()
    WorkManager.getInstance(context).enqueue(request)
}
