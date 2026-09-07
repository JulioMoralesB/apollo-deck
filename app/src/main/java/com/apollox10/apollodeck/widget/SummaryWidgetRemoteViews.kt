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

private val TitleText = Color(0xFFEEF0FA)
private val NormalText = Color(0xFFBFC4D4)
private val WarningText = Color(0xFFF5A623)
private val DangerText = Color(0xFFFF5C5C)

private val ROW_IDS = listOf(
    Triple(R.id.summary_widget_row1, R.id.summary_widget_row1_label, R.id.summary_widget_row1_value),
    Triple(R.id.summary_widget_row2, R.id.summary_widget_row2_label, R.id.summary_widget_row2_value),
    Triple(R.id.summary_widget_row3, R.id.summary_widget_row3_label, R.id.summary_widget_row3_value),
)
const val SUMMARY_WIDGET_MAX_LINES = 3

fun pushSummaryWidgetRemoteViews(context: Context, appWidgetId: Int) {
    AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, buildSummaryWidgetRemoteViews(context, appWidgetId))
}

private fun buildSummaryWidgetRemoteViews(context: Context, appWidgetId: Int): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_summary)
    val serviceName = loadSummaryWidgetConfig(context, appWidgetId)

    if (serviceName == null) {
        views.setTextViewText(R.id.summary_widget_title, "Tap to configure")
        views.setTextColor(R.id.summary_widget_title, TitleText.toArgb())
        ROW_IDS.forEach { (row, _, _) -> views.setViewVisibility(row, View.GONE) }
        views.setOnClickPendingIntent(R.id.summary_widget_root, openAppPendingIntent(context, appWidgetId, null))
        return views
    }

    views.setTextViewText(R.id.summary_widget_title, serviceName)
    views.setTextColor(R.id.summary_widget_title, TitleText.toArgb())

    val summary = loadParsedSummary(context, serviceName)
    val lines = summary?.let { condensedSummaryLines(it, SUMMARY_WIDGET_MAX_LINES) } ?: emptyList()

    ROW_IDS.forEachIndexed { i, (row, labelId, valueId) ->
        val line = lines.getOrNull(i)
        if (line == null) {
            views.setViewVisibility(row, View.GONE)
        } else {
            views.setViewVisibility(row, View.VISIBLE)
            views.setTextViewText(labelId, line.label)
            views.setTextViewText(valueId, line.value)
            views.setTextColor(valueId, colorFor(line.emphasis).toArgb())
        }
    }
    if (summary == null) {
        views.setViewVisibility(ROW_IDS[0].first, View.VISIBLE)
        views.setTextViewText(ROW_IDS[0].second, "")
        views.setTextViewText(ROW_IDS[0].third, "Loading…")
        views.setTextColor(ROW_IDS[0].third, NormalText.toArgb())
    }

    views.setOnClickPendingIntent(R.id.summary_widget_root, openAppPendingIntent(context, appWidgetId, serviceName))
    return views
}

private fun colorFor(emphasis: LineEmphasis) = when (emphasis) {
    LineEmphasis.Normal -> NormalText
    LineEmphasis.Warning -> WarningText
    LineEmphasis.Danger -> DangerText
}

private fun openAppPendingIntent(context: Context, appWidgetId: Int, serviceName: String?): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
        serviceName?.let { putExtra(EXTRA_OPEN_SERVICE_NAME, it) }
    }
    return PendingIntent.getActivity(
        context,
        appWidgetId,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
