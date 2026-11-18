package com.apollox10.apollodeck.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apollox10.apollodeck.BuildConfig
import com.apollox10.apollodeck.core.model.Action
import com.apollox10.apollodeck.core.net.ApiClient
import com.apollox10.apollodeck.core.store.ConfirmActionStore
import com.apollox10.apollodeck.wearsync.ConfirmActionPublisher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface ConfirmActionsUiState {
    data object Loading : ConfirmActionsUiState
    data class Loaded(val actions: List<Action>) : ConfirmActionsUiState
    data class Error(val message: String) : ConfirmActionsUiState
}

class ConfirmActionsViewModel(application: Application) : AndroidViewModel(application) {

    private val store = ConfirmActionStore(application)

    private val _uiState = MutableStateFlow<ConfirmActionsUiState>(ConfirmActionsUiState.Loading)
    val uiState: StateFlow<ConfirmActionsUiState> = _uiState

    private val _required = MutableStateFlow(store.getAll())
    val required: StateFlow<Set<String>> = _required

    init {
        viewModelScope.launch {
            _uiState.value = try {
                val client = ApiClient.create(application, debugLogging = BuildConfig.DEBUG)
                val actions = client.getServices().flatMap { it.actions.orEmpty() }.filter { it.endpoint != null }
                ConfirmActionsUiState.Loaded(actions)
            } catch (e: Exception) {
                ConfirmActionsUiState.Error(e.message ?: "Failed to load scripts")
            }
        }
    }

    fun setRequired(entityId: String, required: Boolean) {
        store.setRequired(entityId, required)
        _required.value = store.getAll()
        ConfirmActionPublisher.publish(getApplication())
    }
}
