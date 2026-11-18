package com.apollox10.apollodeck.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apollox10.apollodeck.ui.icons.resolvedIconFor
import com.apollox10.apollodeck.ui.theme.ErrorRed
import com.apollox10.apollodeck.ui.theme.MonospaceTextStyle

// Order and confirm-before-running for Home Assistant's scripts (see
// ActionPrefsStore), applied on the phone and — synced — the watch.
@Composable
fun ActionPrefsScreen(
    viewModel: ActionPrefsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val confirmRequired by viewModel.confirmRequired.collectAsState()
    val context = LocalContext.current
    val dimmed = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("Actions", style = MonospaceTextStyle, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
        Text(
            "Arrows set the order. The switch makes a script ask first — a dialog on the dashboard, " +
                "long-press on the watch, a ⚠ on widgets",
            style = MonospaceTextStyle,
            fontSize = 12.sp,
            color = dimmed,
            modifier = Modifier.padding(bottom = 20.dp),
        )

        when (val state = uiState) {
            is ActionPrefsUiState.Loading -> Box(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onBackground)
            }

            is ActionPrefsUiState.Error -> Text(state.message, style = MonospaceTextStyle, color = ErrorRed)

            is ActionPrefsUiState.Loaded -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (state.actions.isEmpty()) {
                    Text("No scripts in Home Assistant", style = MonospaceTextStyle, fontSize = 12.sp)
                }
                Text(
                    "Confirm",
                    style = MonospaceTextStyle,
                    fontSize = 10.sp,
                    color = dimmed,
                    modifier = Modifier.align(Alignment.End),
                )
                state.actions.forEachIndexed { index, action ->
                    val entityId = action.endpoint ?: return@forEachIndexed
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        IconButton(onClick = { viewModel.move(index, index - 1) }, enabled = index > 0) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up")
                        }
                        IconButton(onClick = { viewModel.move(index, index + 1) }, enabled = index < state.actions.lastIndex) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down")
                        }
                        Icon(
                            resolvedIconFor(context, action.icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 8.dp).size(20.dp),
                        )
                        Text(
                            action.label,
                            style = MonospaceTextStyle,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = entityId in confirmRequired,
                            onCheckedChange = { viewModel.setConfirmRequired(entityId, it) },
                        )
                    }
                }
            }
        }
    }
}
