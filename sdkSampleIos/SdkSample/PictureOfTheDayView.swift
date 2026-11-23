import SwiftUI
import SpaceSdk

/// View: renders `PictureOfTheDayModel.state` and forwards user actions.
struct PictureOfTheDayView: View {
    let model: PictureOfTheDayModel
    @State private var date = Date()

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    HStack {
                        DatePicker("Date", selection: $date, in: Self.firstPicture...Date(), displayedComponents: .date)
                            .labelsHidden()
                        Spacer()
                        Button("Latest") { model.showLatest() }
                    }
                    content
                }
                .padding()
            }
            .navigationTitle("Picture of the Day")
        }
        .onChange(of: date) { _, newDate in model.show(date: newDate) }
    }

    @ViewBuilder private var content: some View {
        switch model.state {
        case .loading:
            ProgressView().frame(maxWidth: .infinity, minHeight: 200)
        case .shown(let picture):
            PictureDetail(picture: picture)
        case .failed(let message):
            ContentUnavailableView("Couldn't load", systemImage: "moon.zzz", description: Text(message))
        }
    }

    private static let firstPicture = DateComponents(calendar: .current, year: 1995, month: 6, day: 16).date!
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
