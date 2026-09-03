package com.apollox10.apollodeck.wear.tile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apollox10.apollodeck.core.model.Action
import com.apollox10.apollodeck.core.net.ApiClient
import com.apollox10.apollodeck.wear.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class TileConfigItem(val serviceName: String, val action: Action)

sealed interface TileConfigUiState {
    data object Loading : TileConfigUiState
    data class Loaded(val items: List<TileConfigItem>) : TileConfigUiState
    data class Error(val message: String) : TileConfigUiState
}

class TileConfigViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<TileConfigUiState>(TileConfigUiState.Loading)
    val uiState: StateFlow<TileConfigUiState> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = TileConfigUiState.Loading
            try {
                val client = ApiClient.create(getApplication(), debugLogging = BuildConfig.DEBUG)
                val items = client.authenticatedApi.getServices().flatMap { service ->
                    (service.actions.orEmpty())
                        .filter { it.method != null && it.method != "href" && it.endpoint != null }
                        .map { TileConfigItem(service.name, it) }
                }
                _uiState.value = TileConfigUiState.Loaded(items)
            } catch (e: Exception) {
                _uiState.value = TileConfigUiState.Error(e.message ?: "Failed to load services")
            }
        }
    }
}
