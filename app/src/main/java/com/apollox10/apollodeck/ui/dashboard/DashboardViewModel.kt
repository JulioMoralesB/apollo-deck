package com.apollox10.apollodeck.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apollox10.apollodeck.BuildConfig
import com.apollox10.apollodeck.core.model.Action
import com.apollox10.apollodeck.core.model.Service
import com.apollox10.apollodeck.core.net.ApiClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

private const val REFRESH_INTERVAL_MS = 30_000L
// How long a card holds its success/error tint before reverting to idle —
// matches the web dashboard's ActionPanel.jsx exactly (setTimeout(..., 2000)).
private const val ACTION_STATE_HOLD_MS = 2_000L

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Loaded(val services: List<Service>) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

// Mirrors the web dashboard's per-card actionStates: a brief loading spinner
// while in flight, then a green/red flash before reverting — see
// ActionPanel.jsx's executeAction. Keyed by the action's endpoint in
// DashboardViewModel since that's unique within whichever single service's
// action grid is currently open.
enum class ActionCardState { Loading, Success, Error }

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val client = ApiClient.create(application, debugLogging = BuildConfig.DEBUG)

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState

    private val _actionResult = MutableStateFlow<String?>(null)
    val actionResult: StateFlow<String?> = _actionResult

    private val _actionStates = MutableStateFlow<Map<String, ActionCardState>>(emptyMap())
    val actionStates: StateFlow<Map<String, ActionCardState>> = _actionStates

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private var pollingJob: Job? = null

    // Same 30s cadence as the web dashboard's polling loop.
    fun startPolling(onSessionExpired: () -> Unit) {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (isActive) {
                refresh(onSessionExpired)
                delay(REFRESH_INTERVAL_MS)
            }
        }
    }

    // Pull-to-refresh — a one-off refresh outside the 30s cadence, so a
    // manual pull is felt immediately instead of waiting for the next tick.
    fun refreshNow(onSessionExpired: () -> Unit) {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                refresh(onSessionExpired)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private suspend fun refresh(onSessionExpired: () -> Unit) {
        try {
            val services = client.authenticatedApi.getServices()
            _uiState.value = DashboardUiState.Loaded(services)
        } catch (e: HttpException) {
            if (e.code() == 401 || e.code() == 403) {
                onSessionExpired()
            } else {
                _uiState.value = DashboardUiState.Error("Server error (${e.code()})")
            }
        } catch (e: IOException) {
            _uiState.value = DashboardUiState.Error("Could not reach the server")
        }
    }

    fun executeAction(action: Action) {
        val endpoint = action.endpoint ?: return
        val method = action.method ?: "POST"
        viewModelScope.launch {
            _actionStates.value = _actionStates.value + (endpoint to ActionCardState.Loading)
            val cardState = try {
                val result = client.actionExecutor.execute(endpoint, method)
                _actionResult.value = when {
                    !result.message.isNullOrBlank() -> "${action.label}: ${result.message}"
                    result.success -> "${action.label}: done"
                    else -> "${action.label}: failed"
                }
                if (result.success) ActionCardState.Success else ActionCardState.Error
            } catch (e: Exception) {
                _actionResult.value = "${action.label}: ${e.message ?: "failed"}"
                ActionCardState.Error
            }
            _actionStates.value = _actionStates.value + (endpoint to cardState)
            delay(ACTION_STATE_HOLD_MS)
            _actionStates.value = _actionStates.value - endpoint
        }
    }

    fun clearActionResult() {
        _actionResult.value = null
    }

    override fun onCleared() {
        pollingJob?.cancel()
    }
}
