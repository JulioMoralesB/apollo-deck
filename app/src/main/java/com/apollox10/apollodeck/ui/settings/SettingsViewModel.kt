package com.apollox10.apollodeck.ui.settings

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.apollox10.apollodeck.core.auth.HomeAssistantAuth
import com.apollox10.apollodeck.core.store.ServerConfigStore

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val serverConfigStore = ServerConfigStore(application)

    var serverUrl by mutableStateOf(serverConfigStore.getBaseUrl().orEmpty())
    var errorMessage by mutableStateOf<String?>(null)

    // Saving always disconnects: the token was checked against whatever URL
    // was active at login, and re-entering it on the login screen (with the
    // new URL prefilled) re-runs that check instead of guessing.
    fun save(onSaved: () -> Unit) {
        val url = serverUrl.trim()
        if (url.isEmpty()) {
            errorMessage = "Home Assistant URL is required"
            return
        }

        serverConfigStore.setBaseUrl(url)
        HomeAssistantAuth.disconnect(getApplication())
        onSaved()
    }
}
