package com.tvhd.app.ui.channels

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.tvhd.app.R
import com.tvhd.app.data.model.Channel
import com.tvhd.app.data.model.ChannelCategory
import com.tvhd.app.databinding.FragmentChannelsBinding
import com.tvhd.app.ui.player.PlayerActivity
import kotlinx.coroutines.launch

class ChannelsFragment : Fragment() {

    private var _binding: FragmentChannelsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChannelsViewModel by viewModels()

    private lateinit var gridAdapter: ChannelGridAdapter
    private lateinit var listAdapter: ChannelListAdapter
    private var isMosaicView = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChannelsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapters()
        setupCategories()
        setupSearch()
        setupViewToggle()
        observeViewModel()
        viewModel.load()
    }

    private fun setupAdapters() {
        val onChannelClick: (Channel) -> Unit = { channel ->
            PlayerActivity.start(requireContext(), channel.name, channel.watchUrl)
        }

        gridAdapter = ChannelGridAdapter(onChannelClick)
        listAdapter = ChannelListAdapter(onChannelClick)

        binding.recyclerGrid.apply {
            layoutManager = GridLayoutManager(context, gridSpanCount())
            adapter = gridAdapter
        }
        binding.recyclerList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = listAdapter
        }
        updateViewMode()
    }

    private fun gridSpanCount(): Int {
        val screenWidthDp = resources.configuration.screenWidthDp
        return when {
            screenWidthDp >= 840 -> 6   // TV / tablet landscape
            screenWidthDp >= 600 -> 4   // Tablet portrait
            else -> 3                    // Teléfono
        }
    }

    private fun setupCategories() {
        ChannelCategory.values().forEach { cat ->
            val chip = Chip(requireContext()).apply {
                text = "${cat.emoji} ${cat.label}"
                isCheckable = true
                isChecked = cat == ChannelCategory.ALL
                setOnClickListener { viewModel.setCategory(cat) }
            }
            binding.chipGroupCategories.addView(chip)
        }
    }

    private fun setupSearch() {
        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.setQuery(s?.toString() ?: "")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupViewToggle() {
        binding.btnMosaic.setOnClickListener {
            isMosaicView = true
            updateViewMode()
        }
        binding.btnList.setOnClickListener {
            isMosaicView = false
            updateViewMode()
        }
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }
    }

    private fun updateViewMode() {
        binding.recyclerGrid.visibility = if (isMosaicView) View.VISIBLE else View.GONE
        binding.recyclerList.visibility = if (isMosaicView) View.GONE else View.VISIBLE
        binding.btnMosaic.isSelected = isMosaicView
        binding.btnList.isSelected = !isMosaicView
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.channels.collect { channels ->
                gridAdapter.submitList(channels)
                listAdapter.submitList(channels)
                binding.textEmpty.visibility =
                    if (channels.isEmpty()) View.VISIBLE else View.GONE
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { loading ->
                binding.swipeRefresh.isRefreshing = loading
                binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
