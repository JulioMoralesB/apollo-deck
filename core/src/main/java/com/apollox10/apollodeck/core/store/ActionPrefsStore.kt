package com.apollox10.apollodeck.core.store

import android.content.Context

private const val PREFS_NAME = "apollo_deck_action_prefs"
private const val KEY_ORDER = "order"
private const val KEY_CONFIRM = "confirm"
private const val ORDER_SEPARATOR = "\n"

// Per-script preferences Home Assistant has no field for, chosen in the
// phone app (Settings → "Actions") and synced to the watch the same way
// icon overrides are:
// - the display order of scripts (entity ids). Scripts not in it — e.g. one
//   added in HA since — come after, alphabetically;
// - which scripts need a confirmation step before running.
// ApolloDeckClient.getServices() applies both, but confirm must also be
// read live wherever a tap is handled (see isConfirmRequired's callers),
// since a services list loaded earlier predates any change made since.
//
// Plain SharedPreferences: entity ids, nothing sensitive.
class ActionPrefsStore(private val context: Context) {
    private val prefs get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Newline-joined rather than a StringSet: order matters, and
    // SharedPreferences string sets are unordered.
    fun getOrder(): List<String> =
        prefs.getString(KEY_ORDER, null)?.split(ORDER_SEPARATOR)?.filter { it.isNotBlank() }.orEmpty()

    fun setOrder(entityIds: List<String>) {
        prefs.edit().putString(KEY_ORDER, entityIds.joinToString(ORDER_SEPARATOR)).apply()
    }

    fun getConfirmRequired(): Set<String> = prefs.getStringSet(KEY_CONFIRM, emptySet()).orEmpty().toSet()

    fun isConfirmRequired(entityId: String): Boolean = entityId in getConfirmRequired()

    fun setConfirmRequired(entityId: String, required: Boolean) {
        val current = getConfirmRequired()
        prefs.edit().putStringSet(KEY_CONFIRM, if (required) current + entityId else current - entityId).apply()
    }

    fun replaceAll(order: List<String>, confirmRequired: Set<String>) {
        prefs.edit()
            .putString(KEY_ORDER, order.joinToString(ORDER_SEPARATOR))
            .putStringSet(KEY_CONFIRM, confirmRequired)
            .apply()
    }
}

// Applies a saved order: listed entity ids first, in that order, then the
// rest alphabetically by label.
fun <T> List<T>.sortedByActionOrder(order: List<String>, entityId: (T) -> String, label: (T) -> String): List<T> {
    val rank = order.withIndex().associate { (i, id) -> id to i }
    return sortedWith(compareBy<T> { rank[entityId(it)] ?: Int.MAX_VALUE }.thenBy { label(it).lowercase() })
}
