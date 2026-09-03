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

// Standalone login for the watch — no phone pairing, no shared session.
// Cloudflare Access fields aren't offered here (phone's LoginViewModel has
// them): a watch-sized form for a second, less common credential pair isn't
// worth the screen real estate yet. Someone whose backend sits behind
// Access can still set it up once on the phone; this just doesn't cover it
// standalone.
class LoginViewModel(application: Application) : AndroidViewModel(application) {

    var serverUrl by mutableStateOf(ServerConfigStore(application).getBaseUrl() ?: "")
    var username by mutableStateOf("")
    var password by mutableStateOf("")

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

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
