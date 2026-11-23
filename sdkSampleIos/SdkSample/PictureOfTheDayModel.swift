import Foundation
import Observation
import SpaceSdk

/// View model for the Picture of the Day screen: calls SpaceSdk's use cases and
/// exposes what the view should show. Owned by the app, not the SDK.
@MainActor
@Observable
final class PictureOfTheDayModel {
    enum State {
        case loading
        case shown(PictureOfTheDay)
        case failed(String)
    }

    private(set) var state: State = .loading
    private let sdk: SpaceSdk
    private var task: Task<Void, Never>?

    init(sdk: SpaceSdk) {
        self.sdk = sdk
        showLatest()
    }

    func showLatest() {
        load { try await $0.pictureOfTheDay.today() }
    }

    func show(date: Date) {
        let day = Calendar.current.dateComponents([.year, .month, .day], from: date)
        load {
            try await $0.pictureOfTheDay.forDate(
                year: Int32(day.year!), month: Int32(day.month!), day: Int32(day.day!)
            )
        }
    }

    /// Cancels the previous request, so a quick series of dates shows the last one.
    private func load(_ request: @escaping (SpaceSdk) async throws -> PictureResult) {
        task?.cancel()
        state = .loading
        task = Task { [sdk] in
            do {
                let result = try await request(sdk)
                guard !Task.isCancelled else { return }
                // Kotlin sealed class -> Swift enum: exhaustive switch.
                switch result.sealedType() {
                case .success(let success): state = .shown(success.value.picture)
                case .failure(let failure): state = .failed(failure.value.error.message)
                }
            } catch {
                if !Task.isCancelled { state = .failed(SpaceError.NETWORK.message) }
            }
        }
    }
}
