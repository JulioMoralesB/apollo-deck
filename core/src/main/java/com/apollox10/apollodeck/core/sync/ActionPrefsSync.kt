package com.apollox10.apollodeck.core.sync

import kotlinx.serialization.Serializable

// Wire format for syncing action preferences (see
// core/.../store/ActionPrefsStore.kt) from the phone, where they're chosen,
// to the watch. Same synced-DataItem approach as IconOverrideSelection: the
// phone publishes a full snapshot on every change, the watch applies
// whatever it last received.
@Serializable
data class ActionPrefsSelection(
    val order: List<String>,
    val confirmRequired: Set<String>,
)

object ActionPrefsSyncPaths {
    const val ACTION_PREFS_DATA_PATH = "/apollo-deck/action-prefs"
}
