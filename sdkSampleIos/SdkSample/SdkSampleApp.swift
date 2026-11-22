import SwiftUI
import SpaceSdk

@main
struct SdkSampleApp: App {
    // One SDK instance for the app's lifetime: it owns the HTTP clients and caches.
    private let sdk: SpaceSdk
    // State holders are created once (they start loading immediately), not in a view body.
    private let picture: PictureOfTheDayStateHolder
    private let news: NewsStateHolder

    init() {
        sdk = SpaceSdk()
        picture = sdk.createPictureOfTheDay()
        news = sdk.createNews(limit: 20)
    }

    var body: some Scene {
        WindowGroup {
            ContentView(picture: picture, news: news)
        }
    }
}
