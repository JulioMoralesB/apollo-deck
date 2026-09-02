package com.apollox10.apollodeck.ui.settings

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.apollox10.apollodeck.core.store.CloudflareAccessCredentials
import com.apollox10.apollodeck.core.store.CloudflareAccessStore
import com.apollox10.apollodeck.core.store.ServerConfigStore
import com.apollox10.apollodeck.core.store.TokenStore

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val serverConfigStore = ServerConfigStore(application)
    private val cloudflareAccessStore = CloudflareAccessStore(application)
    private val tokenStore = TokenStore(application)

    var serverUrl by mutableStateOf(serverConfigStore.getBaseUrl().orEmpty())
    var cloudflareAccessClientId by mutableStateOf(
        cloudflareAccessStore.getCredentials()?.clientId.orEmpty()
    )
    var cloudflareAccessClientSecret by mutableStateOf(
        cloudflareAccessStore.getCredentials()?.clientSecret.orEmpty()
    )
    var errorMessage by mutableStateOf<String?>(null)

    // Saving always logs the user out, even if only the Cloudflare Access
    // fields changed — the current session's tokens were issued against
    // whatever config was active at login, and there's no reliable way to
    // tell from here whether they're still valid against the new one. A
    // clean re-login is simpler than trying to detect that.
    fun save(onSaved: () -> Unit) {
        val url = serverUrl.trim()
        if (url.isEmpty()) {
            errorMessage = "Server URL is required"
            return
        }

        serverConfigStore.setBaseUrl(url)

        val id = cloudflareAccessClientId.trim()
        val secret = cloudflareAccessClientSecret.trim()
        if (id.isNotEmpty() && secret.isNotEmpty()) {
            cloudflareAccessStore.setCredentials(CloudflareAccessCredentials(id, secret))
        } else {
            cloudflareAccessStore.clear()
        }

        tokenStore.clear()
        onSaved()
    }
}
