package com.apollox10.apollodeck.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.apollox10.apollodeck.EXTRA_OPEN_SERVICE_NAME
import com.apollox10.apollodeck.MainActivity
import com.apollox10.apollodeck.R

private val NormalText = Color(0xFFEEF0FA)
private val WarningText = Color(0xFFF5A623)
private val DangerText = Color(0xFFFF5C5C)

private val ROW_IDS = listOf(
    Triple(R.id.combined_summary_widget_row1, R.id.combined_summary_widget_row1_label, R.id.combined_summary_widget_row1_value),
    Triple(R.id.combined_summary_widget_row2, R.id.combined_summary_widget_row2_label, R.id.combined_summary_widget_row2_value),
    Triple(R.id.combined_summary_widget_row3, R.id.combined_summary_widget_row3_label, R.id.combined_summary_widget_row3_value),
    Triple(R.id.combined_summary_widget_row4, R.id.combined_summary_widget_row4_label, R.id.combined_summary_widget_row4_value),
)
const val COMBINED_SUMMARY_WIDGET_MAX_ROWS = 4

fun pushCombinedSummaryWidgetRemoteViews(context: Context, appWidgetId: Int) {
    AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, buildCombinedSummaryWidgetRemoteViews(context, appWidgetId))
}

private fun buildCombinedSummaryWidgetRemoteViews(context: Context, appWidgetId: Int): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_combined_summary)
    val serviceNames = loadKnownSummaryServiceNames(context).take(COMBINED_SUMMARY_WIDGET_MAX_ROWS)

    if (serviceNames.isEmpty()) {
        ROW_IDS.forEach { (row, _, _) -> views.setViewVisibility(row, View.GONE) }
        views.setViewVisibility(R.id.combined_summary_widget_empty, View.VISIBLE)
        views.setOnClickPendingIntent(R.id.combined_summary_widget_root, openAppPendingIntent(context, appWidgetId, 0, null))
        return views
    }

    views.setViewVisibility(R.id.combined_summary_widget_empty, View.GONE)

    ROW_IDS.forEachIndexed { i, (row, labelId, valueId) ->
        val serviceName = serviceNames.getOrNull(i)
        if (serviceName == null) {
            views.setViewVisibility(row, View.GONE)
            return@forEachIndexed
        }

        views.setViewVisibility(row, View.VISIBLE)
        val summary = loadParsedSummary(context, serviceName)
        val line = summary?.let { condensedSummaryLines(it, maxLines = 1).firstOrNull() }

        views.setTextViewText(labelId, line?.label ?: serviceName)
        views.setTextViewText(valueId, line?.value ?: "Loading…")
        views.setTextColor(valueId, (line?.let { colorFor(it.emphasis) } ?: NormalText).toArgb())
        views.setOnClickPendingIntent(row, openAppPendingIntent(context, appWidgetId, i + 1, serviceName))
    }

    return views
}

private fun colorFor(emphasis: LineEmphasis) = when (emphasis) {
    LineEmphasis.Normal -> NormalText
    LineEmphasis.Warning -> WarningText
    LineEmphasis.Danger -> DangerText
}

// requestCode must be unique per row, not just per widget instance — two
// PendingIntents with the same requestCode are treated as the same one
// (Intent.filterEquals ignores extras), so every row would resolve to
// whichever service's Intent was built first without this. rowSlot (0 for
// the empty-state fallback, 1..MAX_ROWS for real rows) keeps it small and
// collision-free instead of hashing the service name.
private fun openAppPendingIntent(context: Context, appWidgetId: Int, rowSlot: Int, serviceName: String?): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
        serviceName?.let { putExtra(EXTRA_OPEN_SERVICE_NAME, it) }
    }
    val requestCode = appWidgetId * (COMBINED_SUMMARY_WIDGET_MAX_ROWS + 1) + rowSlot
    return PendingIntent.getActivity(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
