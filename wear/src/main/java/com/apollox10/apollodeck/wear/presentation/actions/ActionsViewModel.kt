package com.apollox10.apollodeck.wear.presentation.actions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apollox10.apollodeck.core.auth.AuthRepository
import com.apollox10.apollodeck.core.model.Action
import com.apollox10.apollodeck.core.net.ApiClient
import com.apollox10.apollodeck.core.net.ApolloDeckClient
import com.apollox10.apollodeck.wear.BuildConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface ActionsUiState {
    data object Loading : ActionsUiState
    data class Loaded(val items: List<ActionItem>) : ActionsUiState
    data class Error(val message: String) : ActionsUiState
}

data class ActionItem(val serviceName: String, val action: Action)

class ActionsViewModel(application: Application) : AndroidViewModel(application) {

    private val client: ApolloDeckClient = ApiClient.create(application, debugLogging = BuildConfig.DEBUG)
    private val authRepository = AuthRepository(client)

    private val _uiState = MutableStateFlow<ActionsUiState>(ActionsUiState.Loading)
    val uiState: StateFlow<ActionsUiState> = _uiState

    private val _executionResult = MutableStateFlow<String?>(null)
    val executionResult: StateFlow<String?> = _executionResult

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = ActionsUiState.Loading
            try {
                val services = client.authenticatedApi.getServices()
                // "href" actions open an external link — nothing to "execute"
                // for one on a watch, so they're left off this list (same
                // convention the widget follows).
                val items = services.flatMap { service ->
                    (service.actions.orEmpty())
                        .filter { it.method != null && it.method != "href" && it.endpoint != null }
                        .map { ActionItem(service.name, it) }
                }
                _uiState.value = ActionsUiState.Loaded(items)
            } catch (e: Exception) {
                _uiState.value = ActionsUiState.Error(e.message ?: "Failed to load services")
            }
        }
    }

    fun execute(item: ActionItem) {
        val endpoint = item.action.endpoint ?: return
        val method = item.action.method ?: return
        viewModelScope.launch {
            _executionResult.value = "${item.action.label}…"
            try {
                val result = client.actionExecutor.execute(endpoint, method)
                _executionResult.value = result.message ?: if (result.success) "Done" else "Failed"
            } catch (e: Exception) {
                _executionResult.value = "Error: ${e.message}"
            }
            delay(3000)
            _executionResult.value = null
        }
    }

    fun logout() {
        authRepository.logout()
    }
}
