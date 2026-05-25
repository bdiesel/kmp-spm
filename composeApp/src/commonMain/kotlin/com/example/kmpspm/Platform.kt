package com.example.kmpspm

interface Platform {
    val name: String
}

expect fun platform(): Platform
