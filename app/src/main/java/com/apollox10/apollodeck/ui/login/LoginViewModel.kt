package com.apollox10.apollodeck.ui.login

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apollox10.apollodeck.BuildConfig
import com.apollox10.apollodeck.core.auth.AuthRepository
import com.apollox10.apollodeck.core.net.ApiClient
import com.apollox10.apollodeck.core.store.ServerConfigStore
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
    var username by mutableStateOf("")
    var password by mutableStateOf("")

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    fun login(onSuccess: () -> Unit) {
        val url = serverUrl.trim()
        val user = username.trim()

        if (url.isEmpty() || user.isEmpty() || password.isEmpty()) {
            _uiState.value = LoginUiState.Error("Server URL, username, and password are required")
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
                _uiState.value = if (e.code() == 401) {
                    LoginUiState.Error("Invalid username or password")
                } else {
                    LoginUiState.Error("Server error (${e.code()})")
                }
            } catch (e: IOException) {
                _uiState.value = LoginUiState.Error("Could not reach the server — check the URL")
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error("Something went wrong: ${e.message}")
            }
        }
    }
}
