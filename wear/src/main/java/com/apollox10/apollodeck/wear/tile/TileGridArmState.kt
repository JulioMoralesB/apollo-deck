package com.apollox10.apollodeck.wear.tile

import android.content.Context

private const val PREFS_NAME = "apollo_deck_tiles"
private const val KEY_ARMED_INDEX = "grid_armed_index"
private const val KEY_ARMED_AT = "grid_armed_at"
// How long a tap-armed confirm action stays armed before silently
// resetting — long enough for a genuine follow-up tap, short enough that
// an old arm doesn't linger and fire on an unrelated later tap.
const val GRID_ARM_TIMEOUT_MS = 4_000L

// A Tile's Clickable can't distinguish a long-press from a tap the way
// Compose's combinedClickable can (the mechanism ActionsScreen.kt and the
// widget use for confirm: true actions elsewhere in this app) — there's no
// gesture to hang a "hold to confirm" off of. This is the tile-native
// substitute: the first tap on a confirm-required icon arms it (rendered
// in a warning color instead of firing) and a second tap on the *same*
// icon within GRID_ARM_TIMEOUT_MS actually runs it. Global, not per-tileId
// — MultiActionTileService is a single-instance tile, unlike
// ActionTileService.
fun armGridAction(context: Context, index: Int) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putInt(KEY_ARMED_INDEX, index)
        .putLong(KEY_ARMED_AT, System.currentTimeMillis())
        .apply()
}

fun clearArmedGridAction(context: Context) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .remove(KEY_ARMED_INDEX)
        .remove(KEY_ARMED_AT)
        .apply()
}

// Self-clearing on read once stale — same reasoning as ActionTileService's
// status backstop: don't depend on some other trigger to notice the arm
// window passed.
fun loadArmedGridAction(context: Context): Int? {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    if (!prefs.contains(KEY_ARMED_INDEX)) return null
    val armedAt = prefs.getLong(KEY_ARMED_AT, 0L)
    if (System.currentTimeMillis() - armedAt > GRID_ARM_TIMEOUT_MS) {
        clearArmedGridAction(context)
        return null
    }
    return prefs.getInt(KEY_ARMED_INDEX, -1).takeIf { it >= 0 }
}
