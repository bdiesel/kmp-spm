package com.example.spacesample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.spacesample.news.NewsScreen
import com.example.spacesample.picture.PictureOfTheDayScreen

/** App shell: two tabs, each a view backed by its own ViewModel. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceApp() {
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
                if (tab == 0) PictureOfTheDayScreen() else NewsScreen()
            }
        }
    }
}
