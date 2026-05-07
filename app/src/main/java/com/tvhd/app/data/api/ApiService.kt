package com.tvhd.app.data.api

import com.tvhd.app.BuildConfig
import com.tvhd.app.data.model.ApiResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface TvhdApiService {
    @GET(".")
    suspend fun getAll(): Response<ApiResponse>

    @GET("channels")
    suspend fun getChannels(
        @Query("cat")  category: String? = null,
        @Query("q")    query: String? = null,
        @Query("live") live: Boolean? = null,
    ): Response<ChannelsResponse>

    @GET("events")
    suspend fun getEvents(
        @Query("live") live: Boolean? = null,
        @Query("team") team: String? = null,
    ): Response<EventsResponse>

    @GET("status")
    suspend fun getStatus(): Response<StatusResponse>
}

data class ChannelsResponse(
    val total: Int = 0,
    val channels: List<com.tvhd.app.data.model.Channel> = emptyList(),
)

data class EventsResponse(
    val total: Int = 0,
    val live: Int = 0,
    val events: List<com.tvhd.app.data.model.Event> = emptyList(),
)

data class StatusResponse(
    val status: String = "",
    val last_update: String = "",
    val channels: Int = 0,
    val events: Int = 0,
    val live_events: Int = 0,
)

object ApiClient {
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }

    val service: TvhdApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TvhdApiService::class.java)
    }
}
