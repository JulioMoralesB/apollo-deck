package com.apollox10.apollodeck.core.tile

// Shared accent palette for both watch tiles — ActionTileService's ring/
// icon tint (chosen on-watch, per pinned instance, see TileActionConfig)
// and MultiActionTileService's grid icon tint (chosen on the phone, see
// TileGridSelection, since the grid's whole selection is already
// phone-configured). Kept here rather than duplicated in wear/ and app/ so
// every configure screen and both tile renderers agree on the same
// options — a plain Int list has no Android UI dependency, so core/ (which
// deliberately avoids those) can still hold it.
val TILE_ACCENT_COLORS: List<Int> = listOf(
    0xFF5B9BFF.toInt(), // blue (default)
    0xFF3DDC84.toInt(), // green
    0xFFFFB74D.toInt(), // amber
    0xFFFF6E6E.toInt(), // red
    0xFFB388FF.toInt(), // purple
    0xFF4DD0E1.toInt(), // teal
    0xFFEEF0FA.toInt(), // near-white
)
