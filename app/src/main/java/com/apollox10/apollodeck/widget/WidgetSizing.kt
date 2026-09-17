package com.apollox10.apollodeck.widget

import android.appwidget.AppWidgetManager
import android.content.Context

// The widget's current placed (width, height) in dp, from whatever size the
// launcher last reported (via onAppWidgetOptionsChanged, or the initial
// options at first placement) — there's no live "measure" callback for a
// RemoteViews widget, this is what's available. Returns (0, 0) before the
// launcher has reported anything yet; callers should treat that as
// "unknown, use a conservative default" rather than 0 rows of content.
fun widgetSizeDp(context: Context, appWidgetId: Int): Pair<Int, Int> {
    val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
    val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
    val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
    return width to height
}
