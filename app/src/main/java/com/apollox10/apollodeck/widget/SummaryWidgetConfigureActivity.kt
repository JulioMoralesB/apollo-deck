package com.apollox10.apollodeck.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apollox10.apollodeck.BuildConfig
import com.apollox10.apollodeck.core.net.ApiClient
import com.apollox10.apollodeck.ui.theme.ApolloDeckTheme
import com.apollox10.apollodeck.ui.theme.BorderColor
import com.apollox10.apollodeck.ui.theme.ErrorRed
import com.apollox10.apollodeck.ui.theme.MonospaceTextStyle
import kotlinx.coroutines.launch

// Same launch contract as ActionWidgetConfigureActivity — first placement
// and long-press "Edit" both land here with EXTRA_APPWIDGET_ID; must
// setResult(RESULT_OK) + finish() for the placement to stick.
class SummaryWidgetConfigureActivity : ComponentActivity() {

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

        val existing = loadSummaryWidgetConfig(this, appWidgetId)

        setContent {
            ApolloDeckTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SummaryWidgetPickerScreen(
                        initialServiceName = existing,
                        onPick = { serviceName -> finishConfiguring(serviceName) },
                    )
                }
            }
        }
    }

    private fun finishConfiguring(serviceName: String) {
        saveSummaryWidgetConfig(this, appWidgetId, serviceName)
        pushSummaryWidgetRemoteViews(this, appWidgetId)
        ensureSummaryRefreshScheduled(this)
        refreshSummaryWidgetsNow(this)

        setResult(
            Activity.RESULT_OK,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
        )
        finish()
    }
}

private sealed interface PickerUiState {
    data object Loading : PickerUiState
    data class Loaded(val serviceNames: List<String>) : PickerUiState
    data class Error(val message: String) : PickerUiState
}

@Composable
private fun SummaryWidgetPickerScreen(initialServiceName: String?, onPick: (String) -> Unit) {
    val context = LocalContext.current
    var uiState by remember { mutableStateOf<PickerUiState>(PickerUiState.Loading) }

    LaunchedEffect(Unit) {
        launch {
            uiState = try {
                val client = ApiClient.create(context, debugLogging = BuildConfig.DEBUG)
                val names = client.authenticatedApi.getServices()
                    .filter { it.summaryEndpoint != null }
                    .map { it.name }
                if (names.isEmpty()) {
                    PickerUiState.Error("No services with a summary configured")
                } else {
                    PickerUiState.Loaded(names)
                }
            } catch (e: Exception) {
                PickerUiState.Error(e.message ?: "Could not load services")
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Choose a summary", style = MonospaceTextStyle, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
        Text(
            "Which service's summary should this widget show?",
            style = MonospaceTextStyle,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 20.dp),
        )

        when (val state = uiState) {
            is PickerUiState.Loading -> Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onBackground)
            }

            is PickerUiState.Error -> Text(state.message, style = MonospaceTextStyle, color = ErrorRed)

            is PickerUiState.Loaded -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.serviceNames.forEach { name ->
                    ServiceRow(name = name, isSelected = name == initialServiceName, onClick = { onPick(name) })
                }
            }
        }
    }
}

@Composable
private fun ServiceRow(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) MaterialTheme.colorScheme.onBackground else BorderColor,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Text(name, style = MonospaceTextStyle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
