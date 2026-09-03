package com.apollox10.apollodeck.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

private const val KEY_APP_WIDGET_ID = "appWidgetId"
private const val REVERT_DELAY_SECONDS = 3L

// Reverting the widget back to idle after a tap is scheduled as its own
// independent WorkManager job. Building and pushing the RemoteViews here
// directly (see WidgetRemoteViews.kt) rather than through Glance means this
// no longer depends on Glance's own session infrastructure — the earlier,
// Glance-based version of this worker called GlanceAppWidget.update(),
// which was observed to sometimes silently no-op on this device.
class RevertWidgetStatusWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val appWidgetId = inputData.getInt(KEY_APP_WIDGET_ID, -1)
        if (appWidgetId == -1) return Result.failure()
        setWidgetStatus(applicationContext, appWidgetId, null)
        pushWidgetRemoteViews(applicationContext, appWidgetId)
        return Result.success()
    }
}

fun scheduleWidgetStatusRevert(context: Context, appWidgetId: Int) {
    val request = OneTimeWorkRequestBuilder<RevertWidgetStatusWorker>()
        .setInitialDelay(REVERT_DELAY_SECONDS, TimeUnit.SECONDS)
        .setInputData(workDataOf(KEY_APP_WIDGET_ID to appWidgetId))
        .build()
    WorkManager.getInstance(context).enqueue(request)
}
