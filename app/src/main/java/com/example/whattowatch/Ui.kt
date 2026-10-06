package com.example.whattowatch

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Calendar
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

private const val IMG = "https://image.tmdb.org/t/p/w500"
private const val MIN_YEAR = 1960
private val buttonPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)

@Composable
fun WatchTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun WatchApp(vm: MainViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val later by vm.later.collectAsStateWithLifecycle()
    var showList by rememberSaveable { mutableStateOf(false) }

    Scaffold { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (showList) {
                BackHandler { showList = false }
                ListScreen(
                    items = later,
                    onDone = vm::markDone,
                    onRemove = vm::remove,
                    onBack = { showList = false }
                )
            } else {
                PickScreen(state, vm, later.size) { showList = true }
            }
        }
    }
}

@Composable
private fun PickScreen(state: UiState, vm: MainViewModel, laterCount: Int, onOpenList: () -> Unit) {
    var filtersOpen by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Kind.entries.forEach { kind ->
                FilterChip(
                    selected = state.filters.kind == kind,
                    onClick = { vm.setKind(kind) },
                    label = { Text(kind.label) }
                )
            }
        }
        if (state.loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))

        Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp)) {
            val current = state.current
            when {
                current != null -> TitleCard(current)
                state.loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
        }

        AnimatedVisibility(filtersOpen) { FiltersPanel(state, vm) }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = vm::next,
                contentPadding = buttonPadding,
                modifier = Modifier.weight(1f)
            ) { Text("Другое", maxLines = 1) }
            FilledTonalButton(
                onClick = { vm.mark(STATUS_LATER) },
                enabled = state.current != null,
                contentPadding = buttonPadding,
                modifier = Modifier.weight(1f)
            ) { Text("Посмотрю", maxLines = 1) }
            TextButton(
                onClick = { vm.mark(STATUS_HIDDEN) },
                enabled = state.current != null,
                contentPadding = buttonPadding
            ) { Text("Скрыть", maxLines = 1) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { filtersOpen = !filtersOpen }) { Text("Фильтры") }
            TextButton(onClick = onOpenList) { Text("Мой список ($laterCount)") }
        }
    }
}

@Composable
private fun TitleCard(pick: Pick) {
    val t = pick.title
    val meta = buildString {
        append("★ ").append("%.1f".format(t.voteAverage))
        t.year?.let { append(" · ").append(it) }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        AsyncImage(
            model = IMG + t.posterPath,
            contentDescription = t.label,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .aspectRatio(2f / 3f)
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(12.dp))
        )
        Spacer(Modifier.height(12.dp))
        Text(t.label, style = MaterialTheme.typography.headlineSmall)
        Text(meta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text(t.overview.orEmpty(), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun FiltersPanel(state: UiState, vm: MainViewModel) {
    val f = state.filters
    val maxYear = Calendar.getInstance().get(Calendar.YEAR)
    Column(Modifier.fillMaxWidth()) {
        if (state.genres.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = f.genreId == null,
                        onClick = { vm.setGenre(null) },
                        label = { Text("Любой жанр") }
                    )
                }
                items(state.genres, key = { it.id }) { g ->
                    FilterChip(
                        selected = f.genreId == g.id,
                        onClick = { vm.setGenre(g.id) },
                        label = { Text(g.name) }
                    )
                }
            }
        }
        Text("Рейтинг от ${"%.1f".format(f.minRating)}", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = f.minRating,
            onValueChange = vm::setRating,
            onValueChangeFinished = vm::next,
            valueRange = 5f..9f,
            steps = 7
        )
        Text("Год от ${f.yearFrom}", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = f.yearFrom.toFloat(),
            onValueChange = { vm.setYear(it.toInt()) },
            onValueChangeFinished = vm::next,
            valueRange = MIN_YEAR.toFloat()..maxYear.toFloat(),
            steps = maxYear - MIN_YEAR - 1
        )
    }
}

@Composable
private fun ListScreen(
    items: List<Mark>,
    onDone: (Mark) -> Unit,
    onRemove: (Mark) -> Unit,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        TextButton(onClick = onBack, modifier = Modifier.padding(horizontal = 8.dp)) { Text("Назад") }
        if (items.isEmpty()) {
            Text("Пока пусто", modifier = Modifier.padding(16.dp))
        } else LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { "${it.kind}-${it.id}" }) { m ->
                Row {
                    AsyncImage(
                        model = IMG + m.posterPath,
                        contentDescription = m.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(64.dp)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(m.title, style = MaterialTheme.typography.titleMedium)
                        Row {
                            TextButton(onClick = { onDone(m) }) { Text("Просмотрено") }
                            TextButton(onClick = { onRemove(m) }) { Text("Убрать") }
                        }
                    }
                }
            }
        }
    }
}
