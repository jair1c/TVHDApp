package com.tvhd.app.ui.channels

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tvhd.app.R
import com.tvhd.app.data.model.Channel
import com.tvhd.app.databinding.ItemChannelGridBinding
import com.tvhd.app.databinding.ItemChannelListBinding

// ── DIFF CALLBACK ─────────────────────────────────────────────────────────────
private val DIFF = object : DiffUtil.ItemCallback<Channel>() {
    override fun areItemsTheSame(a: Channel, b: Channel) = a.streamId == b.streamId
    override fun areContentsTheSame(a: Channel, b: Channel) = a == b
}

// ── GRID ADAPTER ──────────────────────────────────────────────────────────────
class ChannelGridAdapter(
    private val onClick: (Channel) -> Unit
) : ListAdapter<Channel, ChannelGridAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemChannelGridBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemChannelGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        val ch = getItem(position)
        with(holder.binding) {
            textName.text = ch.name
            badgeHd.visibility = if (ch.hd) android.view.View.VISIBLE else android.view.View.GONE
            badgeLive.visibility = if (ch.live) android.view.View.VISIBLE else android.view.View.GONE

            // Mostrar inicial como fallback
            textInitial.text = ch.name.firstOrNull()?.uppercase() ?: "?"

            Glide.with(root.context)
                .load(ch.logoUrl.ifEmpty { null })
                .placeholder(R.drawable.bg_channel_placeholder)
                .error(R.drawable.bg_channel_placeholder)
                .into(imageLogo)

            // Ocultar letra si hay logo
            textInitial.visibility =
                if (ch.logoUrl.isBlank()) android.view.View.VISIBLE else android.view.View.GONE

            root.setOnClickListener { onClick(ch) }
        }
    }
}

// ── LIST ADAPTER ──────────────────────────────────────────────────────────────
class ChannelListAdapter(
    private val onClick: (Channel) -> Unit
) : ListAdapter<Channel, ChannelListAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemChannelListBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemChannelListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        val ch = getItem(position)
        with(holder.binding) {
            textName.text = ch.name
            textCategory.text = ch.category.replaceFirstChar { it.uppercase() }
            badgeHd.visibility   = if (ch.hd)   android.view.View.VISIBLE else android.view.View.GONE
            badgeLive.visibility = if (ch.live)  android.view.View.VISIBLE else android.view.View.GONE
            textInitial.text = ch.name.firstOrNull()?.uppercase() ?: "?"

            Glide.with(root.context)
                .load(ch.logoUrl.ifEmpty { null })
                .placeholder(R.drawable.bg_channel_placeholder)
                .error(R.drawable.bg_channel_placeholder)
                .into(imageLogo)

            textInitial.visibility =
                if (ch.logoUrl.isBlank()) android.view.View.VISIBLE else android.view.View.GONE

            root.setOnClickListener { onClick(ch) }
        }
    }
}
