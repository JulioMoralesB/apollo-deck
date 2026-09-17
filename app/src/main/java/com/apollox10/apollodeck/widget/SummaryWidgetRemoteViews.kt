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

private val ITEM_ROW_IDS = listOf(
    R.id.summary_widget_item1, R.id.summary_widget_item2, R.id.summary_widget_item3,
    R.id.summary_widget_item4, R.id.summary_widget_item5, R.id.summary_widget_item6,
    R.id.summary_widget_item7, R.id.summary_widget_item8,
)
const val SUMMARY_WIDGET_MAX_ITEM_ROWS = 8

// Fixed chrome above the item rows — 12dp top padding, the title line
// (~12sp + 6dp margin), the header line (~9sp + 4dp margin) — see
// widget_summary.xml. Each item row is ~13sp text + 3dp margin. Both are
// estimates (there's no callback that reports actual rendered text height
// from a RemoteViews TextView), deliberately conservative so a resize never
// undershoots into a clipped last row.
private const val FIXED_CHROME_DP = 62
private const val ITEM_ROW_DP = 20
private const val MIN_ITEM_ROWS = 1
private const val DEFAULT_ITEM_ROWS = 3

fun pushSummaryWidgetRemoteViews(context: Context, appWidgetId: Int) {
    AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, buildSummaryWidgetRemoteViews(context, appWidgetId))
}

private fun buildSummaryWidgetRemoteViews(context: Context, appWidgetId: Int): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_summary)
    val serviceName = loadSummaryWidgetConfig(context, appWidgetId)

    if (serviceName == null) {
        views.setTextViewText(R.id.summary_widget_title, "Tap to configure")
        views.setTextColor(R.id.summary_widget_title, TitleText.toArgb())
        views.setViewVisibility(R.id.summary_widget_header, View.GONE)
        ITEM_ROW_IDS.forEach { views.setViewVisibility(it, View.GONE) }
        views.setOnClickPendingIntent(R.id.summary_widget_root, openAppPendingIntent(context, appWidgetId, null))
        return views
    }

    views.setTextViewText(R.id.summary_widget_title, serviceName)
    views.setTextColor(R.id.summary_widget_title, TitleText.toArgb())
    views.setOnClickPendingIntent(R.id.summary_widget_root, openAppPendingIntent(context, appWidgetId, serviceName))

    val summary = loadParsedSummary(context, serviceName)
    if (summary == null) {
        views.setViewVisibility(R.id.summary_widget_header, View.GONE)
        showSingleRow(views, "Loading…", NormalText)
        return views
    }

    views.setViewVisibility(R.id.summary_widget_header, View.VISIBLE)
    views.setTextViewText(R.id.summary_widget_header, rowLabel(summary))
    views.setTextColor(R.id.summary_widget_header, colorFor(emphasisFor(summary)).toArgb())

    val names = itemNames(summary)
    if (names.isEmpty()) {
        showSingleRow(views, emptyPlaceholder(summary), NormalText)
        return views
    }

    val maxRows = availableItemRows(context, appWidgetId)
    val shownCount = minOf(names.size, maxRows, ITEM_ROW_IDS.size)
    // Reserve the last visible row for a "+N more" summary only when there's
    // genuine overflow — if everything fits, every row is a real name.
    val namesShown = if (names.size <= shownCount) names else names.take((shownCount - 1).coerceAtLeast(0))

    if (namesShown.isEmpty()) {
        // Not even one name fits — "+N more" would wrongly imply some are
        // already listed above it when none are, so this is its own
        // standalone count statement instead of a list continuation.
        showSingleRow(views, itemCountPhrase(summary, names.size), colorFor(emphasisFor(summary)))
        return views
    }

    ITEM_ROW_IDS.forEachIndexed { i, rowId ->
        when {
            i < namesShown.size -> {
                views.setViewVisibility(rowId, View.VISIBLE)
                views.setTextViewText(rowId, namesShown[i])
                views.setTextColor(rowId, NormalText.toArgb())
            }
            i == namesShown.size && names.size > namesShown.size -> {
                views.setViewVisibility(rowId, View.VISIBLE)
                views.setTextViewText(rowId, "+${names.size - namesShown.size} more")
                views.setTextColor(rowId, NormalText.toArgb())
            }
            else -> views.setViewVisibility(rowId, View.GONE)
        }
    }
    return views
}

private fun showSingleRow(views: RemoteViews, text: String, color: Color) {
    views.setViewVisibility(ITEM_ROW_IDS[0], View.VISIBLE)
    views.setTextViewText(ITEM_ROW_IDS[0], text)
    views.setTextColor(ITEM_ROW_IDS[0], color.toArgb())
    for (i in 1 until ITEM_ROW_IDS.size) views.setViewVisibility(ITEM_ROW_IDS[i], View.GONE)
}

// How many item rows fit the widget's current placed height — falls back to
// a small conservative default when the launcher hasn't reported a size yet
// (e.g. the very first render right after placement, before
// onAppWidgetOptionsChanged has fired).
private fun availableItemRows(context: Context, appWidgetId: Int): Int {
    val (_, heightDp) = widgetSizeDp(context, appWidgetId)
    if (heightDp <= 0) return DEFAULT_ITEM_ROWS
    val rows = (heightDp - FIXED_CHROME_DP) / ITEM_ROW_DP
    return rows.coerceIn(MIN_ITEM_ROWS, SUMMARY_WIDGET_MAX_ITEM_ROWS)
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
