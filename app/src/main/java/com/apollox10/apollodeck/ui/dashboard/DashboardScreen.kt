package com.apollox10.apollodeck.ui.dashboard

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apollox10.apollodeck.core.model.Action
import com.apollox10.apollodeck.core.model.Service
import com.apollox10.apollodeck.ui.icons.iconFor
import com.apollox10.apollodeck.ui.theme.BorderColor
import com.apollox10.apollodeck.ui.theme.ErrorRed
import com.apollox10.apollodeck.ui.theme.MonospaceTextStyle
import com.apollox10.apollodeck.ui.theme.OnlineGreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    onLogout: () -> Unit,
    viewModel: DashboardViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val actionResult by viewModel.actionResult.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedService by remember { mutableStateOf<Service?>(null) }
    var pendingAction by remember { mutableStateOf<Action?>(null) }

    fun handleActionTap(action: Action) {
        when {
            action.href != null -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(action.href)))
            action.confirm -> pendingAction = action
            else -> {
                viewModel.executeAction(action)
                selectedService = null
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.startPolling(onSessionExpired = onLogout)
    }

    LaunchedEffect(actionResult) {
        actionResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionResult()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Text(data.visuals.message, style = MonospaceTextStyle, fontSize = 13.sp)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Apollo Deck",
                    style = MonospaceTextStyle,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                TextButton(onClick = onLogout) {
                    Text("Log out", style = MonospaceTextStyle, fontSize = 12.sp)
                }
            }

            when (val state = uiState) {
                is DashboardUiState.Loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onBackground)
                }

                is DashboardUiState.Error -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(state.message, style = MonospaceTextStyle, color = ErrorRed)
                }

                is DashboardUiState.Loaded -> PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refreshNow(onSessionExpired = onLogout) },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(state.services) { service ->
                            ServiceCard(
                                service = service,
                                onClick = {
                                    if (!service.actions.isNullOrEmpty()) {
                                        selectedService = service
                                    } else if (service.url != null) {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(service.url)))
                                    }
                                },
                                onLongClick = {
                                    service.actions?.singleOrNull()?.let { handleActionTap(it) }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    selectedService?.let { service ->
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { selectedService = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            ActionSheetContent(service = service, onActionTap = ::handleActionTap)
        }
    }

    pendingAction?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = { Text("Run ${action.label}?", style = MonospaceTextStyle) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.executeAction(action)
                    pendingAction = null
                    selectedService = null
                }) {
                    Text("Confirm", style = MonospaceTextStyle)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingAction = null }) {
                    Text("Cancel", style = MonospaceTextStyle)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ServiceCard(service: Service, onClick: () -> Unit, onLongClick: () -> Unit) {
    val hasActions = !service.actions.isNullOrEmpty()
    val isClickable = hasActions || service.url != null
    val statusColor = statusColorFor(service.status)

    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .let {
                if (isClickable) it.combinedClickable(onClick = onClick, onLongClick = onLongClick) else it
            }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Icon(
            imageVector = iconFor(service.icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(26.dp),
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                service.name,
                style = MonospaceTextStyle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(statusColor))
                Text(
                    statusLabelFor(service.status),
                    style = MonospaceTextStyle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        when {
            hasActions -> {
                val count = service.actions!!.size
                Text(
                    "$count ${if (count == 1) "action" else "actions"}",
                    style = MonospaceTextStyle,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            service.url != null -> Text(
                "Open UI",
                style = MonospaceTextStyle,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            else -> Text("", fontSize = 9.sp)
        }
    }
}

private fun statusColorFor(status: String) = when (status) {
    "online" -> OnlineGreen
    "offline" -> ErrorRed
    else -> BorderColor
}

private fun statusLabelFor(status: String) = when (status) {
    "online" -> "Online"
    "offline" -> "Offline"
    else -> "Unknown"
}

@Composable
private fun ActionSheetContent(service: Service, onActionTap: (Action) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
        Text(
            service.name,
            style = MonospaceTextStyle,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        val actions = service.actions.orEmpty()
        if (actions.isEmpty()) {
            Text(
                "No actions configured.",
                style = MonospaceTextStyle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            // A plain chunked Column, not LazyVerticalGrid — the action count
            // per service is always small, and a lazy grid needs a bounded
            // height, which it won't get for free inside a bottom sheet's
            // Column (crashes with an unbounded-height scrollable otherwise).
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                actions.chunked(3).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        row.forEach { action ->
                            ActionButton(
                                action = action,
                                modifier = Modifier.weight(1f),
                                onClick = { onActionTap(action) },
                            )
                        }
                        repeat(3 - row.size) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButton(action: Action, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = iconFor(action.icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(24.dp),
        )
        Text(
            action.label,
            style = MonospaceTextStyle,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
