import { MediaType, PictureOfTheDay } from "@bdiesel/space-sdk";
import { el } from "../dom";
import { PictureOfTheDayViewModel, PictureUiState } from "./pictureOfTheDayViewModel";

const FIRST_PICTURE = "1995-06-16";

/** View: renders the view model's state and forwards user actions. */
export function pictureOfTheDayView(root: HTMLElement, viewModel: PictureOfTheDayViewModel): void {
  const date = el("input", { type: "date", min: FIRST_PICTURE, max: new Date().toISOString().slice(0, 10) });
  const previous = el("button", {}, "◀ Previous day");
  const latest = el("button", { className: "primary" }, "Latest");
  const content = el("div", { className: "content" });

  date.addEventListener("change", () => date.value && viewModel.showDate(date.value));
  previous.addEventListener("click", () => viewModel.showPreviousDay());
  latest.addEventListener("click", () => viewModel.showLatest());

  root.replaceChildren(el("div", { className: "toolbar" }, date, previous, latest), content);

  viewModel.state.subscribe((state: PictureUiState) => {
    previous.disabled = state.kind !== "shown";
    if (state.kind === "shown") date.value = state.picture.date;
    content.replaceChildren(render(state));
  });
}

function render(state: PictureUiState): Node {
  switch (state.kind) {
    case "loading":
      return el("p", { className: "status" }, "Loading…");
    case "failed":
      return el("p", { className: "status error" }, state.message);
    case "shown":
      return pictureDetail(state.picture);
  }
}

function pictureDetail(picture: PictureOfTheDay): Node {
  const media =
    picture.mediaType === MediaType.IMAGE && picture.imageUrl
      ? el("img", { src: picture.imageUrl, alt: picture.altText || picture.title })
      : el("a", { href: picture.pageUrl, target: "_blank", rel: "noopener" }, "This entry is a video. Open it on nasa.gov");

  return el(
    "article",
    { className: "picture" },
    media,
    el("h2", {}, picture.title),
    el("p", { className: "date" }, picture.date),
    el("p", {}, picture.explanation),
    picture.credit ? el("p", { className: "credit" }, `Credit: ${picture.credit}`) : "",
  );
}
