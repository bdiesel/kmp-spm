import SwiftUI
import SpaceSdk

/// UI layer only: renders the SDK's state and sends events back
/// (unidirectional data flow). All logic lives in `PictureOfTheDayStateHolder`.
struct PictureOfTheDayView: View {
    let holder: PictureOfTheDayStateHolder

    @State private var uiState: PictureOfTheDayUiState
    @State private var date = Date()

    init(holder: PictureOfTheDayStateHolder) {
        self.holder = holder
        _uiState = State(initialValue: holder.currentState)
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    HStack {
                        DatePicker("Date", selection: $date, in: Self.firstApod...Date(), displayedComponents: .date)
                            .labelsHidden()
                        Spacer()
                        Button("Latest") { holder.loadLatest() }
                    }
                    content
                }
                .padding()
            }
            .navigationTitle("Picture of the Day")
        }
        // Kotlin Flow consumed as a Swift AsyncSequence; cancelled with the view.
        .task {
            do {
                for try await state in holder.state.asAsyncSequence() { uiState = state }
            } catch {
                uiState = PictureOfTheDayUiState.Error(message: "\(error)")
            }
        }
        .onChange(of: date) { _, newDate in holder.load(date: Self.isoDay.string(from: newDate)) }
    }

    // Kotlin sealed class → Swift enum: the compiler flags any unhandled case.
    @ViewBuilder private var content: some View {
        switch uiState.sealedType() {
        case .loading:
            ProgressView().frame(maxWidth: .infinity, minHeight: 200)
        case .success(let success):
            PictureDetail(picture: success.value.picture)
        case .notFound(let notFound):
            ContentUnavailableView("No picture", systemImage: "moon.zzz",
                                   description: Text("There's no APOD for \(notFound.value.date)."))
        case .error(let error):
            ContentUnavailableView("Couldn't load", systemImage: "wifi.exclamationmark",
                                   description: Text(error.value.message))
        }
    }

    private static let firstApod = isoDay.date(from: "1995-06-16")!
    private static let isoDay: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter
    }()
}

private struct PictureDetail: View {
    let picture: PictureOfTheDay

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            if picture.mediaType == .IMAGE, let url = picture.imageUrl.flatMap(URL.init(string:)) {
                AsyncImage(url: url) { image in
                    image.resizable().scaledToFit()
                } placeholder: {
                    ProgressView().frame(maxWidth: .infinity, minHeight: 240)
                }
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .accessibilityLabel(picture.altText.isEmpty ? picture.title : picture.altText)
            } else if let page = URL(string: picture.pageUrl) {
                Link("This entry is a video. Open it on nasa.gov", destination: page)
            }
            Text(picture.title).font(.title2.bold())
            Text(picture.date).font(.subheadline).foregroundStyle(.secondary)
            Text(picture.explanation)
            if !picture.credit.isEmpty {
                Text("Credit: \(picture.credit)").font(.footnote).foregroundStyle(.secondary)
            }
        }
    }
}
