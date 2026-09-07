package com.apollox10.apollodeck.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.apollox10.apollodeck.BuildConfig
import com.apollox10.apollodeck.core.net.ApiClient
import java.util.concurrent.TimeUnit

private const val PERIODIC_WORK_NAME = "summary_widget_refresh_periodic"
private const val ONE_TIME_WORK_NAME = "summary_widget_refresh_once"
private const val REFRESH_INTERVAL_MINUTES = 15L

// Refreshes every placed summary widget's (per-service and combined) local
// cache from a live fetch, then pushes fresh RemoteViews — same "render
// from local state only, background job does the network work" discipline
// as the Wear tiles' GridTileRefreshWorker (see apollo-deck CLAUDE.md).
// Runs as WorkManager work rather than anything scoped to the
// AppWidgetProvider instances themselves, since those are routinely torn
// down between calls.
class SummaryRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        try {
            val client = ApiClient.create(applicationContext, debugLogging = BuildConfig.DEBUG)
            val services = client.authenticatedApi.getServices()
            val summaryServices = services.filter { it.summaryEndpoint != null }

            summaryServices.forEach { service ->
                val endpoint = service.summaryEndpoint ?: return@forEach
                client.actionExecutor.fetchSummary(endpoint).onSuccess { raw ->
                    saveSummaryCache(applicationContext, service.name, raw)
                }
                // On failure, keep whatever's already cached rather than
                // clearing it — a stale summary is more useful than a blank
                // widget for one missed refresh cycle.
            }
            saveKnownSummaryServiceNames(applicationContext, summaryServices.map { it.name })
        } catch (e: Exception) {
            // Network/auth failure for the whole /services call — leave
            // every cache entry as it was, same reasoning as above.
            return Result.success()
        }

        pushAllSummaryWidgets(applicationContext)
        return Result.success()
    }
}

// Pushes every placed instance of both summary widget types from cache —
// called after a refresh, and also right after a widget is (re)placed so it
// doesn't sit blank until the next periodic tick.
fun pushAllSummaryWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    manager.getAppWidgetIds(ComponentName(context, SummaryWidgetReceiver::class.java))
        .forEach { pushSummaryWidgetRemoteViews(context, it) }
    manager.getAppWidgetIds(ComponentName(context, CombinedSummaryWidgetReceiver::class.java))
        .forEach { pushCombinedSummaryWidgetRemoteViews(context, it) }
}

fun anySummaryWidgetsPlaced(context: Context): Boolean {
    val manager = AppWidgetManager.getInstance(context)
    return manager.getAppWidgetIds(ComponentName(context, SummaryWidgetReceiver::class.java)).isNotEmpty() ||
        manager.getAppWidgetIds(ComponentName(context, CombinedSummaryWidgetReceiver::class.java)).isNotEmpty()
}

fun ensureSummaryRefreshScheduled(context: Context) {
    val request = PeriodicWorkRequestBuilder<SummaryRefreshWorker>(REFRESH_INTERVAL_MINUTES, TimeUnit.MINUTES).build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        PERIODIC_WORK_NAME,
        ExistingPeriodicWorkPolicy.KEEP,
        request,
    )
}

fun cancelSummaryRefreshIfUnused(context: Context) {
    if (!anySummaryWidgetsPlaced(context)) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
    }
}

// Triggered right after a widget is placed/reconfigured so it fills in
// immediately rather than waiting for the next periodic tick (up to 15
// minutes away).
fun refreshSummaryWidgetsNow(context: Context) {
    WorkManager.getInstance(context).enqueueUniqueWork(
        ONE_TIME_WORK_NAME,
        ExistingWorkPolicy.KEEP,
        OneTimeWorkRequestBuilder<SummaryRefreshWorker>().build(),
    )
}
