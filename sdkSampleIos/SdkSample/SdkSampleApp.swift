import SwiftUI
import SpaceSdk

@main
struct SdkSampleApp: App {
    // One SDK instance for the app's lifetime: it owns the network clients and caches.
    // View models are created once here, not in a view body.
    @State private var picture: PictureOfTheDayModel
    @State private var news: NewsModel

    init() {
        let sdk = SpaceSdk()
        _picture = State(initialValue: PictureOfTheDayModel(sdk: sdk))
        _news = State(initialValue: NewsModel(sdk: sdk))
    }

    var body: some Scene {
        WindowGroup {
            TabView {
                PictureOfTheDayView(model: picture)
                    .tabItem { Label("Picture", systemImage: "sparkles") }
                NewsView(model: news)
                    .tabItem { Label("News", systemImage: "newspaper") }
            }
        }
    }
}

/// The app decides how SDK errors read to its users.
extension SpaceError {
    var message: String {
        switch self {
        case .NETWORK: "Couldn't reach the server. Check your connection and try again."
        case .SERVER: "The service had a problem. Try again later."
        case .NOT_FOUND: "There's no picture for that day."
        case .INVALID_DATE: "Pick a date from June 16, 1995 onward."
        }
    }
}
