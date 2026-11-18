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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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

// One switch per Home Assistant script. A checked script asks before
// running everywhere it can be triggered (see ConfirmActionStore), on the
// phone and — synced — the watch.
@Composable
fun ConfirmActionsScreen(
    viewModel: ConfirmActionsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val required by viewModel.required.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("Confirm before running", style = MonospaceTextStyle, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
        Text(
            "Checked scripts ask first — a dialog on the dashboard, long-press on the watch, a ⚠ on widgets",
            style = MonospaceTextStyle,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 20.dp),
        )

        when (val state = uiState) {
            is ConfirmActionsUiState.Loading -> Box(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onBackground)
            }

            is ConfirmActionsUiState.Error -> Text(state.message, style = MonospaceTextStyle, color = ErrorRed)

            is ConfirmActionsUiState.Loaded -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.actions.isEmpty()) {
                    Text("No scripts in Home Assistant", style = MonospaceTextStyle, fontSize = 12.sp)
                }
                state.actions.forEach { action ->
                    val entityId = action.endpoint ?: return@forEach
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            resolvedIconFor(context, action.icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(20.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(action.label, style = MonospaceTextStyle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground)
                            Text(
                                entityId,
                                style = MonospaceTextStyle,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            )
                        }
                        Switch(
                            checked = entityId in required,
                            onCheckedChange = { viewModel.setRequired(entityId, it) },
                        )
                    }
                }
            }
        }
    }
}
