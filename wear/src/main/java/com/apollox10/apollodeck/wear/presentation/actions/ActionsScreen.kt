package com.apollox10.apollodeck.wear.presentation.actions

import androidx.compose.ui.platform.LocalContext
import com.apollox10.apollodeck.core.store.ActionPrefsStore
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

@Composable
fun ActionsScreen(
    onLogout: () -> Unit,
    viewModel: ActionsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val executionResult by viewModel.executionResult.collectAsState()

    when (val state = uiState) {
        is ActionsUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is ActionsUiState.Error -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = state.message, color = MaterialTheme.colors.error, style = MaterialTheme.typography.caption2)
                Chip(onClick = { viewModel.load() }, label = { Text("Retry") }, modifier = Modifier.fillMaxWidth())
            }
        }
        is ActionsUiState.Loaded -> {
            ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
                if (executionResult != null) {
                    item {
                        Text(
                            text = executionResult ?: "",
                            style = MaterialTheme.typography.caption2,
                            color = MaterialTheme.colors.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                }
                if (state.items.isEmpty()) {
                    item {
                        Text(text = "No actions available", style = MaterialTheme.typography.caption2)
                    }
                }
                items(state.items) { item ->
                    ActionRow(item = item, onExecute = { viewModel.execute(item) })
                }
                item {
                    Chip(
                        onClick = onLogout,
                        label = { Text("Log Out") },
                        colors = ChipDefaults.secondaryChipColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

// A confirm action executes only on long-press — the watch has no room for
// a confirmation dialog, so the press itself becomes the confirmation
// (mirrors the widget's precedent of using the gesture in place of a
// dialog it can't show either).
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActionRow(item: ActionItem, onExecute: () -> Unit) {
    // Live, not item.action.confirm — the list was loaded before any change
    // synced from the phone since (see ActionPrefsDataListenerService).
    val needsConfirm = item.action.endpoint?.let { ActionPrefsStore(LocalContext.current).isConfirmRequired(it) } == true
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.surface, RoundedCornerShape(50))
            .combinedClickable(
                onClick = { if (!needsConfirm) onExecute() },
                onLongClick = onExecute,
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        // "Device · action" labels (the convention for HA script names):
        // the action as the title, the device in place of the service name.
        val parts = item.action.label.split(" · ", limit = 2)
        val title = parts.last()
        val subtitle = if (parts.size == 2) parts.first() else item.serviceName
        Column {
            Text(text = title, style = MaterialTheme.typography.button)
            Text(
                text = if (needsConfirm) "$subtitle · long-press" else subtitle,
                style = MaterialTheme.typography.caption3,
                color = MaterialTheme.colors.onSurfaceVariant,
            )
        }
    }
}
