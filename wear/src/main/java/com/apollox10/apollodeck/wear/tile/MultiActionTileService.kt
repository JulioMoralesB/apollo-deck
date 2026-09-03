package com.apollox10.apollodeck.wear.tile

import android.content.Intent
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.material.ChipColors
import androidx.wear.protolayout.material.CompactChip
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
import com.apollox10.apollodeck.core.model.Action
import com.apollox10.apollodeck.core.net.ApiClient
import com.apollox10.apollodeck.wear.BuildConfig
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.withTimeoutOrNull

private const val RESOURCES_VERSION = "1"
private const val MAX_ACTIONS = 4
private const val FETCH_TIMEOUT_MS = 8_000L

private data class MultiTileAction(val serviceName: String, val action: Action)

// Shows up to MAX_ACTIONS executable actions across every service as
// tappable rows, no configuration needed. Tapping one fires it immediately
// (no confirm dialog, no "long-press to confirm" — there's no room for
// either on a tile, same tradeoff the widget made). Unlike
// ActionTileService, taps here aren't tracked/reverted since there's no
// single result slot to show it in; the tap just runs the action, matching
// how fire-and-forget quick-action tiles are in other apps (Home
// Assistant's Wear ShortcutsTile does the same).
class MultiActionTileService : TileService() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    override fun onTileRequest(requestParams: TileRequest): ListenableFuture<Tile> = serviceScope.future {
        val actions = fetchActions()

        val clickedIndex = requestParams.currentState.lastClickableId
            .removePrefix(CLICK_ID_PREFIX)
            .toIntOrNull()
        val clicked = clickedIndex?.let { actions.getOrNull(it) }
        if (clicked != null) {
            val endpoint = clicked.action.endpoint
            val method = clicked.action.method
            if (endpoint != null && method != null) {
                sendBroadcast(
                    Intent(applicationContext, TileActionReceiver::class.java).apply {
                        action = TileActionReceiver.ACTION_EXECUTE
                        putExtra(TileActionReceiver.EXTRA_ENDPOINT, endpoint)
                        putExtra(TileActionReceiver.EXTRA_METHOD, method)
                        putExtra(TileActionReceiver.EXTRA_TRACK_STATUS, false)
                    },
                )
            }
        }

        Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(Timeline.fromLayoutElement(layout(requestParams.deviceConfiguration, actions)))
            .build()
    }

    override fun onTileResourcesRequest(requestParams: ResourcesRequest): ListenableFuture<Resources> =
        serviceScope.future {
            Resources.Builder().setVersion(RESOURCES_VERSION).build()
        }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    private suspend fun fetchActions(): List<MultiTileAction> {
        val actions = withTimeoutOrNull(FETCH_TIMEOUT_MS) {
            try {
                val client = ApiClient.create(applicationContext, debugLogging = BuildConfig.DEBUG)
                client.authenticatedApi.getServices()
                    .flatMap { service ->
                        (service.actions.orEmpty())
                            .filter { it.method != null && it.method != "href" && it.endpoint != null }
                            .map { MultiTileAction(service.name, it) }
                    }
            } catch (e: Exception) {
                null
            }
        }
        return actions.orEmpty().take(MAX_ACTIONS)
    }

    private fun layout(deviceParams: DeviceParameters, actions: List<MultiTileAction>): LayoutElement {
        if (actions.isEmpty()) {
            return PrimaryLayout.Builder(deviceParams)
                .setContent(
                    Text.Builder(this, "No actions available")
                        .setTypography(Typography.TYPOGRAPHY_BODY1)
                        .setColor(argb(TileColors.onSurface))
                        .build(),
                )
                .build()
        }

        val column = Column.Builder()
        actions.forEachIndexed { index, item ->
            if (index > 0) column.addContent(Spacer.Builder().setHeight(dp(4f)).build())
            val clickable = Clickable.Builder()
                .setId("$CLICK_ID_PREFIX$index")
                .setOnClick(ActionBuilders.LoadAction.Builder().build())
                .build()
            column.addContent(
                CompactChip.Builder(this, item.action.label, clickable, deviceParams)
                    .setChipColors(ChipColors.primaryChipColors(TileColors.theme))
                    .build(),
            )
        }
        return column.build()
    }

    private companion object {
        const val CLICK_ID_PREFIX = "action_"
    }
}
