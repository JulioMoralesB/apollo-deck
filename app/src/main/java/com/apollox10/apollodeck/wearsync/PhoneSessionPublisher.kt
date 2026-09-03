package com.apollox10.apollodeck.wearsync

import android.content.Context
import android.net.Uri
import android.util.Log
import com.apollox10.apollodeck.core.store.CloudflareAccessStore
import com.apollox10.apollodeck.core.store.ServerConfigStore
import com.apollox10.apollodeck.core.store.TokenStore
import com.apollox10.apollodeck.core.sync.SessionTransfer
import com.apollox10.apollodeck.core.sync.SessionTransferPaths
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "PhoneSessionPublisher"

// Publishes the current session as a synced Wear Data Layer DataItem so a
// paired watch can read it directly instead of this app having to be woken
// up and reachable at the exact moment the watch asks. An earlier version
// used a live MessageClient request/response instead; tested on real
// hardware, Play Services logged "Failed to deliver message to AppKey" for
// it even with both apps foregrounded, so this DataItem approach — the
// watch just reads whatever the OS has already synced — replaced it.
object PhoneSessionPublisher {

    fun publishCurrentSession(context: Context) {
        val tokenStore = TokenStore(context)
        val accessToken = tokenStore.getAccessToken()
        val refreshToken = tokenStore.getRefreshToken()
        val serverUrl = ServerConfigStore(context).getBaseUrl()

        if (accessToken == null || refreshToken == null || serverUrl == null) {
            Log.d(TAG, "not logged in, clearing any published session instead")
            clear(context)
            return
        }

        val credentials = CloudflareAccessStore(context).getCredentials()
        val transfer = SessionTransfer(
            serverUrl = serverUrl,
            accessToken = accessToken,
            refreshToken = refreshToken,
            cloudflareAccessClientId = credentials?.clientId,
            cloudflareAccessClientSecret = credentials?.clientSecret,
        )
        val request = PutDataMapRequest.create(SessionTransferPaths.SESSION_DATA_PATH).apply {
            dataMap.putString(KEY_PAYLOAD, Json.encodeToString(transfer))
        }.asPutDataRequest().setUrgent()

        Wearable.getDataClient(context).putDataItem(request)
            .addOnSuccessListener { item -> Log.d(TAG, "published session dataItem uri=${item.uri}") }
            .addOnFailureListener { e -> Log.w(TAG, "failed to publish session", e) }
    }

    fun clear(context: Context) {
        val uri = Uri.Builder()
            .scheme(PutDataRequest.WEAR_URI_SCHEME)
            .path(SessionTransferPaths.SESSION_DATA_PATH)
            .build()
        Wearable.getDataClient(context).deleteDataItems(uri)
            .addOnSuccessListener { count -> Log.d(TAG, "cleared $count published session item(s)") }
            .addOnFailureListener { e -> Log.w(TAG, "failed to clear published session", e) }
    }

    private const val KEY_PAYLOAD = "payload"
}
