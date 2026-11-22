import SwiftUI
import KmpSdk

@main
struct SdkSampleApp: App {
    // One SDK instance for the app's lifetime: it owns the shared caches.
    private let sdk = KmpSdk()

    var body: some Scene {
        WindowGroup {
            ContentView(lookup: sdk.createUserLookup())
        }
    }
}
