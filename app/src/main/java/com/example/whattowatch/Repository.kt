package com.example.whattowatch

import kotlin.random.Random

private const val ANIME_GENRE = 16
private const val MAX_PAGE = 20

class Repository(private val api: TmdbApi, private val dao: MarkDao) {

    val later = dao.later()

    suspend fun genres(kind: Kind): List<Genre> {
        val all = api.genres(kind.path).genres
        return if (kind == Kind.ANIME) all.filter { it.id != ANIME_GENRE } else all
    }

    suspend fun pick(filters: Filters): Pick? {
        val seen = dao.ids(filters.kind.path).toSet()
        var maxPage = MAX_PAGE
        repeat(4) {
            val page = Random.nextInt(1, maxPage + 1)
            val response = fetch(filters, page)
            maxPage = minOf(maxPage, response.totalPages.coerceAtLeast(1))
            val candidates = response.results.filter {
                it.id !in seen && !it.overview.isNullOrBlank() && it.posterPath != null
            }
            if (candidates.isNotEmpty()) return Pick(candidates.random(), filters.kind.path)
        }
        return null
    }

    suspend fun mark(pick: Pick, status: String) {
        val t = pick.title
        dao.put(Mark(t.id, pick.path, status, t.label, t.posterPath, System.currentTimeMillis()))
    }

    suspend fun setStatus(mark: Mark, status: String) = dao.setStatus(mark.id, mark.kind, status)

    suspend fun remove(mark: Mark) = dao.delete(mark.id, mark.kind)

    private suspend fun fetch(f: Filters, page: Int): PageResponse {
        val params = params(f, page)
        return if (f.kind == Kind.MOVIE) api.discoverMovies(params) else api.discoverTv(params)
    }

    private fun params(f: Filters, page: Int): Map<String, String> = buildMap {
        put("language", "ru-RU")
        put("page", page.toString())
        put("sort_by", "popularity.desc")
        put("vote_average.gte", f.minRating.toString())
        put("vote_count.gte", "300")
        val dateKey = if (f.kind == Kind.MOVIE) "primary_release_date.gte" else "first_air_date.gte"
        put(dateKey, "${f.yearFrom}-01-01")
        val genres = listOfNotNull(if (f.kind == Kind.ANIME) ANIME_GENRE else null, f.genreId)
        if (genres.isNotEmpty()) put("with_genres", genres.joinToString(","))
        if (f.kind == Kind.ANIME) put("with_original_language", "ja")
    }
}
