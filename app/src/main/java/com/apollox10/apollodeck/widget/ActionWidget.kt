package com.apollox10.apollodeck.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

private val WidgetBackground = Color(0xFF1E2124)
private val WidgetText = Color(0xFFEEF0FA)
private val WarningBackground = Color(0xFF3A2A16)
private val WarningBorder = Color(0xFFF5A623)

class ActionWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val config = loadWidgetActionConfig(context, id)
        provideContent {
            GlanceTheme {
                WidgetContent(config)
            }
        }
    }
}

@Composable
private fun WidgetContent(config: WidgetActionConfig?) {
    val isWarning = config?.confirm == true
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(if (isWarning) WarningBackground else WidgetBackground))
            .cornerRadius(12.dp)
            .padding(8.dp)
            .let {
                if (config != null) {
                    it.clickable(actionRunCallback<ExecuteWidgetActionCallback>())
                } else {
                    it
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            // "⚠" as a plain text prefix rather than a separate icon —
            // guaranteed to render via RemoteViews, no bitmap-conversion
            // risk the way a Material icon would need here.
            text = when {
                config == null -> "Tap to configure"
                isWarning -> "⚠ ${config.label}"
                else -> config.label
            },
            style = TextStyle(
                color = ColorProvider(if (isWarning) WarningBorder else WidgetText),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

class ActionWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ActionWidget()
}
