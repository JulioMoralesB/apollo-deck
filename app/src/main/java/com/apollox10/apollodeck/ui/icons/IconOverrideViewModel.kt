package com.apollox10.apollodeck.ui.icons

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apollox10.apollodeck.BuildConfig
import com.apollox10.apollodeck.core.net.ApiClient
import com.apollox10.apollodeck.core.store.IconOverrideStore
import com.apollox10.apollodeck.core.store.TokenStore
import com.apollox10.apollodeck.wearsync.IconOverridePublisher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface IconOverrideUiState {
    data object Loading : IconOverrideUiState
    data class Loaded(val iconNames: List<String>) : IconOverrideUiState
    data class Error(val message: String) : IconOverrideUiState
}

// Every distinct icon name currently used across services and actions —
// not just ones falling back to the generic icon — so an override can fix
// a wrong-but-technically-mapped icon too, not only an unrecognized one.
class IconOverrideViewModel(application: Application) : AndroidViewModel(application) {

    private val store = IconOverrideStore(application)

    private val _uiState = MutableStateFlow<IconOverrideUiState>(IconOverrideUiState.Loading)
    val uiState: StateFlow<IconOverrideUiState> = _uiState

    private val _overrides = MutableStateFlow(store.getAll())
    val overrides: StateFlow<Map<String, String>> = _overrides

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = IconOverrideUiState.Loading
            val app = getApplication<Application>()
            if (!TokenStore(app).isLoggedIn()) {
                _uiState.value = IconOverrideUiState.Error("Open Apollo Deck and log in first")
                return@launch
            }

            try {
                val client = ApiClient.create(app, debugLogging = BuildConfig.DEBUG)
                val services = client.authenticatedApi.getServices()
                val names = (services.mapNotNull { it.icon } + services.flatMap { it.actions.orEmpty().mapNotNull { a -> a.icon } })
                    .distinct()
                    .sorted()
                _uiState.value = if (names.isEmpty()) {
                    IconOverrideUiState.Error("No icons found — open the dashboard first")
                } else {
                    IconOverrideUiState.Loaded(names)
                }
            } catch (e: Exception) {
                _uiState.value = IconOverrideUiState.Error(e.message ?: "Could not load services")
            }
        }
    }

    fun setOverride(iconName: String, chosenIconId: String) {
        store.setOverride(iconName, chosenIconId)
        _overrides.value = store.getAll()
        IconOverridePublisher.publish(getApplication())
    }

    fun clearOverride(iconName: String) {
        store.clearOverride(iconName)
        _overrides.value = store.getAll()
        IconOverridePublisher.publish(getApplication())
    }
}
