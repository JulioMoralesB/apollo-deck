package com.apollox10.apollodeck.core.store

import android.content.Context

private const val PREFS_NAME = "apollo_deck_confirm_actions"
private const val KEY_ENTITY_IDS = "entity_ids"

// Which Home Assistant scripts need a confirmation step before running.
// HA has no such flag on a script, so it's chosen in the phone app
// (Settings → "Confirm before running") and synced to the watch, the same
// way icon overrides are. ApolloDeckClient.getServices() folds it into each
// Action's `confirm`, so every surface — dashboard dialog, widget ⚠,
// long-press/arm-then-tap on the tiles — honors the same choice.
//
// Plain SharedPreferences: entity ids, nothing sensitive.
class ConfirmActionStore(private val context: Context) {
    private val prefs get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAll(): Set<String> = prefs.getStringSet(KEY_ENTITY_IDS, emptySet()).orEmpty().toSet()

    fun isRequired(entityId: String): Boolean = entityId in getAll()

    fun setRequired(entityId: String, required: Boolean) {
        replaceAll(if (required) getAll() + entityId else getAll() - entityId)
    }

    fun replaceAll(entityIds: Set<String>) {
        prefs.edit().putStringSet(KEY_ENTITY_IDS, entityIds).apply()
    }
}
