package com.apollox10.apollodeck.ui.icons

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apollox10.apollodeck.ui.theme.BorderColor
import com.apollox10.apollodeck.ui.theme.ErrorRed
import com.apollox10.apollodeck.ui.theme.MonospaceTextStyle
import com.apollox10.apollodeck.ui.theme.WarningAmber

private const val GRID_COLUMNS = 6

// Every icon name the dashboard currently uses, each with its resolved
// preview and a tappable grid to override it — the fix for iconFor()
// mapping some backend icon name wrong (or not at all) without needing a
// code change: pick a replacement here and every icon-rendering site on
// both the phone and the watch (see IconOverrideStore/IconOverridePublisher)
// picks it up.
@Composable
fun IconOverrideScreen(
    viewModel: IconOverrideViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val overrides by viewModel.overrides.collectAsState()
    var expandedName by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("Icon overrides", style = MonospaceTextStyle, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
        Text(
            "Fix an icon the app can't map correctly — applies everywhere, including the watch",
            style = MonospaceTextStyle,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 20.dp),
        )

        when (val state = uiState) {
            is IconOverrideUiState.Loading -> Box(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onBackground)
            }

            is IconOverrideUiState.Error -> Text(state.message, style = MonospaceTextStyle, color = ErrorRed)

            is IconOverrideUiState.Loaded -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.iconNames.forEach { name ->
                        IconOverrideRow(
                            name = name,
                            overrideId = overrides[name],
                            isExpanded = expandedName == name,
                            onToggleExpand = { expandedName = if (expandedName == name) null else name },
                            onPick = { id ->
                                viewModel.setOverride(name, id)
                                expandedName = null
                            },
                            onReset = { viewModel.clearOverride(name) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IconOverrideRow(
    name: String,
    overrideId: String?,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onPick: (String) -> Unit,
    onReset: () -> Unit,
) {
    val context = LocalContext.current
    val isUnrecognized = overrideId == null && iconFor(name) == iconFor(null)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(0.5.dp, BorderColor, RoundedCornerShape(12.dp)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpand)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Icon(
                imageVector = resolvedIconFor(context, name),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(name, style = MonospaceTextStyle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                if (isUnrecognized) {
                    Text("Not recognized — showing a generic icon", style = MonospaceTextStyle, fontSize = 10.sp, color = WarningAmber)
                } else if (overrideId != null) {
                    Text("Overridden", style = MonospaceTextStyle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
            if (overrideId != null) {
                Text(
                    "Reset",
                    style = MonospaceTextStyle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.clickable(onClick = onReset).padding(4.dp),
                )
            }
        }

        if (isExpanded) {
            ChunkedIconGrid(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                onPick = onPick,
            )
        }
    }
}

@Composable
private fun ChunkedIconGrid(modifier: Modifier = Modifier, onPick: (String) -> Unit) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        availableIcons.chunked(GRID_COLUMNS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (id, icon) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .border(0.5.dp, BorderColor, RoundedCornerShape(10.dp))
                            .clickable { onPick(id) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = id,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                repeat(GRID_COLUMNS - row.size) { Box(modifier = Modifier.weight(1f)) }
            }
        }
    }
}
