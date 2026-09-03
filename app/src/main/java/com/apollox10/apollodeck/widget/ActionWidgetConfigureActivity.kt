package com.apollox10.apollodeck.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import com.apollox10.apollodeck.core.model.Action
import com.apollox10.apollodeck.ui.theme.ApolloDeckTheme
import kotlinx.coroutines.launch

// Launched automatically by the system when the widget is placed (declared
// via android:configure in the widget's info XML). Must call
// setResult(RESULT_OK) and finish() for the placement to stick — the
// default RESULT_CANCELED (set immediately below) is what happens if the
// user backs out without picking anything.
class ActionWidgetConfigureActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            ApolloDeckTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WidgetConfigureScreen(onActionPicked = ::onActionPicked)
                }
            }
        }
    }

    private fun onActionPicked(serviceName: String, action: Action) {
        val endpoint = action.endpoint ?: return
        lifecycleScope.launch {
            val glanceId = GlanceAppWidgetManager(this@ActionWidgetConfigureActivity)
                .getGlanceIdBy(appWidgetId)

            saveWidgetActionConfig(
                this@ActionWidgetConfigureActivity,
                glanceId,
                WidgetActionConfig(
                    serviceName = serviceName,
                    label = action.label,
                    endpoint = endpoint,
                    method = action.method ?: "POST",
                    confirm = action.confirm,
                ),
            )
            ActionWidget().update(this@ActionWidgetConfigureActivity, glanceId)

            setResult(
                Activity.RESULT_OK,
                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
            )
            finish()
        }
    }
}
