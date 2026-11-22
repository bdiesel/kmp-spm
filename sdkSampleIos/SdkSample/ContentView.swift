import SwiftUI
import SpaceSdk

struct ContentView: View {
    let picture: PictureOfTheDayStateHolder
    let news: NewsStateHolder

    var body: some View {
        TabView {
            PictureOfTheDayView(holder: picture)
                .tabItem { Label("Picture", systemImage: "sparkles") }
            NewsView(holder: news)
                .tabItem { Label("News", systemImage: "newspaper") }
        }
    }
}
