package com.example.whattowatch

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface TmdbApi {
    @GET("discover/movie")
    suspend fun discoverMovies(@QueryMap params: Map<String, String>): PageResponse

    @GET("discover/tv")
    suspend fun discoverTv(@QueryMap params: Map<String, String>): PageResponse

    @GET("genre/{kind}/list")
    suspend fun genres(
        @Path("kind") kind: String,
        @Query("language") language: String = "ru-RU"
    ): GenresResponse
}
