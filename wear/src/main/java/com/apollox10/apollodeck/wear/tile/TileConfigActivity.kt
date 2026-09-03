package com.apollox10.apollodeck.wear.tile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.wear.compose.material.MaterialTheme

// Launched from ActionTileService's unconfigured "Tap to configure" state
// (a LaunchAction, since a Tile's Clickable can't open anything but an
// Activity or fire a LoadAction on itself). Picking an action saves it and
// finishes; there's no per-tile-instance id to key off the way the phone
// widget's configure Activity has, since this tile isn't tracking
// per-instance state — see TileActionConfig.
//
// Manifest note: must be exported="true". The tile renderer that launches
// it runs in a different process/UID (com.samsung.android.wearable.sysui
// on this device); confirmed on real hardware via logcat — "Activity
// constraints not met. Not launching LaunchAction Activity" — when this
// was exported="false".
class TileConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TileConfigScreen(onSelected = { finish() })
            }
        }
    }
}
