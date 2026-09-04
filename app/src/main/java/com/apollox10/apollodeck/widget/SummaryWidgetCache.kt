package com.apollox10.apollodeck.widget

import android.content.Context
import com.apollox10.apollodeck.core.model.ServiceSummary
import com.apollox10.apollodeck.core.model.parseServiceSummary
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Cache written by SummaryRefreshWorker, read synchronously by both summary
// widgets' RemoteViews builders — same "render from local state only, never
// network on the render path" discipline as the Wear tiles (see apollo-deck
// CLAUDE.md). Separate SharedPreferences file from the action widgets'
// (PREFS_NAME in WidgetActionConfig.kt) since this is keyed by service name,
// not appWidgetId — one shared cache feeds every placed summary widget.
@Serializable
data class CachedSummary(
    val serviceName: String,
    val rawJson: String,
    val fetchedAtMillis: Long,
)

private const val SUMMARY_PREFS_NAME = "apollo_deck_summary_widgets"
// The list of service names that currently have a summary_endpoint, in the
// order /services returned them — the combined widget's row order and the
// single source of truth for pruning stale cache entries below.
private const val KEY_KNOWN_SERVICES = "known_summary_services"
private fun summaryKey(serviceName: String) = "summary_$serviceName"

fun saveSummaryCache(context: Context, serviceName: String, rawJson: String) {
    val cached = CachedSummary(serviceName, rawJson, System.currentTimeMillis())
    context.getSharedPreferences(SUMMARY_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(summaryKey(serviceName), Json.encodeToString(cached))
        .apply()
}

fun loadCachedSummary(context: Context, serviceName: String): CachedSummary? {
    val raw = context.getSharedPreferences(SUMMARY_PREFS_NAME, Context.MODE_PRIVATE)
        .getString(summaryKey(serviceName), null) ?: return null
    return try {
        Json.decodeFromString(raw)
    } catch (e: Exception) {
        null
    }
}

fun loadParsedSummary(context: Context, serviceName: String): ServiceSummary? =
    loadCachedSummary(context, serviceName)?.let { parseServiceSummary(it.rawJson) }

fun loadKnownSummaryServiceNames(context: Context): List<String> {
    val raw = context.getSharedPreferences(SUMMARY_PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_KNOWN_SERVICES, null) ?: return emptyList()
    return try {
        Json.decodeFromString(raw)
    } catch (e: Exception) {
        emptyList()
    }
}

// Called once per refresh cycle with every service the live /services
// response currently reports has a summary_endpoint, in that order — drops
// cache entries for anything no longer configured, so a removed service's
// stale data doesn't linger in the combined widget.
fun saveKnownSummaryServiceNames(context: Context, orderedNames: List<String>) {
    val prefs = context.getSharedPreferences(SUMMARY_PREFS_NAME, Context.MODE_PRIVATE)
    val stale = loadKnownSummaryServiceNames(context) - orderedNames.toSet()
    val editor = prefs.edit().putString(KEY_KNOWN_SERVICES, Json.encodeToString(orderedNames))
    stale.forEach { editor.remove(summaryKey(it)) }
    editor.apply()
}
