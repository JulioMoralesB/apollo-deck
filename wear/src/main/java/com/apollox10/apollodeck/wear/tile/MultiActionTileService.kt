package com.apollox10.apollodeck.wear.tile

import android.content.Intent
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.wrap
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.LayoutElementBuilders.VERTICAL_ALIGN_CENTER
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.material.Button
import androidx.wear.protolayout.material.ButtonColors
import androidx.wear.protolayout.material.ButtonDefaults
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
import com.apollox10.apollodeck.core.model.Action
import com.apollox10.apollodeck.core.store.IconOverrideStore
import com.apollox10.apollodeck.core.sync.TileGridActionRef
import com.apollox10.apollodeck.core.tile.TILE_ACCENT_COLORS
import com.apollox10.apollodeck.wear.icons.resolvedIconFor
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.guava.future

private const val GRID_COLUMNS = 3
// Configured from the phone (see TileGridSelection): as many as the user
// picked, up to this cap, so the grid never outgrows a round face. With
// nothing configured yet, the unconfigured default is smaller (see
// FALLBACK_ACTIONS) so a fresh watch isn't showing a wall of icons the user
// never chose.
private const val MAX_GRID_ACTIONS = 9
private const val FALLBACK_ACTIONS = 4
private const val ICON_SIZE_PX = 48
private const val CLICK_ID_PREFIX = "action_"
private const val RES_ID_PREFIX = "grid_icon_"

private data class MultiTileAction(val serviceName: String, val action: Action)

