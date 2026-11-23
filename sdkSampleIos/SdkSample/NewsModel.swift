import Foundation
import Observation
import SpaceSdk

/// View model for the News screen: calls SpaceSdk's use cases and exposes what
/// the view should show. Owned by the app, not the SDK.
@MainActor
@Observable
final class NewsModel {
    private(set) var articles: [NewsArticle] = []
    private(set) var isLoading = true
    private(set) var error: String?

    private let sdk: SpaceSdk

    init(sdk: SpaceSdk) {
        self.sdk = sdk
    }

    /// First load; uses cached articles when available.
    func load() async {
        await show { try await self.sdk.news.latest(count: 20) }
    }

    /// Pull-to-refresh / retry. Keeps the current list visible while loading.
    func refresh() async {
        await show { try await self.sdk.news.refresh(count: 20) }
    }

    private func show(_ request: () async throws -> NewsResult) async {
        isLoading = true
        error = nil
        do {
            switch try await request().sealedType() {
            case .success(let success): articles = success.value.articles
            case .failure(let failure): error = failure.value.error.message
            }
        } catch {
            self.error = SpaceError.NETWORK.message
        }
        isLoading = false
    }
}
