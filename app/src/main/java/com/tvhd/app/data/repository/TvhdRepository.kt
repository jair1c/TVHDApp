package com.tvhd.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.tvhd.app.data.api.ApiClient
import com.tvhd.app.data.model.ApiResponse
import com.tvhd.app.data.model.Channel
import com.tvhd.app.data.model.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

class TvhdRepository(private val context: Context) {

    private val gson = Gson()
    private var cachedResponse: ApiResponse? = null

    // ── Carga datos: API primero, luego assets locales ───────────────────────
    suspend fun loadAll(): Result<ApiResponse> = withContext(Dispatchers.IO) {
        // 1. Intentar API del servidor
        try {
            val resp = ApiClient.service.getChannels()
            if (resp.isSuccessful) {
                // Si el servidor responde, obtener todo
                val allResp = ApiClient.service.getAll()
                if (allResp.isSuccessful && allResp.body() != null) {
                    cachedResponse = allResp.body()
                    return@withContext Result.Success(allResp.body()!!)
                }
            }
        } catch (e: Exception) {
            // Sin servidor: usar datos locales
        }

        // 2. Fallback: leer tvhd_data.json desde assets
        try {
            val json = context.assets.open("tvhd_data.json")
                .bufferedReader().use { it.readText() }
            val response = gson.fromJson(json, ApiResponse::class.java)
            cachedResponse = response
            return@withContext Result.Success(response)
        } catch (e: Exception) {
            return@withContext Result.Error("No se pudieron cargar los datos: ${e.message}")
        }
    }

    suspend fun getChannels(category: String? = null, query: String? = null): List<Channel> {
        val all = cachedResponse ?: (loadAll() as? Result.Success)?.data ?: return emptyList()
        return all.channels.filter { ch ->
            val catOk = category == null || category == "all" || ch.category == category
            val qOk   = query.isNullOrBlank() || ch.name.contains(query, ignoreCase = true)
            catOk && qOk
        }
    }

    suspend fun getEvents(liveOnly: Boolean = false): List<Event> {
        val all = cachedResponse ?: (loadAll() as? Result.Success)?.data ?: return emptyList()
        return if (liveOnly) all.events.filter { it.live } else all.events
    }

    // Forzar recarga desde la API
    suspend fun refresh(): Result<ApiResponse> {
        cachedResponse = null
        return loadAll()
    }
}
