package com.apollox10.apollodeck.ui.dashboard

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apollox10.apollodeck.core.model.Action
import com.apollox10.apollodeck.core.model.Service
import com.apollox10.apollodeck.ui.icons.IconOverrideScreen
import com.apollox10.apollodeck.ui.icons.resolvedIconFor
import com.apollox10.apollodeck.ui.settings.SettingsScreen
import com.apollox10.apollodeck.ui.theme.BorderColor
import com.apollox10.apollodeck.ui.theme.ErrorRed
import com.apollox10.apollodeck.ui.theme.MonospaceTextStyle
import com.apollox10.apollodeck.ui.theme.OnlineGreen
import com.apollox10.apollodeck.wearsync.TileGridConfigScreen

// Cards size themselves to fit this, rather than a fixed column count — the
// same grid reflows on rotation instead of keeping a portrait column count
// in landscape.
private val CARD_MIN_SIZE = 110.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    onLogout: () -> Unit,
    viewModel: DashboardViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val actionResult by viewModel.actionResult.collectAsState()
    val actionStates by viewModel.actionStates.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedServiceName by remember { mutableStateOf<String?>(null) }
    var pendingAction by remember { mutableStateOf<Action?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showTileGridConfig by remember { mutableStateOf(false) }
    var showIconOverrides by remember { mutableStateOf(false) }

    BackHandler(enabled = selectedServiceName != null || showSettings || showTileGridConfig || showIconOverrides) {
        when {
            selectedServiceName != null -> selectedServiceName = null
            showTileGridConfig -> showTileGridConfig = false
            showIconOverrides -> showIconOverrides = false
            else -> showSettings = false
        }
    }

    fun handleActionTap(action: Action) {
        when {
            action.href != null -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(action.href)))
            action.confirm -> pendingAction = action
            else -> viewModel.executeAction(action)
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

    // Re-derived from the latest poll every time, not captured once — so
    // status/action changes on the currently open service still show up
    // live instead of freezing at whatever it looked like on tap.
    val selectedService = (uiState as? DashboardUiState.Loaded)
        ?.services
        ?.find { it.name == selectedServiceName }

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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Reserve this slot unconditionally — the title's start
                    // position stays fixed instead of shifting right
                    // whenever its contents change.
                    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        when {
                            selectedService != null -> IconButton(onClick = { selectedServiceName = null }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                )
                            }
                            showTileGridConfig -> IconButton(onClick = { showTileGridConfig = false }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                )
                            }
                            showIconOverrides -> IconButton(onClick = { showIconOverrides = false }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                )
                            }
                            showSettings -> IconButton(onClick = { showSettings = false }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                )
                            }
                            else -> IconButton(onClick = { showSettings = true }) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                )
                            }
                        }
                    }
                    Text(
                        when {
                            selectedService != null -> selectedService.name
                            showTileGridConfig -> "Watch Tile"
                            showIconOverrides -> "Icon Overrides"
                            showSettings -> "Settings"
                            else -> "Apollo Deck"
                        },
                        style = MonospaceTextStyle,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                TextButton(onClick = onLogout) {
                    Text("Log out", style = MonospaceTextStyle, fontSize = 12.sp)
                }
            }

            if (showTileGridConfig) {
                TileGridConfigScreen(onSaved = { showTileGridConfig = false })
            } else if (showIconOverrides) {
                IconOverrideScreen()
            } else if (showSettings) {
                SettingsScreen(
                    onSaved = onLogout,
                    onConfigureWatchTile = { showTileGridConfig = true },
                    onIconOverrides = { showIconOverrides = true },
                )
            } else when (val state = uiState) {
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

                is DashboardUiState.Loaded -> if (selectedService != null) {
                    ActionGrid(service = selectedService, actionStates = actionStates, onActionTap = ::handleActionTap)
                } else {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refreshNow(onSessionExpired = onLogout) },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(CARD_MIN_SIZE),
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
                                            selectedServiceName = service.name
                                        } else if (service.url != null) {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse(service.url))
                                            )
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
    }

    pendingAction?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = { Text("Run ${action.label}?", style = MonospaceTextStyle) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.executeAction(action)
                    pendingAction = null
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

@Composable
private fun ActionGrid(service: Service, actionStates: Map<String, ActionCardState>, onActionTap: (Action) -> Unit) {
    val actions = service.actions.orEmpty()
    if (actions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No actions configured.", style = MonospaceTextStyle, color = MaterialTheme.colorScheme.onBackground)
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(CARD_MIN_SIZE),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(actions) { action ->
            ActionButton(
                action = action,
                state = action.endpoint?.let { actionStates[it] },
                onClick = { onActionTap(action) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ServiceCard(service: Service, onClick: () -> Unit, onLongClick: () -> Unit) {
    val context = LocalContext.current
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
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Icon(
            imageVector = resolvedIconFor(context, service.icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp),
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                service.name,
                style = MonospaceTextStyle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 3.dp),
            ) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(statusColor))
                Text(
                    statusLabelFor(service.status),
                    style = MonospaceTextStyle,
                    fontSize = 9.sp,
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
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            service.url != null -> Text(
                "Open UI",
                style = MonospaceTextStyle,
                fontSize = 8.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            else -> Text("", fontSize = 8.sp)
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

// Mirrors the web dashboard's action-card success/error flash exactly:
// a 150ms crossfade into a tinted border + background on result, held for
// ACTION_STATE_HOLD_MS (see DashboardViewModel), then a 150ms fade back —
// same colors, same timing (ActionPanel.jsx / ActionPanel.css).
private const val STATE_TRANSITION_MS = 150

@Composable
private fun ActionButton(action: Action, state: ActionCardState?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val context = LocalContext.current
    val transitionSpec = tween<Color>(STATE_TRANSITION_MS)
    val borderColor by animateColorAsState(
        targetValue = when (state) {
            ActionCardState.Success -> OnlineGreen
            ActionCardState.Error -> ErrorRed
            else -> BorderColor
        },
        animationSpec = transitionSpec,
        label = "actionBorderColor",
    )
    val tintColor by animateColorAsState(
        targetValue = when (state) {
            ActionCardState.Success -> OnlineGreen.copy(alpha = 0.08f)
            ActionCardState.Error -> ErrorRed.copy(alpha = 0.08f)
            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0f)
        },
        animationSpec = transitionSpec,
        label = "actionTintColor",
    )

    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .background(tintColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .alpha(if (state == ActionCardState.Loading) 0.5f else 1f)
            .clickable(enabled = state != ActionCardState.Loading, onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = resolvedIconFor(context, action.icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp),
        )
        Text(
            action.label,
            style = MonospaceTextStyle,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
