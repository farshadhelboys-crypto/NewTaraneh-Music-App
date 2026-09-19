package com.newtaraneh.music.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

const val BASE_URL = "https://newtaraneh-api.farshadhelboys.workers.dev/"
const val ADMIN_KEY = "newtaraneh_admin_2026"

interface ApiService {
    @GET("songs")
    suspend fun getSongs(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 40,
        @Query("q") query: String? = null,
        @Query("suggested") suggested: Int? = null
    ): SongsResponse

    @GET("songs/{id}")
    suspend fun getSong(@Path("id") id: Long): SongDetail

    @DELETE("songs/{id}")
    suspend fun deleteSong(
        @Path("id") id: Long,
        @Header("X-Admin-Key") key: String = ADMIN_KEY
    ): Map<String, Any>
}

data class SongsResponse(val page: Int, val limit: Int, val songs: List<SongDto>)

data class SongDto(
    val id: Long,
    val message_id: Long = 0,
    val title: String? = null,
    val artist: String? = null,
    val duration: Int? = 0,
    val file_size: Long? = 0,
    val thumbnail_file_id: String? = null,
    val created_at: Long? = 0,
    val caption: String? = null,
    val cover_url: String? = null,
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
