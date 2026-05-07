package com.tvhd.app.data.model

import com.google.gson.annotations.SerializedName

// ── Canal ────────────────────────────────────────────────────────────────────
data class Channel(
    @SerializedName("name")       val name: String,
    @SerializedName("stream_id")  val streamId: String,
    @SerializedName("stream_url") val streamUrl: String,
    @SerializedName("category")   val category: String = "deportes",
    @SerializedName("logo_url")   val logoUrl: String = "",
    @SerializedName("logo_local") val logoLocal: String = "",
    @SerializedName("hd")         val hd: Boolean = false,
    @SerializedName("live")       val live: Boolean = false,
) {
    // URL del canal en el sitio (para WebView player)
    val watchUrl: String get() = "https://tvtvhd.com/canales.php?stream=$streamId"
    // URL embed directa
    val embedUrl: String get() = streamUrl
}

// ── Opción de canal para un evento ──────────────────────────────────────────
data class EventChannel(
    @SerializedName("name")       val name: String,
    @SerializedName("stream_id")  val streamId: String,
    @SerializedName("stream_url") val streamUrl: String,
)

// ── Evento deportivo ─────────────────────────────────────────────────────────
data class Event(
    @SerializedName("time")            val time: String,
    @SerializedName("time_dt")         val timeDt: String = "",
    @SerializedName("competition")     val competition: String,
    @SerializedName("match")           val match: String,
    @SerializedName("flag_url")        val flagUrl: String = "",
    @SerializedName("live")            val live: Boolean = false,
    @SerializedName("channels")        val channels: List<EventChannel> = emptyList(),
    @SerializedName("home_logo_local") val homeLogoLocal: String = "",
    @SerializedName("away_logo_local") val awayLogoLocal: String = "",
) {
    val homeTeam: String get() {
        val parts = match.split(Regex("\\s+vs\\.?\\s+", RegexOption.IGNORE_CASE))
        return if (parts.size >= 2) parts[0].trim() else match
    }
    val awayTeam: String get() {
        val parts = match.split(Regex("\\s+vs\\.?\\s+", RegexOption.IGNORE_CASE))
        return if (parts.size >= 2) parts[1].trim() else ""
    }
}

// ── Respuesta API ────────────────────────────────────────────────────────────
data class ApiResponse(
    @SerializedName("scraped_at") val scrapedAt: String = "",
    @SerializedName("source")     val source: String = "",
    @SerializedName("stats")      val stats: Stats = Stats(),
    @SerializedName("channels")   val channels: List<Channel> = emptyList(),
    @SerializedName("events")     val events: List<Event> = emptyList(),
)

data class Stats(
    @SerializedName("total_channels") val totalChannels: Int = 0,
    @SerializedName("total_events")   val totalEvents: Int = 0,
    @SerializedName("live_events")    val liveEvents: Int = 0,
)

// ── Categorías de canales ────────────────────────────────────────────────────
enum class ChannelCategory(val label: String, val emoji: String) {
    ALL("Todos", "📺"),
    DEPORTES("Deportes", "⚽"),
    NOTICIAS("Noticias", "📰"),
    ENTRETENIMIENTO("Entretenimiento", "🎬"),
    SEÑALABIERTA("Señal Abierta", "📡"),
    INFANTIL("Infantil", "🧸"),
    MUSICA("Música", "🎵"),
    DOCUMENTALES("Documentales", "🎥");

    companion object {
        fun fromString(s: String) = values().find {
            it.name.lowercase() == s.lowercase()
        } ?: ALL
    }
}
