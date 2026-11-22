package com.example.kmpspm.space

import androidx.lifecycle.ViewModel
import com.example.spacesdk.SpaceSdk

/** One SDK instance for the whole app: it owns the HTTP clients and caches. */
object SpaceSdkProvider {
    val sdk: SpaceSdk by lazy { SpaceSdk() }
}

/**
 * Android glue: the ViewModel survives configuration changes and owns the
 * SDK's shared state holders, closing them when the screen goes away. All
 * logic lives in the SDK.
 */
class SpaceViewModel(sdk: SpaceSdk = SpaceSdkProvider.sdk) : ViewModel() {
    val picture = sdk.createPictureOfTheDay()
    val news = sdk.createNews(limit = 20)

    override fun onCleared() {
        picture.close()
        news.close()
    }
}
