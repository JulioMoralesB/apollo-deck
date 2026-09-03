package com.apollox10.apollodeck.wear.tile

import android.content.Context
import android.content.Intent
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.wrap
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.ColorFilter
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.Image
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.LayoutElementBuilders.VERTICAL_ALIGN_CENTER
import androidx.wear.protolayout.ModifiersBuilders.Background
import androidx.wear.protolayout.ModifiersBuilders.Border
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ModifiersBuilders.Corner
import androidx.wear.protolayout.ModifiersBuilders.Modifiers
import androidx.wear.protolayout.ModifiersBuilders.Semantics
import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
import com.apollox10.apollodeck.core.store.IconOverrideStore
import com.apollox10.apollodeck.wear.icons.iconFor
import com.apollox10.apollodeck.wear.icons.resolvedIconFor
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.guava.future

private const val CLICK_ID_EXECUTE = "execute"
private const val CLICK_ID_CONFIGURE = "configure"
// The platform is free to coarsen this (typically to a floor around a
// minute) — it's a hint, not a guarantee, which is why the age-based
// backstop in loadTileActionStatus is the thing actually responsible for
// this tile never getting stuck on a stale result.
private const val STATUS_REFRESH_INTERVAL_MS = 15_000L
private const val RES_ID_ACTION_ICON = "action_icon"
private const val RES_ID_CONFIGURE_ICON = "configure_icon"
private const val ICON_SIZE_PX = 72
private const val EXECUTE_CIRCLE_SIZE_DP = 76f
private const val CONFIGURE_CIRCLE_SIZE_DP = 64f
private const val RECONFIGURE_CIRCLE_SIZE_DP = 28f
private const val ICON_TO_CIRCLE_RATIO = 0.55f
private const val RING_WIDTH_DP = 2f

// One pinned action, tap to run it — the Tile equivalent of the phone's
// home-screen widget. Can be pinned more than once: each instance is keyed
// by its own tileId (see TileActionConfig) so multiple pins each control a
// different action, the same way the phone widget is per-appWidgetId.
//
// A Tile's Clickable can only launch an Activity or fire a LoadAction
// (re-request this tile); there's no PendingIntent-to-arbitrary-broadcast
// the way RemoteViews has, so "run the action" is modeled as a LoadAction
// whose id (read back via requestParams.currentState.lastClickableId) this
// service recognizes and reacts to by firing the broadcast itself before
// rendering the response.
class ActionTileService : TileService() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    override fun onTileRequest(requestParams: TileRequest): ListenableFuture<Tile> = serviceScope.future {
        val tileId = requestParams.tileId
        val config = loadTileActionConfig(applicationContext, tileId)

        if (config != null && requestParams.currentState.lastClickableId == CLICK_ID_EXECUTE) {
            sendBroadcast(
                Intent(applicationContext, TileActionReceiver::class.java).apply {
                    action = TileActionReceiver.ACTION_EXECUTE
                    putExtra(TileActionReceiver.EXTRA_ENDPOINT, config.endpoint)
                    putExtra(TileActionReceiver.EXTRA_METHOD, config.method)
                    putExtra(TileActionReceiver.EXTRA_TRACK_STATUS_TILE_ID, tileId)
                },
            )
        }

