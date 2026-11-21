package com.example.kmpspm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            // Saveable so the count survives rotation, resizing and process death.
            var count by rememberSaveable { mutableIntStateOf(0) }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // Keep content clear of status/navigation bars, the notch and
                    // Dynamic Island (the app draws edge-to-edge on Android and iOS).
                    .safeContentPadding()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            ) {
                Text(
                    text = "Hello from ${platform().name}",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "Count: $count",
                    style = MaterialTheme.typography.titleLarge,
                )
                Button(onClick = { count++ }) {
                    Text("Tap me")
                }
            }
        }
    }
}
