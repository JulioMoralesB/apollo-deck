package com.apollox10.apollodeck.wearsync

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apollox10.apollodeck.BuildConfig
import com.apollox10.apollodeck.core.model.Action
import com.apollox10.apollodeck.core.net.ApiClient
import com.apollox10.apollodeck.core.store.TokenStore
import com.apollox10.apollodeck.core.sync.TileGridActionRef
import com.apollox10.apollodeck.core.sync.TileGridSelection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Mirrors the watch's own cap (MAX_GRID_ACTIONS in
// wear/.../tile/MultiActionTileService.kt) — the grid tile never shows more
// than this, so the picker doesn't let you select past it.
const val MAX_TILE_GRID_ACTIONS = 9

sealed interface TileGridConfigUiState {
    data object Loading : TileGridConfigUiState
    data class Loaded(val actionsByService: List<Pair<String, List<Action>>>) : TileGridConfigUiState
    data class Error(val message: String) : TileGridConfigUiState
}

class TileGridConfigViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<TileGridConfigUiState>(TileGridConfigUiState.Loading)
    val uiState: StateFlow<TileGridConfigUiState> = _uiState

    // Selected refs, in the order they were tapped — that order becomes the
    // grid's display order. Pre-seeded from whatever was last saved so
    // reopening this screen reflects the current watch config.
    private val _selected = MutableStateFlow(TileGridPublisher.load(application).actions)
    val selected: StateFlow<List<TileGridActionRef>> = _selected

    private val _accentColor = MutableStateFlow(TileGridPublisher.load(application).accentColor)
    val accentColor: StateFlow<Int> = _accentColor

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val app = getApplication<Application>()
            if (!TokenStore(app).isLoggedIn()) {
                _uiState.value = TileGridConfigUiState.Error("Open Apollo Deck and log in first")
                return@launch
            }

            try {
                val client = ApiClient.create(app, debugLogging = BuildConfig.DEBUG)
                val services = client.authenticatedApi.getServices()
                val grouped = services.mapNotNull { service ->
                    val runnable = service.actions.orEmpty()
                        .filter { it.endpoint != null && it.method != "href" }
                    if (runnable.isEmpty()) null else service.name to runnable
                }
                _uiState.value = if (grouped.isEmpty()) {
                    TileGridConfigUiState.Error("No actions available to pin")
                } else {
                    TileGridConfigUiState.Loaded(grouped)
                }
            } catch (e: Exception) {
                _uiState.value = TileGridConfigUiState.Error(e.message ?: "Could not load services")
            }
        }
    }

    fun toggle(serviceName: String, action: Action) {
        val endpoint = action.endpoint ?: return
        val ref = TileGridActionRef(serviceName, endpoint)
        val current = _selected.value
        _selected.value = when {
            current.contains(ref) -> current - ref
            current.size < MAX_TILE_GRID_ACTIONS -> current + ref
            else -> current
        }
    }

    fun selectColor(color: Int) {
        _accentColor.value = color
    }

    fun save(onSaved: () -> Unit) {
        TileGridPublisher.save(getApplication(), TileGridSelection(_selected.value, _accentColor.value))
        onSaved()
    }
}
