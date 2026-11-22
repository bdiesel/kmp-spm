package com.example.kmpspm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.kmpspm.space.SpaceApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // SpaceSdk sample (APOD + Spaceflight News). The shared Compose counter
        // App() from :composeApp still runs on iOS and Web.
        setContent { SpaceApp() }
    }
}
