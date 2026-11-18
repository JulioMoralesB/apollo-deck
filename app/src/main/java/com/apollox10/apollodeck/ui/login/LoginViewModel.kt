package com.apollox10.apollodeck.ui.login

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apollox10.apollodeck.BuildConfig
import com.apollox10.apollodeck.core.auth.HomeAssistantAuth
import com.apollox10.apollodeck.core.store.ServerConfigStore
import com.apollox10.apollodeck.wearsync.PhoneSessionPublisher
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

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    var serverUrl by mutableStateOf(ServerConfigStore(application).getBaseUrl() ?: "")
    var token by mutableStateOf("")

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    fun login(onSuccess: () -> Unit) {
        val url = serverUrl.trim()
        val trimmedToken = token.trim()

        if (url.isEmpty() || trimmedToken.isEmpty()) {
            _uiState.value = LoginUiState.Error("Home Assistant URL and access token are required")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val context = getApplication<Application>()
                HomeAssistantAuth.connect(context, url, trimmedToken, debugLogging = BuildConfig.DEBUG)
                PhoneSessionPublisher.publishCurrentSession(context)
                _uiState.value = LoginUiState.Idle
                onSuccess()
            } catch (e: HttpException) {
                _uiState.value = when (e.code()) {
                    401 -> LoginUiState.Error(
                        "Invalid token — create a long-lived access token in " +
                            "Home Assistant under Profile → Security"
                    )
                    404 -> LoginUiState.Error("No Home Assistant API at this URL")
                    else -> LoginUiState.Error("Server error (${e.code()})")
                }
            } catch (e: IOException) {
                _uiState.value = LoginUiState.Error("Could not reach Home Assistant — check the URL")
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error("Something went wrong: ${e.message}")
            }
        }
    }
}
