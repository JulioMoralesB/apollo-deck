package com.apollox10.apollodeck.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apollox10.apollodeck.core.model.Action
import com.apollox10.apollodeck.ui.icons.availableIcons
import com.apollox10.apollodeck.ui.icons.iconFor
import com.apollox10.apollodeck.ui.icons.widgetIconFor
import com.apollox10.apollodeck.ui.theme.BorderColor
import com.apollox10.apollodeck.ui.theme.ErrorRed
import com.apollox10.apollodeck.ui.theme.MonospaceTextStyle
import com.apollox10.apollodeck.ui.theme.WarningAmber

private val TILE_MIN_SIZE = 64.dp
private val PREVIEW_SIZE = 120.dp
private val WarningBackgroundPreview = Color(0xFF3A2A16)
private const val GRID_COLUMNS = 5

data class WidgetSelection(
    val serviceName: String,
    val action: Action,
    val iconName: String,
    val accentId: String,
    val showBackground: Boolean,
    val showLabel: Boolean,
    val iconSizeDp: Int,
)

// One scrollable page for the whole configure flow — action, icon, and
// style used to be three separate screens; combined here per feedback that
// stepping through pages felt heavier than it needed to for what's really
// a handful of related choices. The icon/color grids are plain chunked
// Rows rather than LazyVerticalGrid, since nesting a lazy grid inside this
// screen's own scroll would crash on unbounded height — safe here because
// the option counts are small and don't need virtualization anyway.
@Composable
fun WidgetSetupScreen(
    initial: WidgetSelection?,
    onPin: (WidgetSelection) -> Unit,
    viewModel: WidgetConfigureViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    var serviceName by remember { mutableStateOf(initial?.serviceName) }
    var action by remember { mutableStateOf(initial?.action) }
    var iconName by remember { mutableStateOf(initial?.iconName) }
    var accentId by remember { mutableStateOf(initial?.accentId ?: "default") }
    var showBackground by remember { mutableStateOf(initial?.showBackground ?: true) }
    var showLabel by remember { mutableStateOf(initial?.showLabel ?: true) }
    var iconSize by remember { mutableStateOf(WidgetIconSize.fromDp(initial?.iconSizeDp ?: WidgetIconSize.Medium.dp)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("Configure widget", style = MonospaceTextStyle, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
        Text(
            "Pick an action, then an icon and a look for it",
            style = MonospaceTextStyle,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 20.dp),
        )

        SectionLabel("Action")
        when (val state = uiState) {
            is WidgetConfigureUiState.Loading -> Box(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onBackground)
            }

            is WidgetConfigureUiState.Error -> Text(state.message, style = MonospaceTextStyle, color = ErrorRed)

            is WidgetConfigureUiState.Loaded -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                state.actionsByService.forEach { (svc, actions) ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            svc.uppercase(),
                            style = MonospaceTextStyle,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        )
                        actions.forEach { a ->
                            ActionRow(
                                action = a,
                                isSelected = action == a && serviceName == svc,
                                onClick = {
                                    serviceName = svc
                                    action = a
                                    iconName = a.icon
                                },
                            )
                        }
                    }
                }
            }
        }

        val currentAction = action
        if (currentAction != null && iconName != null) {
            Spacer(modifier = Modifier.height(24.dp))
            SectionLabel("Icon — the dashboard's own is outlined")
            ChunkedGrid(availableIcons, GRID_COLUMNS) { (name, _) ->
                IconTile(name = name, isDefault = name == currentAction.icon, isSelected = name == iconName, onClick = { iconName = name })
            }

            Spacer(modifier = Modifier.height(24.dp))
            SectionLabel("Preview")
            WidgetPreview(
                label = currentAction.label,
                iconName = iconName!!,
                confirm = currentAction.confirm,
                accent = accentFor(accentId),
                showBackground = showBackground,
                showLabel = showLabel,
                iconSize = iconSize,
            )

            SectionLabel("Color")
            ChunkedGrid(widgetAccents, GRID_COLUMNS) { accent ->
                ColorSwatch(accent = accent, isSelected = accent.id == accentId, onClick = { accentId = accent.id })
            }

            Spacer(modifier = Modifier.height(16.dp))
            SectionLabel("Icon size")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                WidgetIconSize.entries.forEach { size ->
                    SizeOption(size = size, isSelected = size == iconSize, onClick = { iconSize = size }, modifier = Modifier.weight(1f))
                }
            }

            ToggleRow(label = "Show background", checked = showBackground, onCheckedChange = { showBackground = it })
            ToggleRow(label = "Show label", checked = showLabel, onCheckedChange = { showLabel = it })

            Button(
                onClick = {
                    onPin(
                        WidgetSelection(
                            serviceName = serviceName!!,
                            action = currentAction,
                            iconName = iconName!!,
                            accentId = accentId,
                            showBackground = showBackground,
                            showLabel = showLabel,
                            iconSizeDp = iconSize.dp,
                        ),
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background,
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp),
            ) {
                Text("Pin to home screen", style = MonospaceTextStyle)
            }
        }
    }
}

@Composable
private fun <T> ChunkedGrid(items: List<T>, columns: Int, content: @Composable (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 4.dp)) {
        items.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) { content(item) }
                }
                repeat(columns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MonospaceTextStyle,
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun ActionRow(action: Action, isSelected: Boolean, onClick: () -> Unit) {
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
        Icon(
            imageVector = iconFor(action.icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp),
        )
        Text(
            action.label,
            style = MonospaceTextStyle,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 12.dp).weight(1f),
        )
        if (action.confirm) Text("⚠", color = WarningAmber, fontSize = 14.sp)
    }
}

@Composable
private fun IconTile(name: String, isDefault: Boolean, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) MaterialTheme.colorScheme.onBackground else BorderColor,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = widgetIconFor(name),
            contentDescription = name,
            tint = if (isDefault) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ColorSwatch(accent: WidgetAccent, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.aspectRatio(1f).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // Filled with accent.text, not accent.background — icon/label are
        // always tinted with accent.text regardless of the background
        // toggle, so that's the color guaranteed visible on the widget.
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .padding(if (isSelected) 2.dp else 6.dp)
                .background(accent.text, CircleShape)
                .border(
                    width = if (isSelected) 2.dp else 0.5.dp,
                    color = if (isSelected) accent.text else BorderColor,
                    shape = CircleShape,
                ),
        )
    }
}

@Composable
private fun SizeOption(size: WidgetIconSize, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) MaterialTheme.colorScheme.onBackground else BorderColor,
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Text(size.label, style = MonospaceTextStyle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onCheckedChange(!checked) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MonospaceTextStyle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.onBackground,
                checkedThumbColor = MaterialTheme.colorScheme.background,
            ),
        )
    }
}

@Composable
private fun WidgetPreview(
    label: String,
    iconName: String,
    confirm: Boolean,
    accent: WidgetAccent,
    showBackground: Boolean,
    showLabel: Boolean,
    iconSize: WidgetIconSize,
) {
    val background = if (confirm) WarningBackgroundPreview else accent.background
    val textColor = if (confirm) WarningAmber else accent.text

    Box(
        modifier = Modifier
            .padding(bottom = 20.dp)
            .size(PREVIEW_SIZE)
            .let { if (showBackground) it.background(background, RoundedCornerShape(16.dp)) else it }
            .border(0.5.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = widgetIconFor(iconName),
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(iconSize.effectiveDp(showLabel).dp),
            )
            if (showLabel) {
                Text(
                    (if (confirm) "⚠ " else "") + label,
                    style = MonospaceTextStyle,
                    fontSize = 11.sp,
                    color = textColor,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
