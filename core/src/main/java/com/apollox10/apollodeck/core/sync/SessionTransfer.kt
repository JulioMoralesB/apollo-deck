package com.apollox10.apollodeck.core.sync

import kotlinx.serialization.Serializable

// Wire format for handing a logged-in session from the phone to the watch
// over the Wear Data Layer (DataClient), so the watch never needs the user
// to type server URL + username + password on a watch keyboard. The phone
// (PhoneSessionPublisher) writes this as a synced DataItem whenever it logs
// in/out; the watch (PhoneSessionSync) reads whatever is currently synced —
// a plain MessageClient RPC was tried first and measured unreliable on real
// hardware (Play Services logged "Failed to deliver message to AppKey" even
// with both apps foregrounded and the nodes actively paired), so this reads
// the already-synced replica instead of depending on the phone being
// reachable at the exact moment of the request.
@Serializable
data class SessionTransfer(
    val serverUrl: String,
    val accessToken: String,
    val refreshToken: String,
    val cloudflareAccessClientId: String? = null,
    val cloudflareAccessClientSecret: String? = null,
)

object SessionTransferPaths {
    const val SESSION_DATA_PATH = "/apollo-deck/session"
}
