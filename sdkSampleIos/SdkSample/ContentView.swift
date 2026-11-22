import SwiftUI
import SpaceSdk

/// UI layer: renders the SDK's state and sends events back (unidirectional data flow).
/// All logic lives in the shared `UserLookupStateHolder`.
struct ContentView: View {
    let lookup: UserLookupStateHolder

    @State private var userId = "2"
    @State private var uiState: UserLookupUiState

    init(lookup: UserLookupStateHolder) {
        self.lookup = lookup
        _uiState = State(initialValue: lookup.currentState)
    }

    var body: some View {
        VStack(spacing: 20) {
            Text("SpaceSdk (prebuilt Swift package)").font(.headline)

            TextField("User id", text: $userId)
                .textFieldStyle(.roundedBorder)
                .frame(maxWidth: 160)

            Button("Look up user") { lookup.lookUp(id: userId) }

            Text(describe(uiState))
            Text("Try 1–3, an unknown id, or \"error\".")
                .font(.footnote)
                .foregroundStyle(.secondary)
        }
        .padding()
        // Kotlin Flow consumed as a Swift AsyncSequence; cancelled with the view.
        .task {
            do {
                for try await state in lookup.state.asAsyncSequence() {
                    uiState = state
                }
            } catch {
                uiState = UserLookupUiState.Error(message: "\(error)")
            }
        }
        .onAppear { lookup.lookUp(id: userId) }
    }

    // Kotlin sealed class → Swift enum: the compiler flags any unhandled case.
    private func describe(_ state: UserLookupUiState) -> String {
        switch state.sealedType() {
        case .idle:
            return "—"
        case .loading(let loading):
            return "Looking up \(loading.value.id)…"
        case .success(let success):
            return "Found \(success.value.user.name)"
        case .notFound(let missing):
            return "No user with id \(missing.value.id)"
        case .error(let error):
            return "Error: \(error.value.message)"
        }
    }
}
