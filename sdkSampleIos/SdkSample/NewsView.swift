import SwiftUI
import SpaceSdk

/// Latest spaceflight news, rendered from `NewsStateHolder`.
struct NewsView: View {
    let holder: NewsStateHolder

    @State private var uiState: NewsUiState

    init(holder: NewsStateHolder) {
        self.holder = holder
        _uiState = State(initialValue: holder.currentState)
    }

    var body: some View {
        NavigationStack {
            content
                .navigationTitle("Space News")
        }
        .task {
            do {
                for try await state in holder.state.asAsyncSequence() { uiState = state }
            } catch {
                uiState = NewsUiState.Error(message: "\(error)")
            }
        }
    }

    @ViewBuilder private var content: some View {
        switch uiState.sealedType() {
        case .loading:
            ProgressView()
        case .success(let success):
            List(success.value.articles, id: \.id) { article in
                if let url = URL(string: article.url) {
                    Link(destination: url) { ArticleRow(article: article) }
                } else {
                    ArticleRow(article: article)
                }
            }
            .tint(.primary) // rows are links, but should read as text
            .refreshable { holder.refresh() }
        case .error(let error):
            ContentUnavailableView {
                Label("Couldn't load", systemImage: "wifi.exclamationmark")
            } description: {
                Text(error.value.message)
            } actions: {
                Button("Retry") { holder.refresh() }
            }
        }
    }
}

private struct ArticleRow: View {
    let article: NewsArticle

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(article.title).font(.headline).foregroundStyle(.primary)
            Text("\(article.newsSite) · \(article.publishedAt.prefix(10))")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding(.vertical, 4)
    }
}
