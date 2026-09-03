package com.apollox10.apollodeck.core.sync

import kotlinx.serialization.Serializable

// Wire format for syncing icon overrides (see
// core/.../store/IconOverrideStore.kt) from the phone, where they're set,
// to the watch, where both tiles need them to render the same icon the
// phone app now shows. Same synced-DataItem approach as session sync and
// the grid tile's selection — the phone publishes a full snapshot whenever
// an override changes; the watch applies whatever it last received.
@Serializable
data class IconOverrideSelection(
    val overrides: Map<String, String>,
)

object IconOverrideSyncPaths {
    const val ICON_OVERRIDE_DATA_PATH = "/apollo-deck/icon-overrides"
}
