package com.apollox10.apollodeck.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apollox10.apollodeck.core.model.Action
import com.apollox10.apollodeck.ui.theme.BorderColor
import com.apollox10.apollodeck.ui.theme.ErrorRed
import com.apollox10.apollodeck.ui.theme.MonospaceTextStyle

@Composable
fun WidgetConfigureScreen(
    onActionPicked: (serviceName: String, action: Action) -> Unit,
    viewModel: WidgetConfigureViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        when (val state = uiState) {
            is WidgetConfigureUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onBackground)
            }

            is WidgetConfigureUiState.Error -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(state.message, style = MonospaceTextStyle, color = ErrorRed)
            }

            is WidgetConfigureUiState.Loaded -> Column {
                Text(
                    "Choose an action",
                    style = MonospaceTextStyle,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    "Pin one to your home screen",
                    style = MonospaceTextStyle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(state.actionsByService) { (serviceName, actions) ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                serviceName,
                                style = MonospaceTextStyle,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            actions.forEach { action ->
                                ActionRow(
                                    action = action,
                                    onClick = { onActionPicked(serviceName, action) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionRow(action: Action, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(0.5.dp, BorderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Text(
            if (action.confirm) "⚠ ${action.label}" else action.label,
            style = MonospaceTextStyle,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
