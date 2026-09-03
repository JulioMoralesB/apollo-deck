package com.apollox10.apollodeck.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.apollox10.apollodeck.MainActivity
import com.apollox10.apollodeck.R
import com.apollox10.apollodeck.ui.icons.widgetIconFor

private val DefaultText = Color(0xFFEEF0FA)
private val WarningText = Color(0xFFF5A623)
private val SuccessText = Color(0xFF3DDC84)
private val ErrorText = Color(0xFFFF5C5C)
private const val ICON_PX_AT_DENSITY_1X = 28

// Builds the widget's RemoteViews directly and pushes it via
// AppWidgetManager — not through Jetpack Glance's Composable/session
// pipeline. Glance's own SessionWorker was observed (via logcat, across
// several rounds of testing) to silently fail to re-run when update() was
// called from a background-triggered context (an ActionCallback broadcast,
// or a CoroutineWorker) on this device: AppWidgetManager.updateAppWidget()
// still fired on schedule, but with stale, previously-cached content,
// because the actual recomposition never happened. Building and pushing
// RemoteViews here directly removes that whole session/recomposition layer
// — this call is what AppWidgetManager.updateAppWidget() ultimately needs
// either way, so there's nothing Glance was adding for this widget beyond
// a convenience API that turned out to be unreliable for exactly the calls
// (background-triggered updates) this widget depends on most.
fun pushWidgetRemoteViews(context: Context, appWidgetId: Int) {
    AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, buildWidgetRemoteViews(context, appWidgetId))
}

private fun buildWidgetRemoteViews(context: Context, appWidgetId: Int): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_action)
    val config = loadWidgetActionConfig(context, appWidgetId)

    if (config == null) {
        views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_bg_default)
        views.setImageViewBitmap(
            R.id.widget_icon,
            Icons.Default.Info.toBitmap(sizePx = ICON_PX_AT_DENSITY_1X.dpToPx(context), tint = DefaultText),
        )
        views.setTextViewText(R.id.widget_label, "Tap to configure")
        views.setTextColor(R.id.widget_label, DefaultText.toArgb())
        views.setViewVisibility(R.id.widget_label, View.VISIBLE)
        // Not configured shouldn't normally happen (configure runs before
        // the widget ever renders), but open the app rather than leaving a
        // dead tap target if it ever does.
        views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent(context, appWidgetId))
        return views
    }

    val status = loadWidgetStatus(context, appWidgetId)
    val isWarning = config.confirm
    val accent = accentFor(config.accentId)

    // Precedence: a transient result always wins (it's what the user just
    // triggered), then the confirm warning (a standing safety cue that
    // shouldn't be silenced by a style choice), then the chosen accent.
    val (bgRes, textColor, icon) = when (status) {
        WidgetStatus.Success -> Triple(R.drawable.widget_bg_green, SuccessText, Icons.Default.Check)
        WidgetStatus.Error -> Triple(R.drawable.widget_bg_red, ErrorText, Icons.Default.Close)
        null -> if (isWarning) {
            Triple(R.drawable.widget_bg_orange, WarningText, widgetIconFor(config.iconName))
        } else {
            Triple(accent.drawableRes, accent.text, widgetIconFor(config.iconName))
        }
    }

    // "Show background" is an idle-state preference. Mid-feedback, the
    // background flash IS the feedback, so it always renders regardless.
    val showBackground = config.showBackground || status != null
    if (showBackground) {
        views.setInt(R.id.widget_root, "setBackgroundResource", bgRes)
    } else {
        views.setInt(R.id.widget_root, "setBackgroundColor", android.graphics.Color.TRANSPARENT)
    }

    val effectiveIconDp = WidgetIconSize.fromDp(config.iconSizeDp).effectiveDp(config.showLabel)
    views.setImageViewBitmap(
        R.id.widget_icon,
        icon.toBitmap(sizePx = effectiveIconDp.dpToPx(context), tint = textColor),
    )

    if (config.showLabel) {
        val labelText = if (isWarning && status == null) "⚠ ${config.label}" else config.label
        views.setTextViewText(R.id.widget_label, labelText)
        views.setTextColor(R.id.widget_label, textColor.toArgb())
        views.setViewVisibility(R.id.widget_label, View.VISIBLE)
    } else {
        views.setViewVisibility(R.id.widget_label, View.GONE)
    }

    // Mid-feedback: no click listener set at all, so taps are ignored
    // until the revert work clears the status back to idle.
    if (status == null) {
        views.setOnClickPendingIntent(R.id.widget_root, executePendingIntent(context, appWidgetId))
    }

    return views
}

private fun openAppPendingIntent(context: Context, appWidgetId: Int): PendingIntent {
    val intent = Intent(context, MainActivity::class.java)
    return PendingIntent.getActivity(
        context,
        appWidgetId,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

private fun executePendingIntent(context: Context, appWidgetId: Int): PendingIntent {
    val intent = Intent(context, ActionWidgetReceiver::class.java).apply {
        action = ActionWidgetReceiver.ACTION_EXECUTE
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }
    return PendingIntent.getBroadcast(
        context,
        appWidgetId,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

private fun Int.dpToPx(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()
