package com.apollox10.apollodeck.core.sync

import kotlinx.serialization.Serializable

// Wire format for syncing the confirm-before-running set (see
// core/.../store/ConfirmActionStore.kt) from the phone, where it's chosen,
// to the watch. Same synced-DataItem approach as IconOverrideSelection: the
// phone publishes a full snapshot on every change, the watch applies
// whatever it last received.
@Serializable
data class ConfirmActionSelection(
    val entityIds: Set<String>,
)

object ConfirmActionSyncPaths {
    const val CONFIRM_ACTION_DATA_PATH = "/apollo-deck/confirm-actions"
}
