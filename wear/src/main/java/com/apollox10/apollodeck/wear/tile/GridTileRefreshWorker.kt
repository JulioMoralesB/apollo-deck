package com.apollox10.apollodeck.wear.tile

import android.content.Context
import androidx.wear.tiles.TileService
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.apollox10.apollodeck.core.net.ApiClient
import com.apollox10.apollodeck.wear.BuildConfig

private const val UNIQUE_WORK_NAME = "grid_tile_refresh"
private const val PREFS_NAME = "apollo_deck_tiles"
private const val KEY_LAST_REFRESH_AT = "grid_last_refresh_at"
// The grid tile can be re-requested very frequently (wrist raises, being
// the active carousel page) — without a floor here, every single render
// would enqueue another network-calling worker, which calls requestUpdate
// on completion, which triggers another render, forever. This debounces
// that into "refresh roughly once a minute at most" instead.
private const val MIN_REFRESH_INTERVAL_MS = 60_000L

// Refreshes the multi-action grid tile's local cache (see
// TileActionsCache/TileGridSync) from a live network fetch and a Data
// Layer catch-up pull, then requests a fresh render. Runs as WorkManager
// work rather than a coroutine scoped to MultiActionTileService itself —
// confirmed on real hardware that a TileService instance is routinely torn
// down (and its own CoroutineScope cancelled) very shortly after handling
// a single request, especially during the system's tile-picker preview
// scan, which killed a "fire and forget" coroutine launched there mid-
// fetch. Enqueued work survives that; the service that triggered it doesn't
// need to.
class GridTileRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        try {
            val client = ApiClient.create(applicationContext, debugLogging = BuildConfig.DEBUG)
            val flat = client.authenticatedApi.getServices()
                .flatMap { service ->
                    (service.actions.orEmpty())
                        .filter { it.method != null && it.method != "href" && it.endpoint != null }
                        .map { service.name to it }
                }
            cacheGridActions(applicationContext, flat)
        } catch (e: Exception) {
            // Keep whatever's already cached — see TileActionsCache.
        }

        // A catch-up pull, not the primary path — TileGridDataListenerService
        // pushes a new selection to the cache immediately when the phone
        // saves one. This only matters for a selection published before
        // this ever ran (or a missed push).
        pullTileGridSelection(applicationContext)?.let { cacheTileGridSelection(applicationContext, it) }

        TileService.getUpdater(applicationContext).requestUpdate(MultiActionTileService::class.java)
        return Result.success()
    }
}

fun enqueueGridTileRefresh(context: Context) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val now = System.currentTimeMillis()
    if (now - prefs.getLong(KEY_LAST_REFRESH_AT, 0L) < MIN_REFRESH_INTERVAL_MS) return
    prefs.edit().putLong(KEY_LAST_REFRESH_AT, now).apply()

    WorkManager.getInstance(context).enqueueUniqueWork(
        UNIQUE_WORK_NAME,
        ExistingWorkPolicy.KEEP,
        OneTimeWorkRequestBuilder<GridTileRefreshWorker>().build(),
    )
}
