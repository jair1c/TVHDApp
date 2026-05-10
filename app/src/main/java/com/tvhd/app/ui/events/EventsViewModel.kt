package com.tvhd.app.ui.events

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tvhd.app.data.model.Event
import com.tvhd.app.data.repository.Result
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
                Log.d("EventsVM", "Iniciando loadAll()")
                val result = repo.loadAll()
                Log.d("EventsVM", "loadAll() resultado: $result")
                when (result) {
                    is Result.Success -> {
                        Log.d("EventsVM", "Eventos cargados: ${result.data.events.size}")
                        _events.value = result.data.events
                    }
                    is Result.Error -> {
                        Log.e("EventsVM", "Error: ${result.message}")
                        error.value = result.message
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Log.e("EventsVM", "Excepción en load()", e)
                error.value = "Error: ${e.javaClass.simpleName} – ${e.message}"
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
                if (result is Result.Success)
                    _events.value = result.data.events
            } catch (e: Exception) {
                Log.e("EventsVM", "Excepción en refresh()", e)
                error.value = "Error: ${e.javaClass.simpleName} – ${e.message}"
            }
            isLoading.value = false
        }
    }
}
