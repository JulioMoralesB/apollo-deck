package com.apollox10.apollodeck.wear.tile

import android.content.Context
import android.content.Intent
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.material.ChipColors
import androidx.wear.protolayout.material.CompactChip
import androidx.wear.protolayout.material.Colors
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.guava.future

private const val RESOURCES_VERSION = "1"
private const val CLICK_ID_EXECUTE = "execute"
private const val CLICK_ID_CONFIGURE = "configure"

// One pinned action, tap to run it — the Tile equivalent of the phone's
// home-screen widget. A Tile's Clickable can only launch an Activity or
// fire a LoadAction (re-request this tile); there's no PendingIntent-to-
// arbitrary-broadcast the way RemoteViews has, so "run the action" is
// modeled as a LoadAction whose id (read back via
// requestParams.currentState.lastClickableId) this service recognizes and
// reacts to by firing the broadcast itself before rendering the response.
class ActionTileService : TileService() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    override fun onTileRequest(requestParams: TileRequest): ListenableFuture<Tile> = serviceScope.future {
        val config = loadTileActionConfig(applicationContext)

        if (config != null && requestParams.currentState.lastClickableId == CLICK_ID_EXECUTE) {
            sendBroadcast(
                Intent(applicationContext, TileActionReceiver::class.java).apply {
                    action = TileActionReceiver.ACTION_EXECUTE
                    putExtra(TileActionReceiver.EXTRA_ENDPOINT, config.endpoint)
                    putExtra(TileActionReceiver.EXTRA_METHOD, config.method)
                    putExtra(TileActionReceiver.EXTRA_TRACK_STATUS, true)
                },
            )
        }

        val status = loadTileActionStatus(applicationContext)
        Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(Timeline.fromLayoutElement(layout(requestParams.deviceConfiguration, config, status)))
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

    private fun layout(
        deviceParams: DeviceParameters,
        config: TileActionConfig?,
        status: TileActionStatus?,
    ): LayoutElement {
        if (config == null) {
            val configureClickable = Clickable.Builder()
                .setId(CLICK_ID_CONFIGURE)
                .setOnClick(
                    ActionBuilders.LaunchAction.Builder()
                        .setAndroidActivity(
                            ActionBuilders.AndroidActivity.Builder()
                                .setPackageName(packageName)
                                .setClassName(TileConfigActivity::class.java.name)
                                .build(),
                        )
                        .build(),
                )
                .build()
            return PrimaryLayout.Builder(deviceParams)
                .setContent(
                    Text.Builder(this, "Tap to configure")
                        .setTypography(Typography.TYPOGRAPHY_BODY1)
                        .setColor(argb(TileColors.onSurface))
                        .build(),
                )
                .setPrimaryChipContent(
                    CompactChip.Builder(this, "Configure", configureClickable, deviceParams)
                        .setChipColors(ChipColors.primaryChipColors(TileColors.theme))
                        .build(),
                )
                .build()
        }

        val (label, chipColors) = when (status) {
            TileActionStatus.Success -> "Done" to ChipColors.primaryChipColors(TileColors.successTheme)
            TileActionStatus.Error -> "Failed" to ChipColors.primaryChipColors(TileColors.errorTheme)
            null -> "Run" to ChipColors.primaryChipColors(TileColors.theme)
        }

        val executeClickable = Clickable.Builder()
            .setId(CLICK_ID_EXECUTE)
            .setOnClick(ActionBuilders.LoadAction.Builder().build())
            .build()

        return PrimaryLayout.Builder(deviceParams)
            .setPrimaryLabelTextContent(
                Text.Builder(this, config.label)
                    .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                    .setColor(argb(TileColors.primary))
                    .setMaxLines(2)
                    .build(),
            )
            .setContent(
                Text.Builder(this, config.serviceName)
                    .setTypography(Typography.TYPOGRAPHY_BODY1)
                    .setColor(argb(TileColors.onSurface))
                    .build(),
            )
            .setPrimaryChipContent(
                CompactChip.Builder(this, label, executeClickable, deviceParams)
                    .setChipColors(chipColors)
                    .build(),
            )
            .build()
    }

    companion object {
        fun requestUpdate(context: Context) {
            getUpdater(context).requestUpdate(ActionTileService::class.java)
        }
    }
}

// A small, self-contained palette for tile layouts — not tied to the phone
// app's Compose Material3 theme (different toolkit, different module) or
// the widget's WidgetAccent set (rasterizes icons + drawable resources,
// neither of which the plain-text tile layouts here need).
internal object TileColors {
    const val primary = 0xFF5B9BFF.toInt()
    const val onSurface = 0xFFEEF0FA.toInt()
    private const val surface = 0xFF1E2124.toInt()
    private const val onPrimary = 0xFF0B1220.toInt()
    private const val successColor = 0xFF3DDC84.toInt()
    private const val errorColor = 0xFFFF5C5C.toInt()

    val theme = Colors(primary, onPrimary, surface, onSurface)
    val successTheme = Colors(successColor, onPrimary, surface, onSurface)
    val errorTheme = Colors(errorColor, onPrimary, surface, onSurface)
}
