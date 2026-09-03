package com.apollox10.apollodeck.wear.tile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.wear.compose.material.MaterialTheme

// Launched from ActionTileService's unconfigured "Tap to configure" state
// (a LaunchAction, since a Tile's Clickable can't open anything but an
// Activity or fire a LoadAction on itself). The triggering tile's tileId
// rides along as a LaunchAction intent extra — see ActionTileService's
// addKeyToExtraMapping(EXTRA_TILE_ID, ...) — so a second, independently
// pinned instance of this tile configures itself, not the first one.
//
// Manifest note: must be exported="true". The tile renderer that launches
// it runs in a different process/UID (com.samsung.android.wearable.sysui
// on this device); confirmed on real hardware via logcat — "Activity
// constraints not met. Not launching LaunchAction Activity" — when this
// was exported="false".
class TileConfigActivity : ComponentActivity() {

    private var tileId = NO_TILE_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tileId = intent?.getIntExtra(EXTRA_TILE_ID, NO_TILE_ID) ?: NO_TILE_ID

        setContent {
            MaterialTheme {
                TileConfigScreen(onSave = { item, accentColor -> save(item, accentColor) })
            }
        }
    }

    private fun save(item: TileConfigItem, accentColor: Int) {
        val endpoint = item.action.endpoint ?: return
        val method = item.action.method ?: return
        saveTileActionConfig(
            this,
            tileId,
            TileActionConfig(
                serviceName = item.serviceName,
                label = item.action.label,
                endpoint = endpoint,
                method = method,
                confirm = item.action.confirm,
                iconName = item.action.icon,
                accentColor = accentColor,
            ),
        )
        ActionTileService.requestUpdate(this)
        finish()
    }

    companion object {
        const val EXTRA_TILE_ID = "tile_id"
        const val NO_TILE_ID = 0
    }
}
