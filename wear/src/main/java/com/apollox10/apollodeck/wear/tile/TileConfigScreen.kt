package com.apollox10.apollodeck.wear.tile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

@Composable
fun TileConfigScreen(
    onSelected: () -> Unit,
    viewModel: TileConfigViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

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
            ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
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
                items(state.items) { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colors.surface, RoundedCornerShape(50))
                            .clickable {
                                viewModel.select(item)
                                onSelected()
                            }
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
            }
        }
    }
}
