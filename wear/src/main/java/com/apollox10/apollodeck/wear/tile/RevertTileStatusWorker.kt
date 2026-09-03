package com.apollox10.apollodeck.wear.tile

import android.content.Context
import androidx.wear.tiles.TileService
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

private const val REVERT_DELAY_SECONDS = 3L

// Mirrors the phone widget's RevertWidgetStatusWorker: reverting the tile
// back to idle after showing a tap result runs as its own independent
// WorkManager job, decoupled from the broadcast that triggered the action,
// rather than a delay() held inside it.
class RevertTileStatusWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        setTileActionStatus(applicationContext, null)
        TileService.getUpdater(applicationContext).requestUpdate(ActionTileService::class.java)
        return Result.success()
    }
}

fun scheduleTileStatusRevert(context: Context) {
    val request = OneTimeWorkRequestBuilder<RevertTileStatusWorker>()
        .setInitialDelay(REVERT_DELAY_SECONDS, TimeUnit.SECONDS)
        .build()
    WorkManager.getInstance(context).enqueue(request)
}
