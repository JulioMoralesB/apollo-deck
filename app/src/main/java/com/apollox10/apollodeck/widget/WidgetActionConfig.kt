package com.apollox10.apollodeck.widget

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
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
    // The widget always executes on tap, with no confirmation dialog — a
    // Glance widget can't show one without launching an Activity, and the
    // point is a single tap. This is carried through purely so the widget
    // can render a visual warning as an ongoing reminder for actions that
    // require confirmation everywhere else.
    val confirm: Boolean,
)

private val CONFIG_KEY = stringPreferencesKey("widget_action_config")

suspend fun saveWidgetActionConfig(context: Context, glanceId: GlanceId, config: WidgetActionConfig) {
    updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
        prefs.toMutablePreferences().apply {
            this[CONFIG_KEY] = Json.encodeToString(config)
        }
    }
}

suspend fun loadWidgetActionConfig(context: Context, glanceId: GlanceId): WidgetActionConfig? {
    val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
    val raw = prefs[CONFIG_KEY] ?: return null
    return try {
        Json.decodeFromString(raw)
    } catch (e: Exception) {
        null
    }
}
