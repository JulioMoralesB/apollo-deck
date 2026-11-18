package com.apollox10.apollodeck.wear.tile

import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ModifiersBuilders.Modifiers
import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.apollox10.apollodeck.core.model.EnergySummary
import com.apollox10.apollodeck.core.model.ServiceSummary
import com.apollox10.apollodeck.core.model.formatCost
import com.apollox10.apollodeck.core.model.formatKwh
import com.apollox10.apollodeck.core.model.formatWatts
import com.apollox10.apollodeck.core.model.parseServiceSummary
import com.apollox10.apollodeck.core.net.ApiClient
import com.apollox10.apollodeck.core.store.TokenStore
import com.apollox10.apollodeck.wear.BuildConfig
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.guava.future

private const val PREFS_NAME = "apollo_deck_energy_tile"
private const val KEY_SUMMARY = "summary"
private const val KEY_LAST_REFRESH_AT = "last_refresh_at"
private const val MIN_REFRESH_INTERVAL_MS = 60_000L
// HA's energy sensor itself only updates every 5 min; asking the renderer
// for more often than this would only cost battery.
private const val FRESHNESS_INTERVAL_MS = 15 * 60_000L
private const val UNIQUE_WORK_NAME = "energy_tile_refresh"
private const val CLICK_ID_REFRESH = "refresh"
private const val RESOURCES_VERSION = "energy-1"

// Household power at a glance: total watts now and today's kWh, from Home
// Assistant's energy summary sensor. Same render-path rule as the other
// tiles — onTileRequest only reads the local cache; EnergyTileRefreshWorker
// does the network fetch and calls requestUpdate() when it has new data.
// Tapping the tile asks for a fresh fetch.
class EnergyTileService : TileService() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    override fun onTileRequest(requestParams: TileRequest): ListenableFuture<Tile> = serviceScope.future {
        enqueueEnergyTileRefresh(applicationContext, force = requestParams.currentState.lastClickableId == CLICK_ID_REFRESH)
        Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setFreshnessIntervalMillis(FRESHNESS_INTERVAL_MS)
            .setTileTimeline(Timeline.fromLayoutElement(layout(loadCachedEnergy(applicationContext))))
            .build()
    }

    override fun onTileResourcesRequest(requestParams: ResourcesRequest): ListenableFuture<Resources> =
        serviceScope.future { Resources.Builder().setVersion(RESOURCES_VERSION).build() }

    private fun layout(energy: EnergySummary?): LayoutElement {
        val column = Column.Builder().setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
        column.addContent(text("Energy", Typography.TYPOGRAPHY_CAPTION1, TileColors.onSurface))
        column.addContent(Spacer.Builder().setHeight(dp(4f)).build())
        when {
            !TokenStore(applicationContext).isLoggedIn() ->
                column.addContent(text("Sign in on the watch app", Typography.TYPOGRAPHY_CAPTION2, TileColors.onSurface))
            energy == null ->
                column.addContent(text("Loading…", Typography.TYPOGRAPHY_CAPTION2, TileColors.onSurface))
            else -> {
                column.addContent(text(formatWatts(energy.powerW), Typography.TYPOGRAPHY_DISPLAY3, TileColors.primary))
                energy.todayKwh?.let { kwh ->
                    val cost = energy.costOf(kwh)?.let { " · ${formatCost(it)}" }.orEmpty()
                    column.addContent(Spacer.Builder().setHeight(dp(2f)).build())
                    column.addContent(text("${formatKwh(kwh)}$cost", Typography.TYPOGRAPHY_CAPTION1, TileColors.onSurface))
                }
                energy.last60DaysKwh?.let { kwh -> energy.costOf(kwh) }?.let { cost ->
                    column.addContent(Spacer.Builder().setHeight(dp(4f)).build())
                    column.addContent(text("60 d · ${formatCost(cost)}", Typography.TYPOGRAPHY_CAPTION2, TileColors.warning))
                }
            }
        }
        return Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setModifiers(
                Modifiers.Builder()
                    .setClickable(
                        Clickable.Builder()
                            .setId(CLICK_ID_REFRESH)
                            .setOnClick(ActionBuilders.LoadAction.Builder().build())
                            .build(),
                    )
                    .build(),
            )
            .addContent(column.build())
            .build()
    }

    private fun text(value: String, typography: Int, color: Int): LayoutElement =
        Text.Builder(this, value)
            .setTypography(typography)
            .setColor(argb(color))
            .build()

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}

// Finds the energy summary by shape (like everything else here, not by
// entity name) among HA's summary sensors and caches its raw JSON.
class EnergyTileRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!TokenStore(applicationContext).isLoggedIn()) return Result.success()
        try {
            val client = ApiClient.create(applicationContext, debugLogging = BuildConfig.DEBUG)
            for (service in client.getServices()) {
                val entityId = service.summaryEntityId ?: continue
                val raw = client.fetchSummary(entityId).getOrNull() ?: continue
                if (parseServiceSummary(raw) is ServiceSummary.Energy) {
                    prefs(applicationContext).edit().putString(KEY_SUMMARY, raw).apply()
                    break
                }
            }
        } catch (e: Exception) {
            // Keep whatever's cached — a slightly old reading beats a blank tile.
        }
        TileService.getUpdater(applicationContext).requestUpdate(EnergyTileService::class.java)
        return Result.success()
    }
}

private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

private fun loadCachedEnergy(context: Context): EnergySummary? {
    val raw = prefs(context).getString(KEY_SUMMARY, null) ?: return null
    return (parseServiceSummary(raw) as? ServiceSummary.Energy)?.data
}

// Debounced like enqueueGridTileRefresh: every render enqueues, but at most
// one fetch a minute unless the user tapped for one.
private fun enqueueEnergyTileRefresh(context: Context, force: Boolean) {
    val now = System.currentTimeMillis()
    if (!force && now - prefs(context).getLong(KEY_LAST_REFRESH_AT, 0L) < MIN_REFRESH_INTERVAL_MS) return
    prefs(context).edit().putLong(KEY_LAST_REFRESH_AT, now).apply()
    WorkManager.getInstance(context).enqueueUniqueWork(
        UNIQUE_WORK_NAME,
        ExistingWorkPolicy.KEEP,
        OneTimeWorkRequestBuilder<EnergyTileRefreshWorker>().build(),
    )
}
