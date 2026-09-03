package com.apollox10.apollodeck.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.apollox10.apollodeck.BuildConfig
import com.apollox10.apollodeck.core.net.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private const val NETWORK_TIMEOUT_MS = 8000L

// Plain AppWidgetProvider — see WidgetRemoteViews.kt for why this isn't a
// Jetpack Glance GlanceAppWidget.
class ActionWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { pushWidgetRemoteViews(context, it) }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { deleteWidgetActionConfig(context, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_EXECUTE) return

        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                executeAction(context, appWidgetId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_EXECUTE = "com.apollox10.apollodeck.widget.ACTION_EXECUTE"
    }
}

// Runs on tap. No confirmation step — there's no way to show one without
// launching an Activity, and a single tap is the whole point. Reuses
// whatever session is already stored by the phone app; if it's expired, the
// shared OkHttp Authenticator in :core refreshes it the same way the app
// itself does, transparently.
private suspend fun executeAction(context: Context, appWidgetId: Int) {
    val config = loadWidgetActionConfig(context, appWidgetId)
    if (config == null) {
        toast(context, "Widget not configured")
        return
    }

    try {
        val (status, message) = runAction(context, config)
        toast(context, message)
        setWidgetStatus(context, appWidgetId, status)
        pushWidgetRemoteViews(context, appWidgetId)
    } finally {
        scheduleWidgetStatusRevert(context, appWidgetId)
    }
}

private suspend fun runAction(context: Context, config: WidgetActionConfig): Pair<WidgetStatus, String> {
    return try {
        val result = withTimeoutOrNull(NETWORK_TIMEOUT_MS) {
            val client = ApiClient.create(context, debugLogging = BuildConfig.DEBUG)
            client.actionExecutor.execute(config.endpoint, config.method)
        } ?: return WidgetStatus.Error to "${config.label}: timed out"

        val text = when {
            !result.message.isNullOrBlank() -> "${config.label}: ${result.message}"
            result.success -> "${config.label}: done"
            else -> "${config.label}: failed"
        }
        (if (result.success) WidgetStatus.Success else WidgetStatus.Error) to text
    } catch (e: Exception) {
        WidgetStatus.Error to "${config.label}: ${e.message ?: "failed"}"
    }
}

// Runs on a background dispatcher here — Toast requires a thread with a
// prepared Looper.
private suspend fun toast(context: Context, message: String) {
    withContext(Dispatchers.Main) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
