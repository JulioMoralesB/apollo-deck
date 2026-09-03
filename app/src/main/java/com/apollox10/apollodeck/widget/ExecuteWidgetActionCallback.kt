package com.apollox10.apollodeck.widget

import android.content.Context
import android.widget.Toast
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.apollox10.apollodeck.BuildConfig
import com.apollox10.apollodeck.core.net.ApiClient

// Runs on tap. No confirmation step — a Glance widget can't show a dialog
// without launching an Activity, and a single tap is the whole point.
// Reuses whatever session is already stored by the phone app; if it's
// expired, the shared OkHttp Authenticator in :core refreshes it the same
// way the app itself does, transparently.
class ExecuteWidgetActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val config = loadWidgetActionConfig(context, glanceId)
        if (config == null) {
            Toast.makeText(context, "Widget not configured", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val client = ApiClient.create(context, debugLogging = BuildConfig.DEBUG)
            val result = client.actionExecutor.execute(config.endpoint, config.method)
            val message = when {
                !result.message.isNullOrBlank() -> "${config.label}: ${result.message}"
                result.success -> "${config.label}: done"
                else -> "${config.label}: failed"
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "${config.label}: ${e.message ?: "failed"}", Toast.LENGTH_SHORT).show()
        }
    }
}
