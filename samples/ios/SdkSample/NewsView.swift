import SwiftUI
import SpaceSdk

/// View: renders `NewsModel` and forwards user actions.
struct NewsView: View {
    let model: NewsModel

    var body: some View {
        NavigationStack {
            content
                .navigationTitle("Space News")
        }
        .task { await model.load() }
    }

    @ViewBuilder private var content: some View {
        if model.articles.isEmpty, model.isLoading {
            ProgressView()
        } else if model.articles.isEmpty, let error = model.error {
            ContentUnavailableView {
                Label("Couldn't load", systemImage: "wifi.exclamationmark")
            } description: {
                Text(error)
            } actions: {
                Button("Retry") { Task { await model.refresh() } }
            }
        } else {
            List(model.articles, id: \.id) { article in
                if let url = URL(string: article.url) {
                    Link(destination: url) { ArticleRow(article: article) }
                } else {
                    ArticleRow(article: article)
                }
            }
            .tint(.primary) // rows are links, but should read as text
            .refreshable { await model.refresh() }
        }
    }
}

private struct ArticleRow: View {
    let article: NewsArticle

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(article.title).font(.headline)
            Text("\(article.newsSite) · \(article.publishedAt.prefix(10))")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding(.vertical, 4)
    }
}
