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

private data class RowViews(
    val root: Int,
    val compact: Int,
    val compactLabel: Int,
    val compactValue: Int,
    val expanded: Int,
    val expandedLabel: Int,
    val expandedValue: Int,
)

private val ROW_VIEWS = listOf(
    RowViews(
        R.id.combined_summary_widget_row1,
        R.id.combined_summary_widget_row1_compact, R.id.combined_summary_widget_row1_compact_label, R.id.combined_summary_widget_row1_compact_value,
        R.id.combined_summary_widget_row1_expanded, R.id.combined_summary_widget_row1_expanded_label, R.id.combined_summary_widget_row1_expanded_value,
    ),
    RowViews(
        R.id.combined_summary_widget_row2,
        R.id.combined_summary_widget_row2_compact, R.id.combined_summary_widget_row2_compact_label, R.id.combined_summary_widget_row2_compact_value,
        R.id.combined_summary_widget_row2_expanded, R.id.combined_summary_widget_row2_expanded_label, R.id.combined_summary_widget_row2_expanded_value,
    ),
    RowViews(
        R.id.combined_summary_widget_row3,
        R.id.combined_summary_widget_row3_compact, R.id.combined_summary_widget_row3_compact_label, R.id.combined_summary_widget_row3_compact_value,
        R.id.combined_summary_widget_row3_expanded, R.id.combined_summary_widget_row3_expanded_label, R.id.combined_summary_widget_row3_expanded_value,
    ),
    RowViews(
        R.id.combined_summary_widget_row4,
        R.id.combined_summary_widget_row4_compact, R.id.combined_summary_widget_row4_compact_label, R.id.combined_summary_widget_row4_compact_value,
        R.id.combined_summary_widget_row4_expanded, R.id.combined_summary_widget_row4_expanded_label, R.id.combined_summary_widget_row4_expanded_value,
    ),
)
const val COMBINED_SUMMARY_WIDGET_MAX_ROWS = 4

// Sizing estimates — see widget_combined_summary.xml. No callback reports
// actual rendered text/row height from RemoteViews, so all of this is
// approximate by design (deliberately conservative: better to leave a
// little unused space than clip a row).
private const val FIXED_CHROME_DP = 48
private const val COMPACT_ROW_DP = 24
private const val EXPANDED_ROW_DP = 50
private const val DEFAULT_AVAILABLE_HEIGHT_DP = 2 * COMPACT_ROW_DP
private const val HORIZONTAL_PADDING_DP = 28
private const val AVG_CHAR_WIDTH_DP = 6.5f
private const val MIN_CHARS_PER_LINE = 8

fun pushCombinedSummaryWidgetRemoteViews(context: Context, appWidgetId: Int) {
    AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, buildCombinedSummaryWidgetRemoteViews(context, appWidgetId))
}

private data class RowPlan(val serviceName: String, val expanded: Boolean)

// Decides, from the widget's current placed size, how many service rows to
// show and which of those get the expanded (label above, value up to 2
// lines) treatment vs the compact one-line fallback. Every row starts
// compact; whatever height is left over after fitting all shown rows that
// way upgrades rows to expanded, in order, one at a time — so with just
// enough room for a peek, still-shown rows stay compact and readable rather
// than partially expanding.
private fun planRows(context: Context, appWidgetId: Int, serviceNames: List<String>): List<RowPlan> {
    val (_, heightDp) = widgetSizeDp(context, appWidgetId)
    val available = if (heightDp > 0) heightDp - FIXED_CHROME_DP else DEFAULT_AVAILABLE_HEIGHT_DP

    val maxByHeight = (available / COMPACT_ROW_DP).coerceAtLeast(if (serviceNames.isEmpty()) 0 else 1)
    val shownCount = minOf(serviceNames.size, maxByHeight, COMBINED_SUMMARY_WIDGET_MAX_ROWS)
    val names = serviceNames.take(shownCount)

    var leftover = available - names.size * COMPACT_ROW_DP
    val expandCost = EXPANDED_ROW_DP - COMPACT_ROW_DP
    val expanded = BooleanArray(names.size)
    for (i in names.indices) {
        if (leftover < expandCost) break
        expanded[i] = true
        leftover -= expandCost
    }
    return names.mapIndexed { i, name -> RowPlan(name, expanded[i]) }
}

