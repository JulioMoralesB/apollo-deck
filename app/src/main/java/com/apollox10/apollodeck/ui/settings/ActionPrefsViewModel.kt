package com.apollox10.apollodeck.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apollox10.apollodeck.BuildConfig
import com.apollox10.apollodeck.core.model.Action
import com.apollox10.apollodeck.core.net.ApiClient
import com.apollox10.apollodeck.core.store.ActionPrefsStore
import com.apollox10.apollodeck.wearsync.ActionPrefsPublisher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface ActionPrefsUiState {
    data object Loading : ActionPrefsUiState
    data class Loaded(val actions: List<Action>) : ActionPrefsUiState
    data class Error(val message: String) : ActionPrefsUiState
}

class ActionPrefsViewModel(application: Application) : AndroidViewModel(application) {

    private val store = ActionPrefsStore(application)

    private val _uiState = MutableStateFlow<ActionPrefsUiState>(ActionPrefsUiState.Loading)
    val uiState: StateFlow<ActionPrefsUiState> = _uiState

    private val _confirmRequired = MutableStateFlow(store.getConfirmRequired())
    val confirmRequired: StateFlow<Set<String>> = _confirmRequired

    init {
        viewModelScope.launch {
            _uiState.value = try {
                val client = ApiClient.create(application, debugLogging = BuildConfig.DEBUG)
                // Already in the saved order (getServices applies it).
                val actions = client.getServices().flatMap { it.actions.orEmpty() }.filter { it.endpoint != null }
                ActionPrefsUiState.Loaded(actions)
            } catch (e: Exception) {
                ActionPrefsUiState.Error(e.message ?: "Failed to load scripts")
            }
        }
    }

    // Persists the full list, not just the moved pair, so the order is
    // explicit for every script shown — including ones that were only
    // sorted alphabetically until now.
    fun move(from: Int, to: Int) {
        val actions = (_uiState.value as? ActionPrefsUiState.Loaded)?.actions ?: return
        if (to !in actions.indices) return
        val reordered = actions.toMutableList().apply { add(to, removeAt(from)) }
        _uiState.value = ActionPrefsUiState.Loaded(reordered)
        store.setOrder(reordered.mapNotNull { it.endpoint })
        ActionPrefsPublisher.publish(getApplication())
    }

    fun setConfirmRequired(entityId: String, required: Boolean) {
        store.setConfirmRequired(entityId, required)
        _confirmRequired.value = store.getConfirmRequired()
        ActionPrefsPublisher.publish(getApplication())
    }
}
