package com.example.whattowatch

import com.google.gson.annotations.SerializedName

enum class Kind(val path: String, val label: String) {
    MOVIE("movie", "Фильмы"),
    TV("tv", "Сериалы"),
    ANIME("tv", "Аниме")
}

data class Filters(
    val kind: Kind = Kind.MOVIE,
    val genreId: Int? = null,
    val minRating: Float = 7f,
    val yearFrom: Int = 1990
)

data class Genre(val id: Int, val name: String)

data class GenresResponse(val genres: List<Genre>)

data class Title(
    val id: Int,
    val title: String?,
    val name: String?,
    val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("vote_average") val voteAverage: Double,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("first_air_date") val firstAirDate: String?
) {
    val label: String get() = title ?: name.orEmpty()
    val year: String? get() = (releaseDate ?: firstAirDate)?.take(4)?.takeIf { it.isNotBlank() }
}

data class PageResponse(
    val results: List<Title>,
    @SerializedName("total_pages") val totalPages: Int
)

data class Pick(val title: Title, val path: String)
