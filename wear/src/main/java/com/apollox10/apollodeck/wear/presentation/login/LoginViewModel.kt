package com.apollox10.apollodeck.wear.presentation.login

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apollox10.apollodeck.core.auth.AuthRepository
import com.apollox10.apollodeck.core.net.ApiClient
import com.apollox10.apollodeck.core.store.ServerConfigStore
import com.apollox10.apollodeck.wear.BuildConfig
import com.apollox10.apollodeck.wear.sync.PhoneSessionSync
import com.apollox10.apollodeck.wear.sync.PhoneSyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Error(val message: String) : LoginUiState
}

// Typing server URL + username + password (and, if the backend needs it,
// Cloudflare Access credentials) on a watch keyboard is painful, so
// "sign in from phone" is the primary path — it pulls the already-logged-in
// session from a paired phone over the Wear Data Layer (see
// PhoneSessionSync). The manual form below stays as a fallback for a watch
// used without a phone nearby.
class LoginViewModel(application: Application) : AndroidViewModel(application) {

    var serverUrl by mutableStateOf(ServerConfigStore(application).getBaseUrl() ?: "")
    var username by mutableStateOf("")
    var password by mutableStateOf("")

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    private val phoneSessionSync = PhoneSessionSync(application)

    fun signInFromPhone(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            when (val result = phoneSessionSync.requestSessionFromPhone()) {
                PhoneSyncResult.Success -> {
                    _uiState.value = LoginUiState.Idle
                    onSuccess()
                }
                PhoneSyncResult.NotLoggedInOnPhone ->
                    _uiState.value = LoginUiState.Error("Sign in on the phone app first")
                is PhoneSyncResult.Error ->
                    _uiState.value = LoginUiState.Error(result.message)
            }
        }
    }

    fun login(onSuccess: () -> Unit) {
        val url = serverUrl.trim()
        val user = username.trim()

        if (url.isEmpty() || user.isEmpty() || password.isEmpty()) {
            _uiState.value = LoginUiState.Error("Server, username, and password are required")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val context = getApplication<Application>()
                ServerConfigStore(context).setBaseUrl(url)

                val client = ApiClient.create(context, debugLogging = BuildConfig.DEBUG)
                AuthRepository(client).login(user, password)
                _uiState.value = LoginUiState.Idle
                onSuccess()
            } catch (e: HttpException) {
                _uiState.value = when (e.code()) {
                    401 -> LoginUiState.Error("Invalid username or password")
                    403 -> LoginUiState.Error("Blocked (403) — set up Cloudflare Access on the phone app first")
                    else -> LoginUiState.Error("Server error (${e.code()})")
                }
            } catch (e: IOException) {
                _uiState.value = LoginUiState.Error("Could not reach the server")
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error("Something went wrong: ${e.message}")
            }
        }
    }
}
