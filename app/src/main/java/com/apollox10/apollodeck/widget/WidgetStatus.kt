package com.apollox10.apollodeck.widget

import android.content.Context

// Transient per-instance state, separate from WidgetActionConfig: the result
// of the most recent tap, shown briefly on the widget itself (a colored
// background + check/close icon) before the revert work (see
// RevertWidgetStatusWorker) clears it back to idle. A Toast can be missed or
// suppressed by the launcher — this is the feedback that's actually pushed
// to the widget's own RemoteViews, guaranteed visible for as long as it's set.
enum class WidgetStatus {
    Success,
    Error,
}

internal fun statusKey(appWidgetId: Int) = "status_$appWidgetId"

fun setWidgetStatus(context: Context, appWidgetId: Int, status: WidgetStatus?) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    if (status == null) {
        prefs.edit().remove(statusKey(appWidgetId)).apply()
    } else {
        prefs.edit().putString(statusKey(appWidgetId), status.name).apply()
    }
}

fun loadWidgetStatus(context: Context, appWidgetId: Int): WidgetStatus? {
    val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(statusKey(appWidgetId), null) ?: return null
    return try {
        WidgetStatus.valueOf(raw)
    } catch (e: IllegalArgumentException) {
        null
    }
}
