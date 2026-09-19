package com.newtaraneh.music.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

const val BASE_URL = "https://newtaraneh-api.farshadhelboys.workers.dev/"

interface ApiService {
    @GET("songs")
    suspend fun getSongs(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 30,
        @Query("q") query: String? = null,
        @Query("suggested") suggested: Int? = null
    ): SongsResponse

    @GET("songs/{id}")
    suspend fun getSong(@Path("id") id: Long): SongDetail
}

data class SongsResponse(
    val page: Int,
    val limit: Int,
    val songs: List<SongDto>
)

data class SongDto(
    val id: Long,
    val message_id: Long,
    val title: String?,
    val artist: String?,
    val duration: Int?,
    val file_size: Long?,
    val thumbnail_file_id: String?,
    val created_at: Long?,
    val caption: String?,
    val cover_url: String?,
    val is_suggested: Int? = 0
)

data class SongDetail(
    val id: Long,
    val title: String?,
    val artist: String?,
    val caption: String?,
    val duration: Int?,
    val stream_url: String?,
    val cover_url: String?
)

object ApiClient {
    val service: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
