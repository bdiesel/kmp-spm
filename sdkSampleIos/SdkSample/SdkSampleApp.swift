import SwiftUI
import SpaceSdk

@main
struct SdkSampleApp: App {
    // One SDK instance for the app's lifetime: it owns the shared caches.
    private let sdk = SpaceSdk()

    var body: some Scene {
        WindowGroup {
            ContentView(lookup: sdk.createUserLookup())
        }
    }
}