        val status = loadTileActionStatus(applicationContext, tileId)
        val tileBuilder = Tile.Builder()
            .setResourcesVersion(resourcesVersionFor(config))
            .setTileTimeline(Timeline.fromLayoutElement(layout(requestParams.deviceConfiguration, config, status, tileId)))
        // Only while showing a transient result — nudges the system to ask
        // for this tile again around when loadTileActionStatus's own
        // staleness backstop would revert it, so a tile sitting unattended
        // on the carousel doesn't need to wait for a manual glance to self-
        // heal. Not set at all otherwise: an idle tile has no need for
        // (battery-costing) periodic refresh.
        if (status != null) {
            tileBuilder.setFreshnessIntervalMillis(STATUS_REFRESH_INTERVAL_MS)
        }
        tileBuilder.build()
    }

    override fun onTileResourcesRequest(requestParams: ResourcesRequest): ListenableFuture<Resources> =
        serviceScope.future {
            val config = loadTileActionConfig(applicationContext, requestParams.tileId)
            val builder = Resources.Builder()
                .setVersion(resourcesVersionFor(config))
                .addIdToImageMapping(RES_ID_CONFIGURE_ICON, iconFor("settings").toInlineImageResource(ICON_SIZE_PX))
            if (config != null) {
                builder.addIdToImageMapping(
                    RES_ID_ACTION_ICON,
                    resolvedIconFor(applicationContext, config.iconName).toInlineImageResource(ICON_SIZE_PX),
                )
            }
            builder.build()
        }

    // The renderer only re-invokes onTileResourcesRequest when this string
    // changes from what it already has cached — a constant version would
    // let it keep serving a stale action_icon bitmap after reconfiguring to
    // a different action with a different icon (or across the
    // unconfigured-to-configured transition, when the resource id set
    // itself changes). Deriving it from the config's icon keeps it in sync
    // without needing to bump a hardcoded version by hand — and since
    // resolvedIconFor can render something other than iconName's own
    // mapping when an override exists (see IconOverrideStore), the override
    // (if any) has to be part of this string too, or changing/clearing one
    // wouldn't force a re-render of an already-configured tile.
    private fun resourcesVersionFor(config: TileActionConfig?): String =
        config?.let {
            val overrideId = IconOverrideStore(applicationContext).getOverride(it.iconName)
            "action:${it.iconName}:${overrideId ?: "-"}"
        } ?: "unconfigured"

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    private fun layout(
        deviceParams: DeviceParameters,
        config: TileActionConfig?,
        status: TileActionStatus?,
        tileId: Int,
    ): LayoutElement {
        if (config == null) {
            return centered(
                iconCircle(
                    resourceId = RES_ID_CONFIGURE_ICON,
                    clickable = configureClickable(tileId),
                    tintColor = TileColors.onSurface,
                    contentDescription = "Configure",
                    sizeDp = CONFIGURE_CIRCLE_SIZE_DP,
                ),
                "Tap to configure",
            )
        }

        val tintColor = when (status) {
            TileActionStatus.Success -> TileColors.success
            TileActionStatus.Error -> TileColors.error
            null -> config.accentColor
        }

        val executeClickable = Clickable.Builder()
            .setId(CLICK_ID_EXECUTE)
            .setOnClick(ActionBuilders.LoadAction.Builder().build())
            .build()

        // A second, smaller tap target under the label to change the pinned
        // action/color later — once configured, the big circle only runs
        // the action (a LoadAction re-requesting the tile), so without this
        // there'd be no way back into TileConfigActivity short of
        // unpinning and re-pinning the tile. Unlike the phone widget, a
        // Tile has no OS-level "long-press to reconfigure" gesture to hang
        // this off of.
        return centered(
            icon = iconCircle(
                resourceId = RES_ID_ACTION_ICON,
                clickable = executeClickable,
                tintColor = tintColor,
                contentDescription = config.label,
                sizeDp = EXECUTE_CIRCLE_SIZE_DP,
            ),
            label = config.label,
            footer = iconCircle(
                resourceId = RES_ID_CONFIGURE_ICON,
                clickable = configureClickable(tileId),
                tintColor = TileColors.onSurface,
                contentDescription = "Change action",
                sizeDp = RECONFIGURE_CIRCLE_SIZE_DP,
            ),
        )
    }

    private fun configureClickable(tileId: Int): Clickable =
        Clickable.Builder()
            .setId(CLICK_ID_CONFIGURE)
            .setOnClick(
                ActionBuilders.LaunchAction.Builder()
                    .setAndroidActivity(
                        ActionBuilders.AndroidActivity.Builder()
                            .setPackageName(packageName)
                            .setClassName(TileConfigActivity::class.java.name)
                            .addKeyToExtraMapping(
                                TileConfigActivity.EXTRA_TILE_ID,
                                ActionBuilders.AndroidIntExtra.Builder().setValue(tileId).build(),
                            )
                            .build(),
                    )
                    .build(),
            )
            .build()

    // Icon only, no filled background — just the icon (tinted to
    // [tintColor]) and a thin ring in the same color. Hand-built rather
    // than the protolayout-material Button component: Button always fills
    // its background from ButtonColors and offers no way to opt out of
    // that, but the requested look here is closer to an outlined toggle
    // than a filled button.
    private fun iconCircle(
        resourceId: String,
        clickable: Clickable,
        tintColor: Int,
        contentDescription: String,
        sizeDp: Float,
    ): LayoutElement {
        val iconSize = dp(sizeDp * ICON_TO_CIRCLE_RATIO)
        return Box.Builder()
            .setWidth(dp(sizeDp))
            .setHeight(dp(sizeDp))
            .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
            .setModifiers(
                Modifiers.Builder()
                    .setClickable(clickable)
                    .setSemantics(Semantics.Builder().setContentDescription(contentDescription).build())
                    .setBackground(
                        // No fill (fully transparent) — set purely so the
                        // tap ripple is masked to a circle instead of the
                        // Box's square bounds.
                        Background.Builder()
                            .setColor(argb(0x00000000))
                            .setCorner(Corner.Builder().setRadius(dp(sizeDp / 2f)).build())
                            .build(),
                    )
                    .setBorder(Border.Builder().setWidth(dp(RING_WIDTH_DP)).setColor(argb(tintColor)).build())
                    .build(),
            )
            .addContent(
                Image.Builder()
                    .setResourceId(resourceId)
                    .setWidth(iconSize)
                    .setHeight(iconSize)
                    .setColorFilter(ColorFilter.Builder().setTint(argb(tintColor)).build())
                    .build(),
            )
            .build()
    }

    // The icon circle with its label underneath, centered on the round
    // face — the same shape as the phone's home-screen widget. An optional
    // smaller footer element (the reconfigure affordance) stacks below the
    // label.
    private fun centered(icon: LayoutElement, label: String, footer: LayoutElement? = null): LayoutElement =
        Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
            .addContent(
                Column.Builder()
                    .setWidth(wrap())
                    .setHeight(wrap())
                    .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                    .addContent(icon)
                    .addContent(Spacer.Builder().setHeight(dp(8f)).build())
                    .addContent(
                        Text.Builder(this, label)
                            .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                            .setColor(argb(TileColors.onSurface))
                            .setMaxLines(2)
                            .setMultilineAlignment(HORIZONTAL_ALIGN_CENTER)
                            .build(),
                    )
                    .apply {
                        if (footer != null) {
                            addContent(Spacer.Builder().setHeight(dp(6f)).build())
                            addContent(footer)
                        }
                    }
                    .build(),
            )
            .build()

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
    const val success = 0xFF3DDC84.toInt()
    const val error = 0xFFFF5C5C.toInt()
    // The dark content color paired with any accent background — used by
    // MultiActionTileService's grid buttons, whose background color is the
    // phone-configured accent (see TileGridSelection), not a fixed one.
    const val onAccent = 0xFF0B1220.toInt()
}
