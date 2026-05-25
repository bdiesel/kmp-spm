package com.example.kmpspm

import kotlinx.browser.window

private class WebPlatform : Platform {
    override val name: String = "Web (${window.navigator.userAgent.substringBefore(' ')})"
}

actual fun platform(): Platform = WebPlatform()
