package com.example.spacesample.news

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/** View: renders [NewsViewModel.state] and forwards user actions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(viewModel: NewsViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    when {
        state.articles.isEmpty() && state.isLoading -> Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        state.articles.isEmpty() && state.error != null -> Column(Modifier.padding(16.dp)) {
            Text(state.error!!, Modifier.padding(vertical = 16.dp), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = viewModel::refresh) { Text("Retry") }
        }
        else -> PullToRefreshBox(isRefreshing = state.isLoading, onRefresh = viewModel::refresh) {
            LazyColumn(Modifier.fillMaxSize()) {
                items(state.articles, key = { it.id }) { article ->
                    ListItem(
                        headlineContent = { Text(article.title) },
                        supportingContent = { Text("${article.newsSite} · ${article.publishedAt.take(10)}") },
                        modifier = Modifier.clickable { uriHandler.openUri(article.url) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
