package com.tvhd.app.ui.events

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tvhd.app.data.model.Event
import com.tvhd.app.data.repository.TvhdRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EventsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = TvhdRepository(app)

    val isLoading = MutableStateFlow(false)
    val error     = MutableStateFlow<String?>(null)

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events

    fun load() {
        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            try {
                val result = repo.loadAll()
                when (result) {
                    is com.tvhd.app.data.repository.Result.Success ->
                        _events.value = result.data.events
                    is com.tvhd.app.data.repository.Result.Error ->
                        error.value = result.message
                    else -> {}
                }
            } catch (e: Exception) {
                error.value = e.message ?: "Error desconocido"
            }
            isLoading.value = false
        }
    }

    fun refresh() {
        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            try {
                repo.refresh()
                val result = repo.loadAll()
                if (result is com.tvhd.app.data.repository.Result.Success)
                    _events.value = result.data.events
            } catch (e: Exception) {
                error.value = e.message
            }
            isLoading.value = false
        }
    }
}
