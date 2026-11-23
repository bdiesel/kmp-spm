package com.example.spacesample.picture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.spacesdk.PictureOfTheDay

/** View: renders [PictureOfTheDayViewModel.state] and forwards user actions. */
@Composable
fun PictureOfTheDayScreen(viewModel: PictureOfTheDayViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(enabled = state is PictureUiState.Shown, onClick = viewModel::showPreviousDay) {
                Text("◀ Previous day")
            }
            Button(onClick = viewModel::showLatest) { Text("Latest") }
        }
        when (val s = state) {
            PictureUiState.Loading -> Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is PictureUiState.Shown -> PictureDetail(s.picture)
            is PictureUiState.Failed -> Text(s.message, style = MaterialTheme.typography.bodyLarge)
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
