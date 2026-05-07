package com.tvhd.app.ui.channels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tvhd.app.data.model.Channel
import com.tvhd.app.data.model.ChannelCategory
import com.tvhd.app.data.repository.TvhdRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ChannelsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = TvhdRepository(app)

    private val _allChannels  = MutableStateFlow<List<Channel>>(emptyList())
    private val _category     = MutableStateFlow(ChannelCategory.ALL)
    private val _query        = MutableStateFlow("")
    val isLoading             = MutableStateFlow(false)

    val channels: StateFlow<List<Channel>> = combine(
        _allChannels, _category, _query
    ) { all, cat, q ->
        all.filter { ch ->
            val catOk = cat == ChannelCategory.ALL || ch.category == cat.name.lowercase()
            val qOk   = q.isBlank() || ch.name.contains(q, ignoreCase = true)
            catOk && qOk
        }
    }.let {
        val flow = MutableStateFlow<List<Channel>>(emptyList())
        viewModelScope.launch {
            it.collect { filtered -> flow.value = filtered }
        }
        flow
    }

    fun load() {
        viewModelScope.launch {
            isLoading.value = true
            _allChannels.value = repo.getChannels()
            isLoading.value = false
        }
    }

    fun refresh() {
        viewModelScope.launch {
            isLoading.value = true
            repo.refresh()
            _allChannels.value = repo.getChannels()
            isLoading.value = false
        }
    }

    fun setCategory(cat: ChannelCategory) { _category.value = cat }
    fun setQuery(q: String)               { _query.value = q }
}
