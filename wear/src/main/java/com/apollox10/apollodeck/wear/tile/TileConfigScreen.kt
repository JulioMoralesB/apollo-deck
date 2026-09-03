package com.apollox10.apollodeck.wear.tile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.apollox10.apollodeck.core.tile.TILE_ACCENT_COLORS
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

// Two steps: pick an action, then pick an accent color for it — mirrors the
// phone widget's configure flow (action, then icon/color) scaled down to
// what fits a round screen. Both steps live in one ScalingLazyColumn rather
// than separate screens, same reasoning as the phone's WidgetSetupScreen.
@Composable
fun TileConfigScreen(
    onSave: (TileConfigItem, Int) -> Unit,
    viewModel: TileConfigViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedItem by remember { mutableStateOf<TileConfigItem?>(null) }
    var selectedColor by remember { mutableIntStateOf(TILE_ACCENT_COLORS.first()) }

    when (val state = uiState) {
        is TileConfigUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is TileConfigUiState.Error -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = state.message, color = MaterialTheme.colors.error, style = MaterialTheme.typography.caption2)
                Chip(onClick = { viewModel.load() }, label = { Text("Retry") }, modifier = Modifier.fillMaxWidth())
            }
        }
        is TileConfigUiState.Loaded -> {
            val chosen = selectedItem
            ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
                if (chosen == null) {
                    item(key = "title") {
                        Text(
                            text = "Pick an action",
                            style = MaterialTheme.typography.title3,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                    }
                    if (state.items.isEmpty()) {
                        item(key = "empty") {
                            Text(text = "No actions available", style = MaterialTheme.typography.caption2)
                        }
                    }
                    items(state.items, key = { "${it.serviceName}:${it.action.endpoint}" }) { item ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colors.surface, RoundedCornerShape(50))
                                .clickable { selectedItem = item }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            Column {
                                Text(text = item.action.label, style = MaterialTheme.typography.button)
                                Text(
                                    text = item.serviceName,
                                    style = MaterialTheme.typography.caption3,
                                    color = MaterialTheme.colors.onSurfaceVariant,
                                )
                            }
                        }
                    }
                } else {
                    item(key = "color_title") {
                        Text(
                            text = chosen.action.label,
                            style = MaterialTheme.typography.title3,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                    }
                    item(key = "color_swatches") {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TILE_ACCENT_COLORS.forEach { color ->
                                ColorSwatch(
                                    color = color,
                                    isSelected = color == selectedColor,
                                    onClick = { selectedColor = color },
                                )
                            }
                        }
                    }
                    item(key = "save") {
                        Chip(
                            onClick = { onSave(chosen, selectedColor) },
                            label = { Text("Save") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item(key = "back") {
                        Chip(
                            onClick = { selectedItem = null },
                            label = { Text("Back") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(color: Int, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(if (isSelected) 32.dp else 26.dp)
            .background(Color(color), CircleShape)
            .let { if (isSelected) it.border(2.dp, MaterialTheme.colors.onSurface, CircleShape) else it }
            .clickable(onClick = onClick),
    )
}
