package com.tvhd.app.ui.events

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tvhd.app.R
import com.tvhd.app.data.model.Event
import com.tvhd.app.data.model.EventChannel
import com.tvhd.app.databinding.ItemEventBinding
import com.tvhd.app.databinding.ItemEventChannelChipBinding

private val DIFF = object : DiffUtil.ItemCallback<Event>() {
    override fun areItemsTheSame(a: Event, b: Event) =
        a.match == b.match && a.time == b.time
    override fun areContentsTheSame(a: Event, b: Event) = a == b
}

class EventsAdapter(
    private val onChannelClick: (Event, EventChannel) -> Unit
) : ListAdapter<Event, EventsAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemEventBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        val ev = getItem(position)
        with(holder.binding) {
            // Competición y hora
            textCompetition.text = ev.competition.ifBlank { "Evento" }
            textTime.text        = ev.time

            // Indicador en vivo
            chipLive.visibility = if (ev.live) android.view.View.VISIBLE else android.view.View.GONE

            // Equipos
            textHome.text = ev.homeTeam
            textAway.text = ev.awayTeam

            // Logos de equipos — intentar imagen, fallback emoji
            val homeLogoUrl = ev.channels.firstOrNull()?.let { "" } ?: ""
            Glide.with(root.context)
                .load(ev.homeLogoLocal.ifBlank { null })
                .placeholder(R.drawable.ic_shield_placeholder)
                .error(R.drawable.ic_shield_placeholder)
                .into(imageHome)

            Glide.with(root.context)
                .load(ev.awayLogoLocal.ifBlank { null })
                .placeholder(R.drawable.ic_shield_placeholder)
                .error(R.drawable.ic_shield_placeholder)
                .into(imageAway)

            // Bandera de la competición
            Glide.with(root.context)
                .load(ev.flagUrl.ifBlank { null })
                .placeholder(R.drawable.ic_flag_placeholder)
                .error(R.drawable.ic_flag_placeholder)
                .into(imageFlag)

            // Chips de canales disponibles
            chipGroupChannels.removeAllViews()
            // Deduplicar canales por stream_id (OP2/OP3 del mismo canal)
            val uniqueChannels = ev.channels
                .distinctBy { it.streamId }
                .take(6)  // max 6 chips

            uniqueChannels.forEach { ch ->
                val chip = ItemEventChannelChipBinding
                    .inflate(LayoutInflater.from(root.context), chipGroupChannels, false)
                chip.root.text = ch.name.replace(Regex("\\s*[|┃]\\s*OP\\d+\\s*$"), "")
                chip.root.setOnClickListener { onChannelClick(ev, ch) }
                chipGroupChannels.addView(chip.root)
            }

            // Si no hay canales disponibles
            textNoChannels.visibility =
                if (uniqueChannels.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }
    }
}
