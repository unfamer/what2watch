package com.example.whattowatch

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val filters: Filters = Filters(),
    val genres: List<Genre> = emptyList(),
    val current: Pick? = null,
    val loading: Boolean = false,
    val error: String? = null
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as App).repo
    private var job: Job? = null

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    val later: StateFlow<List<Mark>> =
        repo.later.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadGenres()
        next()
    }

    fun setKind(kind: Kind) {
        _state.update { it.copy(filters = it.filters.copy(kind = kind, genreId = null), genres = emptyList()) }
        loadGenres()
        next()
    }

    fun setGenre(id: Int?) {
        _state.update { it.copy(filters = it.filters.copy(genreId = id)) }
        next()
    }

    fun setRating(value: Float) = _state.update { it.copy(filters = it.filters.copy(minRating = value)) }

    fun setYear(value: Int) = _state.update { it.copy(filters = it.filters.copy(yearFrom = value)) }

    fun next() {
        if (BuildConfig.TMDB_TOKEN.isBlank()) {
            _state.update { it.copy(error = "Добавь TMDB_TOKEN в local.properties и пересобери приложение") }
            return
        }
        job?.cancel()
        job = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val pick = repo.pick(_state.value.filters)
                _state.update {
                    it.copy(
                        current = pick ?: it.current,
                        loading = false,
                        error = if (pick == null) "Ничего не нашлось, смягчи фильтры" else null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = "Не удалось загрузить: ${e.message}") }
            }
        }
    }

    fun mark(status: String) {
        val pick = _state.value.current ?: return
        _state.update { it.copy(current = null) }
        viewModelScope.launch {
            repo.mark(pick, status)
            next()
        }
    }

    fun markDone(mark: Mark) {
        viewModelScope.launch { repo.setStatus(mark, STATUS_WATCHED) }
    }

    fun remove(mark: Mark) {
        viewModelScope.launch { repo.remove(mark) }
    }

    private fun loadGenres() {
        val kind = _state.value.filters.kind
        viewModelScope.launch {
            val genres = runCatching { repo.genres(kind) }.getOrDefault(emptyList())
            _state.update { if (it.filters.kind == kind) it.copy(genres = genres) else it }
        }
    }
}
