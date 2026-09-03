package com.apollox10.apollodeck.core.sync

import com.apollox10.apollodeck.core.tile.TILE_ACCENT_COLORS
import kotlinx.serialization.Serializable

// Wire format for the watch's multi-action grid tile: which actions to show
// and in what order, chosen from the phone app (typing/scrolling a long
// action list on a watch is painful — same reasoning as SessionTransfer)
// and synced over the Wear Data Layer the same way a session is: the phone
// (app/.../wearsync/TileGridPublisher) writes this as a synced DataItem
// whenever the selection is saved; the watch
// (wear/.../tile/TileGridSync) reads whatever is currently synced.
//
// Only an identifier per action, not a full snapshot (label/icon/confirm) —
// the watch always re-fetches live services to build its grid and uses this
// purely as a filter + display order, so a label or icon changed on the
// backend since the phone last synced still shows up correctly.
@Serializable
data class TileGridActionRef(
    val serviceName: String,
    val endpoint: String,
)

@Serializable
data class TileGridSelection(
    val actions: List<TileGridActionRef>,
    // Applied uniformly to every icon in the grid — chosen alongside the
    // action list in the same phone screen (TileGridConfigScreen). Defaults
    // to TILE_ACCENT_COLORS' first entry so a selection saved before this
    // field existed keeps rendering the same way.
    val accentColor: Int = TILE_ACCENT_COLORS.first(),
)

object TileGridSyncPaths {
    const val TILE_GRID_DATA_PATH = "/apollo-deck/tile-grid"
}
