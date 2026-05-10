package com.tvhd.app.ui.events

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tvhd.app.databinding.FragmentEventsBinding
import com.tvhd.app.ui.player.PlayerActivity
import kotlinx.coroutines.launch

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
                title = "${event.homeTeam} vs ${event.awayTeam}",
                url   = channel.streamUrl,
            )
        }

        binding.recycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@EventsFragment.adapter
        }

        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.events.collect { evs ->
                adapter.submitList(evs)
                binding.textEmpty.visibility =
                    if (evs.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { loading ->
                binding.swipeRefresh.isRefreshing = loading
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { err ->
                if (err != null) {
                    binding.textEmpty.text = "Error: $err"
                    binding.textEmpty.visibility = View.VISIBLE
                }
            }
        }

        viewModel.load()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
