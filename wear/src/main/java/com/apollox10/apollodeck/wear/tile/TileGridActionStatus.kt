package com.apollox10.apollodeck.wear.tile

import android.content.Context

private const val PREFS_NAME = "apollo_deck_tiles"
private const val KEY_STATUS_INDEX = "grid_status_index"
private const val KEY_STATUS_VALUE = "grid_status_value"
private const val KEY_STATUS_AT = "grid_status_at"
// Matches the phone dashboard's flash duration (ActionPanel.jsx on the web,
// DashboardViewModel.ACTION_STATE_HOLD_MS on the phone) so a tap feels the
// same everywhere.
const val GRID_STATUS_HOLD_MS = 2_000L

// Tracks the result of the single most recently completed grid-tile
// action, by index, so that one icon can flash green/red before
// auto-reverting — see TileActionReceiver's EXTRA_TRACK_GRID_INDEX path.
// Global, not per-tileId: MultiActionTileService is a single-instance
// tile, same as TileGridArmState.
fun setGridActionStatus(context: Context, index: Int, status: TileActionStatus) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putInt(KEY_STATUS_INDEX, index)
        .putString(KEY_STATUS_VALUE, status.name)
        .putLong(KEY_STATUS_AT, System.currentTimeMillis())
        .apply()
}

fun clearGridActionStatus(context: Context) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .remove(KEY_STATUS_INDEX)
        .remove(KEY_STATUS_VALUE)
        .remove(KEY_STATUS_AT)
        .apply()
}

// Null once GRID_STATUS_HOLD_MS has elapsed — self-clearing on read, same
// backstop pattern as TileGridArmState/ActionTileService's own status, so
// this never depends on a background job having run to revert on time.
fun loadGridActionStatus(context: Context): Pair<Int, TileActionStatus>? {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_STATUS_VALUE, null) ?: return null

    val at = prefs.getLong(KEY_STATUS_AT, 0L)
    if (System.currentTimeMillis() - at > GRID_STATUS_HOLD_MS) {
        clearGridActionStatus(context)
        return null
    }

    val index = prefs.getInt(KEY_STATUS_INDEX, -1)
    if (index < 0) return null
    val status = try {
        TileActionStatus.valueOf(raw)
    } catch (e: Exception) {
        return null
    }
    return index to status
}