private fun buildCombinedSummaryWidgetRemoteViews(context: Context, appWidgetId: Int): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_combined_summary)
    val serviceNames = loadKnownSummaryServiceNames(context)

    if (serviceNames.isEmpty()) {
        ROW_VIEWS.forEach { views.setViewVisibility(it.root, View.GONE) }
        views.setViewVisibility(R.id.combined_summary_widget_empty, View.VISIBLE)
        views.setOnClickPendingIntent(R.id.combined_summary_widget_root, openAppPendingIntent(context, appWidgetId, 0, null))
        return views
    }

    views.setViewVisibility(R.id.combined_summary_widget_empty, View.GONE)
    val (widthDp, _) = widgetSizeDp(context, appWidgetId)
    val plan = planRows(context, appWidgetId, serviceNames)

    ROW_VIEWS.forEachIndexed { i, row ->
        val rowPlan = plan.getOrNull(i)
        if (rowPlan == null) {
            views.setViewVisibility(row.root, View.GONE)
            return@forEachIndexed
        }

        views.setViewVisibility(row.root, View.VISIBLE)
        val summary = loadParsedSummary(context, rowPlan.serviceName)
        val names = summary?.let { itemNames(it) } ?: emptyList()
        val color = (summary?.let { colorFor(emphasisFor(it)) } ?: NormalText).toArgb()
        val placeholder = when {
            summary == null -> "Loading…"
            names.isEmpty() -> emptyPlaceholder(summary)
            else -> null
        }

        if (rowPlan.expanded) {
            views.setViewVisibility(row.compact, View.GONE)
            views.setViewVisibility(row.expanded, View.VISIBLE)
            // The expanded label gets a full line to itself (not shared
            // with the value like the compact row), so it can afford the
            // more descriptive expandedRowLabel instead of rowLabel.
            views.setTextViewText(row.expandedLabel, summary?.let { expandedRowLabel(it) } ?: rowPlan.serviceName)
            val maxChars = estimateCharBudget(widthDp, lines = 2, fullWidth = true)
            views.setTextViewText(row.expandedValue, placeholder ?: joinTruncated(names, maxChars))
            views.setTextColor(row.expandedValue, color)
        } else {
            views.setViewVisibility(row.expanded, View.GONE)
            views.setViewVisibility(row.compact, View.VISIBLE)
            views.setTextViewText(row.compactLabel, summary?.let { rowLabel(it) } ?: rowPlan.serviceName)
            val maxChars = estimateCharBudget(widthDp, lines = 1, fullWidth = false)
            views.setTextViewText(row.compactValue, placeholder ?: joinTruncated(names, maxChars))
            views.setTextColor(row.compactValue, color)
        }
        views.setOnClickPendingIntent(row.root, openAppPendingIntent(context, appWidgetId, i + 1, rowPlan.serviceName))
    }

    return views
}

// Rough character budget for a value string given the widget's current
// width — fullWidth=false is the compact row, where label and value split
// the row via layout_weight so the value only gets half.
private fun estimateCharBudget(widthDp: Int, lines: Int, fullWidth: Boolean): Int {
    val usableWidth = if (widthDp > 0) widthDp - HORIZONTAL_PADDING_DP else 200
    val valueWidth = if (fullWidth) usableWidth else usableWidth / 2
    val charsPerLine = (valueWidth / AVG_CHAR_WIDTH_DP).toInt().coerceAtLeast(MIN_CHARS_PER_LINE)
    // Word-wrap loses some density versus raw per-line math (a line rarely
    // ends exactly at its last character), so multi-line budgets use 0.9x
    // per additional line rather than a flat multiply — conservative on
    // purpose, same reasoning as the row-height estimates above.
    return if (lines <= 1) charsPerLine else (charsPerLine * (1 + (lines - 1) * 0.9)).toInt()
}

private fun colorFor(emphasis: LineEmphasis) = when (emphasis) {
    LineEmphasis.Normal -> NormalText
    LineEmphasis.Warning -> WarningText
    LineEmphasis.Danger -> DangerText
}

// requestCode must be unique per row, not just per widget instance —
// otherwise every row's PendingIntent.getActivity call would return the
// same cached Intent (the first one built) since FLAG_UPDATE_CURRENT still
// matches on requestCode, and every row would open whichever service was
// configured first. rowSlot (0 for the empty-state fallback, 1..MAX_ROWS
// for real rows) keeps it small and collision-free instead of hashing the
// service name.
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
