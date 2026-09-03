package com.apollox10.apollodeck.wear.sync

import android.content.Context
import android.util.Log
import com.apollox10.apollodeck.core.store.CloudflareAccessCredentials
import com.apollox10.apollodeck.core.store.CloudflareAccessStore
import com.apollox10.apollodeck.core.store.ServerConfigStore
import com.apollox10.apollodeck.core.store.TokenStore
import com.apollox10.apollodeck.core.sync.SessionTransfer
import com.apollox10.apollodeck.core.sync.SessionTransferPaths
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private const val TAG = "PhoneSessionSync"

sealed interface PhoneSyncResult {
    data object Success : PhoneSyncResult
    data object NotLoggedInOnPhone : PhoneSyncResult
    data class Error(val message: String) : PhoneSyncResult
}

// Reads the session a paired phone has published as a synced Wear Data
// Layer DataItem (see the phone's PhoneSessionPublisher) instead of the
// user typing server URL + username + password on a watch keyboard. This
// is a local read against whatever the OS has already synced — it doesn't
// need the phone to be reachable at this exact moment. An earlier version
// asked the phone live over MessageClient; that measured unreliable on
// real hardware (Play Services logged "Failed to deliver message to
// AppKey" even with both apps foregrounded), so this replaced it.
class PhoneSessionSync(private val context: Context) {

    suspend fun requestSessionFromPhone(): PhoneSyncResult {
        return try {
            // No URI filter — fetch every synced item and match by path
            // client-side, rather than relying on wildcard-authority URI
            // matching (unconfirmed against this GMS version and, on real
            // hardware, the one thing that changed between two otherwise
            // identical attempts here).
            val buffer = Wearable.getDataClient(context).dataItems.await()
            try {
                Log.d(TAG, "dataItems returned ${buffer.count} item(s) total: " +
                    (0 until buffer.count).map { buffer[it].uri }.toString())
                val item = (0 until buffer.count)
                    .map { buffer[it] }
                    .firstOrNull { it.uri.path == SessionTransferPaths.SESSION_DATA_PATH }
                    ?: return PhoneSyncResult.NotLoggedInOnPhone

                val dataMap = DataMapItem.fromDataItem(item).dataMap
                val payload = dataMap.getString(KEY_PAYLOAD) ?: return PhoneSyncResult.NotLoggedInOnPhone
                val transfer = Json.decodeFromString<SessionTransfer>(payload)
                applySession(transfer)
                PhoneSyncResult.Success
            } finally {
                buffer.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "failed to read the phone's session", e)
            PhoneSyncResult.Error(e.message ?: "Could not read the phone's session")
        }
    }

    private fun applySession(transfer: SessionTransfer) {
        ServerConfigStore(context).setBaseUrl(transfer.serverUrl)
        TokenStore(context).saveTokenPair(transfer.accessToken, transfer.refreshToken)
        val cfId = transfer.cloudflareAccessClientId
        val cfSecret = transfer.cloudflareAccessClientSecret
        if (cfId != null && cfSecret != null) {
            CloudflareAccessStore(context).setCredentials(CloudflareAccessCredentials(cfId, cfSecret))
        }
    }

    private companion object {
        const val KEY_PAYLOAD = "payload"
    }
}
