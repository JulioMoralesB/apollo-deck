package com.apollox10.apollodeck.widget

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apollox10.apollodeck.BuildConfig
import com.apollox10.apollodeck.core.model.Action
import com.apollox10.apollodeck.core.net.ApiClient
import com.apollox10.apollodeck.core.store.TokenStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface WidgetConfigureUiState {
    data object Loading : WidgetConfigureUiState
    data class Loaded(val actionsByService: List<Pair<String, List<Action>>>) : WidgetConfigureUiState
    data class Error(val message: String) : WidgetConfigureUiState
}

class WidgetConfigureViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<WidgetConfigureUiState>(WidgetConfigureUiState.Loading)
    val uiState: StateFlow<WidgetConfigureUiState> = _uiState

    init {
        viewModelScope.launch {
            if (!TokenStore(application).isLoggedIn()) {
                _uiState.value = WidgetConfigureUiState.Error("Open Apollo Deck and log in first")
                return@launch
            }

            try {
                val client = ApiClient.create(application, debugLogging = BuildConfig.DEBUG)
                val services = client.authenticatedApi.getServices()
                // href actions open a link, they don't call the backend —
                // they don't fit an "execute an action" widget.
                val grouped = services.mapNotNull { service ->
                    val runnable = service.actions.orEmpty()
                        .filter { it.endpoint != null && it.method != "href" }
                    if (runnable.isEmpty()) null else service.name to runnable
                }
                _uiState.value = if (grouped.isEmpty()) {
                    WidgetConfigureUiState.Error("No actions available to pin")
                } else {
                    WidgetConfigureUiState.Loaded(grouped)
                }
            } catch (e: Exception) {
                _uiState.value = WidgetConfigureUiState.Error(e.message ?: "Could not load services")
            }
        }
    }
}
