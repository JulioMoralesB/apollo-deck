package com.apollox10.apollodeck.widget

import android.content.Context

// What a single placed per-service summary widget instance is bound to.
// Plain SharedPreferences, same reasoning as WidgetActionConfig — reads
// need to work synchronously from an AppWidgetProvider's onUpdate.
private const val SUMMARY_CONFIG_PREFS_NAME = "apollo_deck_summary_widget_configs"
private fun configKey(appWidgetId: Int) = "service_$appWidgetId"

fun saveSummaryWidgetConfig(context: Context, appWidgetId: Int, serviceName: String) {
    context.getSharedPreferences(SUMMARY_CONFIG_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(configKey(appWidgetId), serviceName)
        .apply()
}

fun loadSummaryWidgetConfig(context: Context, appWidgetId: Int): String? =
    context.getSharedPreferences(SUMMARY_CONFIG_PREFS_NAME, Context.MODE_PRIVATE).getString(configKey(appWidgetId), null)

fun deleteSummaryWidgetConfig(context: Context, appWidgetId: Int) {
    context.getSharedPreferences(SUMMARY_CONFIG_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .remove(configKey(appWidgetId))
        .apply()
}
