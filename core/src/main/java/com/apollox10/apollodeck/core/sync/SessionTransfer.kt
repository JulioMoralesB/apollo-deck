package com.apollox10.apollodeck.core.sync

import kotlinx.serialization.Serializable

// Wire format for handing a logged-in session from the phone to the watch
// over the Wear Data Layer (DataClient), so the watch never needs the user
// to type a Home Assistant URL + token on a watch keyboard. The phone
// (PhoneSessionPublisher) writes this as a synced DataItem whenever it logs
// in/out; the watch (PhoneSessionSync) reads whatever is currently synced —
// a plain MessageClient RPC was tried first and measured unreliable on real
// hardware (Play Services logged "Failed to deliver message to AppKey" even
// with both apps foregrounded and the nodes actively paired), so this reads
// the already-synced replica instead of depending on the phone being
// reachable at the exact moment of the request.
//
// Version 1 carried the old dashboard's JWT pair; a watch reading one of
// those (or a phone app too old to publish this) treats it as "not logged
// in on the phone" rather than half-applying it.
@Serializable
data class SessionTransfer(
    // Required, not defaulted: kotlinx.serialization omits default-valued
    // fields when encoding, and a v1 payload must fail to decode here.
    val schemaVersion: Int,
    val serverUrl: String,
    val token: String,
)

const val SESSION_SCHEMA_VERSION = 2

object SessionTransferPaths {
    const val SESSION_DATA_PATH = "/apollo-deck/session"
}
