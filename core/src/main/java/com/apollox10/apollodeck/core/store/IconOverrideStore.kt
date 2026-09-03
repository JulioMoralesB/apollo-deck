package com.apollox10.apollodeck.core.store

import android.content.Context

private const val PREFS_NAME = "apollo_deck_icon_overrides"
private const val KEY_PREFIX = "override_"

// Lets a dashboard icon name the app can't map correctly (see
// app/.../ui/icons/IconMapping.kt's iconFor — a hand-maintained dictionary,
// since there's no Lucide distribution for Compose) be pointed at a chosen
// Material icon instead, from inside the app — no code change/redeploy
// needed. Set from the phone (ui/icons/IconOverrideScreen.kt) and synced to
// the watch (wear/.../tile/IconOverrideSync.kt) so every icon-rendering
// site on both platforms — dashboard cards/actions, both tiles — honors
// the same override.
//
// Plain SharedPreferences, not the encrypted store TokenStore/
// ServerConfigStore/CloudflareAccessStore use — these are just icon name
// strings, nothing sensitive, matching the tile stores' convention.
class IconOverrideStore(private val context: Context) {
    private val prefs get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getOverride(iconName: String): String? = prefs.getString(KEY_PREFIX + iconName, null)

    fun getAll(): Map<String, String> =
        prefs.all.entries
            .filter { it.key.startsWith(KEY_PREFIX) }
            .mapNotNull { (key, value) -> (value as? String)?.let { key.removePrefix(KEY_PREFIX) to it } }
            .toMap()

    fun setOverride(iconName: String, chosenIconId: String) {
        prefs.edit().putString(KEY_PREFIX + iconName, chosenIconId).apply()
    }

    fun clearOverride(iconName: String) {
        prefs.edit().remove(KEY_PREFIX + iconName).apply()
    }

    // Bulk-replaces every override — used when applying a synced snapshot
    // from the phone (see IconOverrideSync's pull/listener on the watch).
    fun replaceAll(overrides: Map<String, String>) {
        prefs.edit().apply {
            getAll().keys.forEach { remove(KEY_PREFIX + it) }
            overrides.forEach { (name, id) -> putString(KEY_PREFIX + name, id) }
        }.apply()
    }
}