// A grid of tappable icons — one per action, no labels — styled after
// Home Assistant's Wear shortcuts tile and the watch's own app-launcher
// grid, rather than the stacked text rows this used to be. Which actions
// show, in what order, and the accent color they're tinted with are all
// configured from the phone app (see TileGridSelection/TileGridSync) since
// picking many actions (or a color) on a watch keyboard-less UI is
// painful; with nothing configured yet, this falls back to the first few
// executable actions across all services, in the palette's default color,
// so the tile isn't empty out of the box.
//
// Tapping an icon fires it immediately, except for a confirm: true action
// (Free Games Notifier's "Check E2E"/"Resend Notification", say) — a Tile's
// Clickable can't tell a long-press from a tap, so those arm on the first
// tap (rendered in a warning color) and only fire on a second tap within
// GRID_ARM_TIMEOUT_MS; see TileGridArmState. Whichever icon last completed
// a run flashes green/red for GRID_STATUS_HOLD_MS (TileGridActionStatus) —
// same as ActionTileService and the phone widget, nothing here is
// fire-and-forget.
class MultiActionTileService : TileService() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    // The system calls onTileRequest then onTileResourcesRequest back to
    // back for one render pass, on the same live service instance — this is
    // resolved once per pass and reused here rather than recomputed for the
    // resources call too.
    @Volatile private var lastResolvedActions: List<MultiTileAction>? = null

    override fun onTileRequest(requestParams: TileRequest): ListenableFuture<Tile> = serviceScope.future {
        val actions = resolveDisplayedActionsFast()
        lastResolvedActions = actions
        enqueueGridTileRefresh(applicationContext)

        val clickedIndex = requestParams.currentState.lastClickableId
            .removePrefix(CLICK_ID_PREFIX)
            .toIntOrNull()
        val clicked = clickedIndex?.let { actions.getOrNull(it) }
        if (clicked != null) {
            val alreadyArmed = loadArmedGridAction(applicationContext) == clickedIndex
            if (clicked.action.confirm && !alreadyArmed) {
                // First tap on a confirm-required action — arm it and
                // stop here, don't run it yet.
                armGridAction(applicationContext, clickedIndex!!)
            } else {
                clearArmedGridAction(applicationContext)
                val endpoint = clicked.action.endpoint
                val method = clicked.action.method
                if (endpoint != null && method != null) {
                    sendBroadcast(
                        Intent(applicationContext, TileActionReceiver::class.java).apply {
                            action = TileActionReceiver.ACTION_EXECUTE
                            putExtra(TileActionReceiver.EXTRA_ENDPOINT, endpoint)
                            putExtra(TileActionReceiver.EXTRA_METHOD, method)
                            putExtra(TileActionReceiver.EXTRA_TRACK_STATUS_TILE_ID, TileActionReceiver.NO_TILE_ID)
                            putExtra(TileActionReceiver.EXTRA_TRACK_GRID_INDEX, clickedIndex)
                        },
                    )
                }
            }
        }

        val armedIndex = loadArmedGridAction(applicationContext)
        val gridStatus = loadGridActionStatus(applicationContext)
        val accentColor = loadCachedTileGridSelection(applicationContext)?.accentColor ?: TILE_ACCENT_COLORS.first()
        val tileBuilder = Tile.Builder()
            .setResourcesVersion(resourcesVersionFor(actions))
            .setTileTimeline(
                Timeline.fromLayoutElement(
                    layout(requestParams.deviceConfiguration, actions, accentColor, armedIndex, gridStatus),
                ),
            )
        // Same reasoning as ActionTileService's status refresh hint — a
        // best-effort nudge so an armed icon or a just-completed result
        // reverts to its normal color even if the user never taps the tile
        // again before its own window (GRID_ARM_TIMEOUT_MS /
        // GRID_STATUS_HOLD_MS) elapses.
        if (armedIndex != null || gridStatus != null) {
            tileBuilder.setFreshnessIntervalMillis(minOf(GRID_ARM_TIMEOUT_MS, GRID_STATUS_HOLD_MS))
        }
        tileBuilder.build()
    }

    override fun onTileResourcesRequest(requestParams: ResourcesRequest): ListenableFuture<Resources> =
        serviceScope.future {
            val actions = lastResolvedActions ?: resolveDisplayedActionsFast()
            val builder = Resources.Builder().setVersion(resourcesVersionFor(actions))
            actions.forEachIndexed { index, item ->
                builder.addIdToImageMapping(
                    "$RES_ID_PREFIX$index",
                    resolvedIconFor(applicationContext, item.action.icon).toInlineImageResource(ICON_SIZE_PX),
                )
            }
            builder.build()
        }

    // See ActionTileService's identical helper: the renderer only refetches
    // resources when this string changes, and the icon set here is fully
    // dynamic (phone-configured selection, the live top-N fallback, and any
    // icon override — see IconOverrideStore), so a hardcoded constant would
    // risk serving stale icon bitmaps after any of those change.
    private fun resourcesVersionFor(actions: List<MultiTileAction>): String {
        val overrides = IconOverrideStore(applicationContext)
        return actions.joinToString(",") { "${it.serviceName}#${it.action.icon}#${overrides.getOverride(it.action.icon) ?: "-"}" }
            .hashCode()
            .toString()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    // Renders purely from local state — no network call, no Data Layer
    // read — so this is always fast regardless of network conditions or
    // cold-start overhead. Real hardware testing found the system
    // cancelling onTileRequest/onTileResourcesRequest before a live
    // fetch-then-sync-read could complete even once the two were run
    // concurrently: a cold app process alone can eat most of a Tile's
    // response budget before any of that code even runs. See
    // TileActionsCache/TileGridSync for what keeps this local state fresh.
    private fun resolveDisplayedActionsFast(): List<MultiTileAction> {
        val cachedActions = loadCachedGridActions(applicationContext)
            .map { (serviceName, action) -> MultiTileAction(serviceName, action) }
        val selection = loadCachedTileGridSelection(applicationContext)?.actions
        return if (!selection.isNullOrEmpty()) {
            selection.mapNotNull { ref -> cachedActions.find { it.matches(ref) } }.take(MAX_GRID_ACTIONS)
        } else {
            cachedActions.take(FALLBACK_ACTIONS)
        }
    }

    private fun MultiTileAction.matches(ref: TileGridActionRef) =
        serviceName == ref.serviceName && action.endpoint == ref.endpoint

    private fun layout(
        deviceParams: DeviceParameters,
        actions: List<MultiTileAction>,
        accentColor: Int,
        armedIndex: Int?,
        gridStatus: Pair<Int, TileActionStatus>?,
    ): LayoutElement {
        if (actions.isEmpty()) {
            return Box.Builder()
                .setWidth(expand())
                .setHeight(expand())
                .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
                .addContent(
                    Text.Builder(this, "No actions configured")
                        .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                        .setColor(argb(TileColors.onSurface))
                        .setMaxLines(2)
                        .setMultilineAlignment(HORIZONTAL_ALIGN_CENTER)
                        .build(),
                )
                .build()
        }

        val buttonColors = ButtonColors(accentColor, TileColors.onAccent)
        val armedButtonColors = ButtonColors(TileColors.warning, TileColors.onAccent)
        val successButtonColors = ButtonColors(TileColors.success, TileColors.onAccent)
        val errorButtonColors = ButtonColors(TileColors.error, TileColors.onAccent)
        val grid = Column.Builder().setWidth(wrap()).setHeight(wrap()).setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
        actions.chunked(GRID_COLUMNS).forEachIndexed { rowIndex, rowItems ->
            if (rowIndex > 0) grid.addContent(Spacer.Builder().setHeight(dp(8f)).build())

            val row = Row.Builder().setVerticalAlignment(VERTICAL_ALIGN_CENTER)
            rowItems.forEachIndexed { columnIndex, item ->
                if (columnIndex > 0) row.addContent(Spacer.Builder().setWidth(dp(8f)).build())
                val index = rowIndex * GRID_COLUMNS + columnIndex
                val isArmed = index == armedIndex
                val resultStatus = gridStatus?.takeIf { it.first == index }?.second
                val colors = when {
                    resultStatus == TileActionStatus.Success -> successButtonColors
                    resultStatus == TileActionStatus.Error -> errorButtonColors
                    isArmed -> armedButtonColors
                    else -> buttonColors
                }
                val description = when {
                    resultStatus == TileActionStatus.Success -> "${item.action.label} — done"
                    resultStatus == TileActionStatus.Error -> "${item.action.label} — failed"
                    isArmed -> "${item.action.label} — tap again to confirm"
                    else -> item.action.label
                }
                val clickable = Clickable.Builder()
                    .setId("$CLICK_ID_PREFIX$index")
                    .setOnClick(ActionBuilders.LoadAction.Builder().build())
                    .build()
                row.addContent(
                    Button.Builder(this, clickable)
                        .setSize(ButtonDefaults.DEFAULT_SIZE)
                        .setButtonColors(colors)
                        .setIconContent("$RES_ID_PREFIX$index", ButtonDefaults.recommendedIconSize(ButtonDefaults.DEFAULT_SIZE))
                        .setContentDescription(description)
                        .build(),
                )
            }
            grid.addContent(row.build())
        }

        return Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
            .addContent(grid.build())
            .build()
    }
}
