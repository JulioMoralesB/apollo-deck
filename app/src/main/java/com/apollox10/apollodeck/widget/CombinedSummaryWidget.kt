package com.apollox10.apollodeck.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle

// No per-instance config — always shows every service that currently has a
// summary_endpoint (see SummaryWidgetCache), so there's nothing to pick.
// Placing it just works; see SummaryWidget.kt for the per-service version.
class CombinedSummaryWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { pushCombinedSummaryWidgetRemoteViews(context, it) }
        ensureSummaryRefreshScheduled(context)
        refreshSummaryWidgetsNow(context)
    }

    // Fires on resize — row count and each row's compact/expanded format are
    // both driven by the widget's placed size (see
    // CombinedSummaryWidgetRemoteViews.planRows), so a resize needs its own
    // re-render rather than waiting for the next periodic refresh.
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        pushCombinedSummaryWidgetRemoteViews(context, appWidgetId)
    }

    override fun onEnabled(context: Context) {
        ensureSummaryRefreshScheduled(context)
    }

    override fun onDisabled(context: Context) {
        cancelSummaryRefreshIfUnused(context)
    }
}
