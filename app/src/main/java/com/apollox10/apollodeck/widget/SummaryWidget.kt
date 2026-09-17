package com.apollox10.apollodeck.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle

// Plain AppWidgetProvider, same reasoning as ActionWidgetReceiver (see
// WidgetRemoteViews.kt) — no execute-on-tap here though, this widget is
// read-only, its data comes from SummaryRefreshWorker's periodic cache
// refresh, never a network call on the render path.
class SummaryWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { pushSummaryWidgetRemoteViews(context, it) }
        ensureSummaryRefreshScheduled(context)
        refreshSummaryWidgetsNow(context)
    }

    // Fires whenever the user resizes the widget on the home screen — the
    // item list is responsive to the widget's placed size (see
    // SummaryWidgetRemoteViews.availableItemRows), so a resize needs its own
    // re-render rather than waiting for the next periodic refresh.
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        pushSummaryWidgetRemoteViews(context, appWidgetId)
    }

    override fun onEnabled(context: Context) {
        ensureSummaryRefreshScheduled(context)
    }

    override fun onDisabled(context: Context) {
        cancelSummaryRefreshIfUnused(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { deleteSummaryWidgetConfig(context, it) }
    }
}
