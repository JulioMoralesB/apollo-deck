package com.apollox10.apollodeck.widget

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// What a single placed widget instance is bound to — captured once at
// configure time so the widget can render and execute without a network
// round-trip on every redraw. Re-run the configure flow to point a widget
// at a different action; there's no "edit in place".
@Serializable
data class WidgetActionConfig(
    val serviceName: String,
    val label: String,
    val endpoint: String,
    val method: String,
    // The widget always executes on tap, with no confirmation dialog —
    // there's no way to show one without launching an Activity, and the
    // point is a single tap. This is carried through purely so the widget
    // can render a visual warning as an ongoing reminder for actions that
    // require confirmation everywhere else.
    val confirm: Boolean,
    // Defaults to the action's own dashboard icon at configure time, but can
    // be overridden in the icon picker step — an id resolvable via
    // ui.icons.widgetIconFor.
    val iconName: String,
    // Style, chosen in the configure flow's last step. Defaults keep old
    // configs (saved before this field existed) rendering exactly as
    // before, since a missing field just decodes to its default.
    val accentId: String = "default",
    val showBackground: Boolean = true,
    val showLabel: Boolean = true,
    val iconSizeDp: Int = WidgetIconSize.Medium.dp,
)

// Plain SharedPreferences, keyed by the raw appWidgetId — not DataStore
// bound to a Glance session. Reads need to be instant and reliable from a
// BroadcastReceiver's onReceive (not a suspend context) and from a
// CoroutineWorker running under tight scheduling constraints; a
// synchronous, process-independent store sidesteps both.
internal const val PREFS_NAME = "apollo_deck_widgets"
private fun configKey(appWidgetId: Int) = "config_$appWidgetId"

fun saveWidgetActionConfig(context: Context, appWidgetId: Int, config: WidgetActionConfig) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(configKey(appWidgetId), Json.encodeToString(config))
        .apply()
}

fun loadWidgetActionConfig(context: Context, appWidgetId: Int): WidgetActionConfig? {
    val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(configKey(appWidgetId), null) ?: return null
    return try {
        Json.decodeFromString(raw)
    } catch (e: Exception) {
        null
    }
}

fun deleteWidgetActionConfig(context: Context, appWidgetId: Int) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .remove(configKey(appWidgetId))
        .remove(statusKey(appWidgetId))
        .apply()
}
