package com.example.kmpspm.space

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.spacesdk.MediaType
import com.example.spacesdk.NewsArticle
import com.example.spacesdk.NewsStateHolder
import com.example.spacesdk.NewsUiState
import com.example.spacesdk.PictureOfTheDay
import com.example.spacesdk.PictureOfTheDayStateHolder
import com.example.spacesdk.PictureOfTheDayUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** UI layer only: renders SpaceSdk state and sends events back (unidirectional data flow). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceApp(viewModel: SpaceViewModel = viewModel()) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    MaterialTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text(if (tab == 0) "Picture of the Day" else "Space News") }) },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Text("✨") }, label = { Text("Picture") })
                    NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Text("📰") }, label = { Text("News") })
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding)) {
                if (tab == 0) PictureScreen(viewModel.picture) else NewsScreen(viewModel.news)
            }
        }
    }
}

@Composable
private fun PictureScreen(holder: PictureOfTheDayStateHolder) {
    val state by holder.state.collectAsStateWithLifecycle(holder.currentState)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val shownDate = (state as? PictureOfTheDayUiState.Success)?.picture?.date
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(enabled = shownDate != null, onClick = { shownDate?.let { holder.load(it.plusDays(-1)) } }) {
                Text("◀ Previous day")
            }
            Button(onClick = { holder.loadLatest() }) { Text("Latest") }
        }
        // Kotlin sealed class: `when` is exhaustive, so a new state can't be forgotten.
        when (val s = state) {
            PictureOfTheDayUiState.Loading -> Loading()
            is PictureOfTheDayUiState.Success -> PictureDetail(s.picture)
            is PictureOfTheDayUiState.NotFound -> Message("There's no APOD for ${s.date}.")
            is PictureOfTheDayUiState.Error -> Message(s.message)
        }
    }
}

@Composable
private fun PictureDetail(picture: PictureOfTheDay) {
    val uriHandler = LocalUriHandler.current
    if (picture.mediaType == MediaType.IMAGE && picture.imageUrl != null) {
        AsyncImage(
            model = picture.imageUrl,
            contentDescription = picture.altText.ifEmpty { picture.title },
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
        )
    } else {
        OutlinedButton(onClick = { uriHandler.openUri(picture.pageUrl) }) { Text("This entry is a video. Open on nasa.gov") }
    }
    Text(picture.title, style = MaterialTheme.typography.headlineSmall)
    Text(picture.date, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(picture.explanation, style = MaterialTheme.typography.bodyLarge)
    if (picture.credit.isNotEmpty()) {
        Text("Credit: ${picture.credit}", style = MaterialTheme.typography.bodySmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewsScreen(holder: NewsStateHolder) {
    val state by holder.state.collectAsStateWithLifecycle(holder.currentState)
    val uriHandler = LocalUriHandler.current
    when (val s = state) {
        NewsUiState.Loading -> Loading()
        is NewsUiState.Error -> Column(Modifier.padding(16.dp)) {
            Message(s.message)
            Button(onClick = { holder.refresh() }) { Text("Retry") }
        }
        is NewsUiState.Success -> PullToRefreshBox(isRefreshing = false, onRefresh = { holder.refresh() }) {
            LazyColumn(Modifier.fillMaxSize()) {
                items(s.articles, key = NewsArticle::id) { article ->
                    ListItem(
                        supportingContent = { Text("${article.newsSite} · ${article.publishedAt.take(10)}") },
                        modifier = Modifier.clickable { uriHandler.openUri(article.url) },
                    ) {
                        Text(article.title)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun Loading() {
    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun Message(text: String) {
    Text(text, Modifier.padding(vertical = 16.dp), style = MaterialTheme.typography.bodyLarge)
}

/** "2026-11-22".plusDays(-1) == "2026-11-21". java.util APIs keep minSdk 24 without desugaring. */
private fun String.plusDays(days: Int): String {
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val calendar = Calendar.getInstance().apply {
        time = format.parse(this@plusDays)!!
        add(Calendar.DAY_OF_MONTH, days)
    }
    return format.format(calendar.time)
}
