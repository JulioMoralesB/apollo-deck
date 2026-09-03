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
import com.apollox10.apollodeck.core.model.Action
import com.apollox10.apollodeck.ui.theme.ApolloDeckTheme

// Launched by the system both when the widget is first placed and, since
// action_widget_info.xml declares widgetFeatures="reconfigurable", from the
// launcher's long-press "Settings"/"Edit" menu on an existing one — same
// Activity, same EXTRA_APPWIDGET_ID, in both cases. Must call
// setResult(RESULT_OK) and finish() for the placement/edit to stick — the
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

        // Config storage is plain SharedPreferences (see WidgetActionConfig.kt)
        // — synchronous, so a reconfigure's existing setup is available
        // immediately, no loading state needed.
        val existing = loadWidgetActionConfig(this, appWidgetId)
        val initial = existing?.let {
            WidgetSelection(
                serviceName = it.serviceName,
                action = Action(
                    label = it.label,
                    icon = it.iconName,
                    endpoint = it.endpoint,
                    method = it.method,
                    confirm = it.confirm,
                ),
                iconName = it.iconName,
                accentId = it.accentId,
                showBackground = it.showBackground,
                showLabel = it.showLabel,
                iconSizeDp = it.iconSizeDp,
            )
        }

        setContent {
            ApolloDeckTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WidgetSetupScreen(
                        initial = initial,
                        onPin = { selection -> finishConfiguring(selection) },
                    )
                }
            }
        }
    }

    private fun finishConfiguring(selection: WidgetSelection) {
        val endpoint = selection.action.endpoint ?: return

        saveWidgetActionConfig(
            this,
            appWidgetId,
            WidgetActionConfig(
                serviceName = selection.serviceName,
                label = selection.action.label,
                endpoint = endpoint,
                method = selection.action.method ?: "POST",
                confirm = selection.action.confirm,
                iconName = selection.iconName,
                accentId = selection.accentId,
                showBackground = selection.showBackground,
                showLabel = selection.showLabel,
                iconSizeDp = selection.iconSizeDp,
            ),
        )
        pushWidgetRemoteViews(this, appWidgetId)

        setResult(
            Activity.RESULT_OK,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
        )
        finish()
    }
}
