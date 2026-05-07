package com.tvhd.app.ui.events

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tvhd.app.data.model.Event
import com.tvhd.app.data.model.EventChannel
import com.tvhd.app.data.repository.TvhdRepository
import com.tvhd.app.databinding.FragmentEventsBinding
import com.tvhd.app.ui.player.PlayerActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ── VIEWMODEL ────────────────────────────────────────────────────────────────
class EventsViewModel(app: android.app.Application) : AndroidViewModel(app) {

    private val repo = TvhdRepository(app)
    val events    = MutableStateFlow<List<Event>>(emptyList())
    val isLoading = MutableStateFlow(false)

    fun load() {
        viewModelScope.launch {
            isLoading.value = true
            events.value = repo.getEvents()
            isLoading.value = false
        }
    }

    fun refresh() {
        viewModelScope.launch {
            isLoading.value = true
            repo.refresh()
            events.value = repo.getEvents()
            isLoading.value = false
        }
    }
}

// ── FRAGMENT ─────────────────────────────────────────────────────────────────
class EventsFragment : Fragment() {

    private var _binding: FragmentEventsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EventsViewModel by viewModels()
    private lateinit var adapter: EventsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = EventsAdapter { event, channel ->
            PlayerActivity.start(
                requireContext(),
                title  = "${event.homeTeam} vs ${event.awayTeam}",
                url    = channel.streamUrl,
            )
        }

        binding.recycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@EventsFragment.adapter
        }

        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }

        viewLifecycleOwner.let { owner ->
            owner.lifecycleScope().launch {
                viewModel.events.collect { evs ->
                    adapter.submitList(evs)
                    binding.textEmpty.visibility =
                        if (evs.isEmpty()) View.VISIBLE else View.GONE
                }
            }
            owner.lifecycleScope().launch {
                viewModel.isLoading.collect { loading ->
                    binding.swipeRefresh.isRefreshing = loading
                }
            }
        }

        viewModel.load()
    }

    private fun androidx.lifecycle.LifecycleOwner.lifecycleScope() =
        androidx.lifecycle.lifecycleScope

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
