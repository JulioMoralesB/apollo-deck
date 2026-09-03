package com.apollox10.apollodeck.wearsync

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apollox10.apollodeck.core.model.Action
import com.apollox10.apollodeck.core.sync.TileGridActionRef
import com.apollox10.apollodeck.core.tile.TILE_ACCENT_COLORS
import com.apollox10.apollodeck.ui.icons.resolvedIconFor
import com.apollox10.apollodeck.ui.theme.BorderColor
import com.apollox10.apollodeck.ui.theme.ErrorRed
import com.apollox10.apollodeck.ui.theme.MonospaceTextStyle

// Picks which actions show in the watch's multi-action grid tile, and in
// what order — reusing the phone's screen and keyboard because doing this
// selection on the watch itself is exactly what we're trying to avoid (see
// TileGridPublisher). Tapping an action toggles it; the order tapped in is
// the order it'll appear in the grid, shown as a numbered badge.
@Composable
fun TileGridConfigScreen(
    onSaved: () -> Unit,
    viewModel: TileGridConfigViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val selected by viewModel.selected.collectAsState()
    val accentColor by viewModel.accentColor.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("Configure watch tile", style = MonospaceTextStyle, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
        Text(
            "Pick up to $MAX_TILE_GRID_ACTIONS actions for the watch's grid tile — tap order sets display order",
            style = MonospaceTextStyle,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 20.dp),
        )

        Text(
            "Selected (${selected.size}/$MAX_TILE_GRID_ACTIONS)",
            style = MonospaceTextStyle,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 8.dp),
        )

        when (val state = uiState) {
            is TileGridConfigUiState.Loading -> Box(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onBackground)
            }

            is TileGridConfigUiState.Error -> Text(state.message, style = MonospaceTextStyle, color = ErrorRed)

            is TileGridConfigUiState.Loaded -> {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    state.actionsByService.forEach { (svc, actions) ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                svc.uppercase(),
                                style = MonospaceTextStyle,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            )
                            actions.forEach { action ->
                                val ref = TileGridActionRef(svc, action.endpoint!!)
                                val position = selected.indexOf(ref).let { if (it >= 0) it + 1 else null }
                                ActionSelectRow(
                                    action = action,
                                    position = position,
                                    onClick = { viewModel.toggle(svc, action) },
                                )
                            }
                        }
                    }
                }

                SectionLabel("Color", modifier = Modifier.padding(top = 20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TILE_ACCENT_COLORS.forEach { color ->
                        ColorSwatch(
                            color = color,
                            isSelected = color == accentColor,
                            onClick = { viewModel.selectColor(color) },
                        )
                    }
                }

                Button(
                    onClick = { viewModel.save(onSaved) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onBackground,
                        contentColor = MaterialTheme.colorScheme.background,
                    ),
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 24.dp),
                ) {
                    Text("Save to watch", style = MonospaceTextStyle)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MonospaceTextStyle,
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        modifier = modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun ColorSwatch(color: Int, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(if (isSelected) 36.dp else 30.dp)
            .clip(CircleShape)
            .background(Color(color))
            .let {
                if (isSelected) it.border(2.dp, MaterialTheme.colorScheme.onBackground, CircleShape) else it
            }
            .clickable(onClick = onClick),
    )
}

@Composable
private fun ActionSelectRow(action: Action, position: Int?, onClick: () -> Unit) {
    val context = LocalContext.current
    val isSelected = position != null
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) MaterialTheme.colorScheme.onBackground else BorderColor,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.background)
                .border(0.5.dp, BorderColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Text(
                    position.toString(),
                    style = MonospaceTextStyle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.background,
                )
            }
        }
        Icon(
            imageVector = resolvedIconFor(context, action.icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 12.dp).size(20.dp),
        )
        Text(
            action.label,
            style = MonospaceTextStyle,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 12.dp).weight(1f),
        )
    }
}
